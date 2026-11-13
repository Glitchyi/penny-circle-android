package com.spendingbuddy.app.p2p

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.spendingbuddy.app.BuddyNotifications
import com.spendingbuddy.app.BuddyRepository
import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.Member
import com.spendingbuddy.app.SecretVault
import com.spendingbuddy.app.Visibility
import com.spendingbuddy.app.R
import computer.iroh.EndpointAddr
import computer.iroh.EndpointId
import computer.iroh.EndpointTicket
import computer.iroh.SecretKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import com.spendingbuddy.app.p2p.approveJoin as coreApproveJoin
import com.spendingbuddy.app.p2p.declineJoin as coreDeclineJoin
import com.spendingbuddy.app.p2p.issueJoinCode as coreIssueJoinCode
import com.spendingbuddy.app.p2p.respondToSuggestion as coreRespondToSuggestion
import com.spendingbuddy.app.p2p.setVisibility as coreSetVisibility
import com.spendingbuddy.app.p2p.transferAdmin as coreTransferAdmin

/**
 * Circle runtime. There is no gossip in the iroh 1.2.0 Kotlin bindings, so each member is reached over its own direct
 * QUIC connection (ALPN pennycircle/sync/2) using [fanOut]: one `sync` message per member, bounded concurrency,
 * failures isolated per member. Delivery is approximate: a member's phone must be open, kept reachable, or caught by
 * an approximate background pass; queued events stay in the outbox until that member acknowledges them.
 *
 * Public suspend functions throw IllegalStateException with a calm, factual message on failure (also recorded in
 * `status.lastError`); the UI shows it. [joinWithCode] instead reports through `status.joinProgress` (Refused) and does not throw.
 */
class PeerSyncManager private constructor(private val app: Context) {
    private val repo = BuddyRepository.get(app)
    private val vault = SecretVault(app)
    private val joinPrefs = app.getSharedPreferences("buddy_join", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val endpointLock = Mutex()
    private val exchangeLock = Mutex()
    private val joinLock = Mutex()
    private val keyLock = Any()
    private val serveGate = Semaphore(MAX_SERVING)
    private var cachedKey: ByteArray? = null
    private var transport: IrohTransport? = null
    private var acceptJob: Job? = null
    private var periodicJob: Job? = null
    private var pollJob: Job? = null
    private var pollGeneration: Long? = null
    @Volatile private var activeJoinJob: Job? = null
    private val joinAttempts = JoinAttemptGate()
    @Volatile private var foreground = false
    @Volatile private var service = false
    @Volatile private var worker = false
    /** Keeps the endpoint up for a while after a join code was shown, so the admin's phone can take the request. */
    @Volatile private var codeHoldUntil = 0L
    private val status0 = MutableStateFlow(PeerStatus())
    val status: StateFlow<PeerStatus> get() = status0

    init {
        repo.addSharedChangeListener { if (repo.state.value.group != null) scope.launch { delay(2_000); syncOnce() } }
    }

    // ---- Identity ----

    private fun keyBytes(): ByteArray = synchronized(keyLock) {
        cachedKey ?: (vault.getBytes(KEY_IROH_KEY) ?: SecretKey.generate().toBytes().also { vault.putBytes(KEY_IROH_KEY, it) }).also { cachedKey = it }
    }

    private fun signer(): MessageSigner = IrohMessageSigner(keyBytes())

    /** Loads (or creates) this phone's iroh key and records its endpoint id as the self member. Safe to call any time; call on every start. */
    fun ensureIdentity(): String? = runCatching {
        val id = SecretKey.fromBytes(keyBytes()).public().toString()
        repo.mutate { ensureSelf(it, id) }
        id
    }.getOrNull()

    // ---- Circle lifecycle ----

    suspend fun createCircle(name: String) = guarded {
        if (ensureIdentity() == null) throw IllegalStateException("Endpoint id not set; call ensureSelf first")
        repo.mutate { createGroup(it, name) }
        runCatching { ensureEndpoint() }
    }

    /** Admin only. [rotate] = true starts a new code so outstanding unused codes stop working. Throws IllegalStateException (with a user-presentable message) when this phone is not the admin or cannot start networking. */
    suspend fun issueJoinCode(rotate: Boolean = false): String = withContext(Dispatchers.IO) {
        if (!isAdmin(repo.state.value)) throw IllegalStateException("Only the circle admin can make a code.")
        val t = try { ensureEndpoint() } catch (e: Exception) { throw CircleActionException(friendlyError(e, app), e) }
        runCatching { withTimeout(10_000) { t.online() } }
        val ticket = t.ticket
        val now = System.currentTimeMillis()
        var issued: Pair<BuddyState, String>? = null
        try {
            repo.mutate { s -> coreIssueJoinCode(if (rotate) rotateJoinCode(s, now) else s, ticket, now).also { issued = it }.first }
        } catch (e: IllegalArgumentException) { throw IllegalStateException(e.message, e) }
        val (state, code) = issued ?: throw IllegalStateException("Couldn't make a code.")
        codeHoldUntil = now + CODE_HOLD_MS
        status0.update { it.copy(joinCodeExpiresAt = state.joinNonceExpiresAt, lastError = null) }
        code
    }

    /** Parses [code], sends a join request to the admin and, while the admin has not decided, keeps checking in the background. Progress is in `status.joinProgress`. */
    suspend fun joinWithCode(code: String): Unit = withContext(Dispatchers.IO) {
        val operation = currentCoroutineContext()[Job]
        val attempt = joinAttempts.begin {
            activeJoinJob?.cancel()
            activeJoinJob = null
            pollJob?.cancel(); pollJob = null; pollGeneration = null
            // A push for the previous code must not be accepted under the replacement generation.
            clearPendingJoin()
            activeJoinJob = operation
        }
        try {
            if (repo.state.value.group != null) return@withContext refuseJoin("You are already in a circle. Leave it first.", attempt, clear = false)
            val parsed = try { parseJoinCode(code) } catch (e: IllegalArgumentException) { return@withContext refuseJoin(e.message ?: "That circle code is not valid.", attempt) }
            if (parsed.expired) return@withContext refuseJoin("This circle code has expired. Ask the admin for a new one.", attempt)
            if (ensureIdentity() == null) throw IllegalStateException("Endpoint id not set; call ensureSelf first")
            joinAttempts.ifCurrent(attempt) {
                joinPrefs.edit().putString(PREF_CODE, code.trim()).putLong(PREF_POLL_UNTIL, System.currentTimeMillis() + PENDING_JOIN_TTL_MS).apply()
                status0.update { it.copy(joinProgress = JoinProgress.Requesting, lastError = null) }
            } ?: return@withContext
            when (val r = requestJoinOnce(parsed.payload, attempt)) {
                is JoinStep.Waiting -> startPolling(attempt)
                is JoinStep.Failed -> refuseJoin(friendlyError(r.cause, app), attempt)
                else -> Unit
            }
        } catch (e: Exception) { rethrowIfCancelled(e); refuseJoin(friendlyError(e, app), attempt) }
        finally { joinAttempts.ifCurrent(attempt) { if (activeJoinJob === operation) activeJoinJob = null } }
    }

    /** Stops waiting for a join decision and clears the join state shown by `status.joinProgress` (also dismisses a finished or refused result). */
    fun userFacingError(cause: Throwable): String = userFacingCircleError(cause, app)

    fun cancelJoin() {
        joinAttempts.cancel {
            activeJoinJob?.cancel(); activeJoinJob = null
            pollJob?.cancel(); pollJob = null; pollGeneration = null
            clearPendingJoin()
            status0.update { it.copy(joinProgress = JoinProgress.None, lastError = null) }
        }
        scope.launch { releaseEndpointIfIdle() }
    }

    /** Checks the retained request without requiring the code to be pasted again. */
    suspend fun checkPendingJoin() = withContext(Dispatchers.IO) {
        val attempt = joinAttempts.current()
        val operation = currentCoroutineContext()[Job]
        joinAttempts.ifCurrent(attempt) { activeJoinJob = operation } ?: return@withContext
        try {
            when (val result = requestJoinOnce(pendingPayload(attempt), attempt)) {
                is JoinStep.Waiting -> startPolling(attempt)
                is JoinStep.Failed -> joinAttempts.ifCurrent(attempt) {
                    val message = friendlyError(result.cause, app)
                    setError(message)
                    throw CircleActionException(message, result.cause)
                }
                is JoinStep.Refused -> joinAttempts.ifCurrent(attempt) { throw CircleActionException(userFacingCircleError(IllegalStateException(result.reason), app)) }
                else -> Unit
            }
        } finally { joinAttempts.ifCurrent(attempt) { if (activeJoinJob === operation) activeJoinJob = null } }
    }

    suspend fun retryMember(memberId: String) = guarded { syncOnce(only = memberId) }

    suspend fun approveJoin(requestId: String) = guarded {
        var result: ApproveResult? = null
        repo.mutate { s -> coreApproveJoin(s, requestId).also { result = it }.state }
        val approved = result ?: return@guarded
        // Local approval is complete. Delivery continues without holding the form open.
        scope.launch {
            runCatching { withTimeout(15_000) { exchangeWith(approved.member, approved.joinOk, 12_000L) } }
            syncOnce()
        }
    }

    suspend fun declineJoin(requestId: String) = guarded { repo.mutate { coreDeclineJoin(it, requestId) } }

    /** Leaves the circle. [successorId] is only used when this phone is the admin (otherwise ignored); the oldest remaining member is chosen if null. Personal data stays on the phone. */
    suspend fun leaveCircle(successorId: String? = null) = guarded {
        if (repo.state.value.group == null) return@guarded
        ensureIdentity()
        val msgSigner = signer()
        var result: LeaveResult? = null
        repo.mutate { s -> leaveGroup(s, successorId, msgSigner).also { result = it }.state }
        val left = result ?: return@guarded
        cancelJoin()
        status0.update { it.copy(members = emptyMap(), joinCodeExpiresAt = 0) }
        // Complete local cleanup before returning; a late notice must not change a newly created circle.
        if (repo.state.value.stayReachable) {
            repo.mutate { it.copy(stayReachable = false) }
            service = false
            runCatching { app.stopService(Intent(app, BuddyLinkService::class.java)) }
            status0.update { it.copy(serviceRunning = false) }
        }
        scope.launch {
        if (left.notices.isNotEmpty()) {
            val t = runCatching { ensureEndpoint() }.getOrNull()
            if (t != null) {
                // Best effort and not queued: a member we could not reach learns later when we answer "not in a circle".
                val gate = Semaphore(MAX_CONCURRENT)
                coroutineScope {
                    left.notices.map { n -> async { gate.withPermit { runCatching { withTimeout(10_000) { exchangeWith(n.member, n.message, 8_000L) } } } } }.awaitAll()
                }
            }
        }
        releaseEndpointIfIdle()
        }
    }

    suspend fun transferAdmin(memberId: String) = guarded {
        repo.mutate { coreTransferAdmin(it, memberId) }
        scope.launch { syncOnce() }
    }

    suspend fun setVisibility(v: Visibility) = guarded {
        repo.mutate { coreSetVisibility(it, v) }
        scope.launch { syncOnce() }
    }

    /** Queues a 30 minute nudge for [memberId] (at most one per member per day) and sends it as soon as their phone is reachable. */
    suspend fun nudge(memberId: String) = guarded {
        val now = System.currentTimeMillis()
        if (!canNudge(repo.state.value, memberId, now)) throw IllegalStateException("You already nudged them today, or they are not in your circle.")
        repo.mutate { s -> queueNudge(s, memberId, NUDGE_MINUTES, now).copy(lastNudgedDate = com.spendingbuddy.app.todayIndia().toString()) }
        scope.launch { syncOnce(only = memberId) }
    }

    suspend fun suggestBudget(targetId: String, dailyPaise: Long, weeklyPaise: Long) = guarded {
        repo.mutate { queueSuggestion(it, targetId, dailyPaise, weeklyPaise) }
        scope.launch { syncOnce(only = targetId) }
    }

    suspend fun respondToSuggestion(id: String, accepted: Boolean) = guarded {
        val senderId = repo.state.value.suggestions.firstOrNull { it.id == id }?.senderId
        var ok = false
        repo.mutate { s -> (coreRespondToSuggestion(s, id, accepted)?.also { ok = true }) ?: s }
        if (!ok) throw IllegalStateException("That suggestion is no longer available.")
        scope.launch { syncOnce(only = senderId) }
    }

    fun syncNow() { scope.launch { syncOnce() } }

    fun setStayReachable(enabled: Boolean) {
        scope.launch {
            // Lifecycle/service callers report through status; UI callers await the same operation below.
            runCatching { setStayReachableAwait(enabled) }
        }
    }

    suspend fun setStayReachableAwait(enabled: Boolean) = guarded {
            if (enabled) {
                try {
                    ContextCompat.startForegroundService(app, Intent(app, BuddyLinkService::class.java))
                    repo.mutate { it.copy(stayReachable = true) }
                    service = true
                    status0.update { it.copy(serviceRunning = true) }
                } catch (e: Exception) {
                    rethrowIfCancelled(e)
                    service = false
                    runCatching { app.stopService(Intent(app, BuddyLinkService::class.java)) }
                    status0.update { it.copy(lastError = app.getString(R.string.circle_reachable_error), serviceRunning = false) }
                    throw CircleActionException(app.getString(R.string.circle_reachable_error), e)
                }
            } else {
                repo.mutate { it.copy(stayReachable = false) }
                service = false
                app.stopService(Intent(app, BuddyLinkService::class.java))
                status0.update { it.copy(serviceRunning = false) }
                scope.launch { releaseEndpointIfIdle() }
            }
    }

    // ---- Lifecycle hooks ----

    /** Called by BuddyApplication's process lifecycle observer. */
    fun onForeground() {
        foreground = true
        scope.launch {
            runCatching { ensureEndpoint() }
            resumeJoinIfPending()
            syncOnce()
            periodicJob?.cancel()
            periodicJob = launch { while (foreground) { delay(60_000); syncOnce() } }
        }
    }
    fun onBackground() {
        foreground = false
        scope.launch { delay(20_000); releaseEndpointIfIdle() }
    }
    fun setServiceHolder(running: Boolean) { service = running; status0.update { it.copy(serviceRunning = running) }; scope.launch { if (running) runCatching { ensureEndpoint() } else releaseEndpointIfIdle() } }
    fun setWorkerHolder(running: Boolean) { worker = running; scope.launch { if (running) runCatching { ensureEndpoint() } else releaseEndpointIfIdle() } }
    suspend fun syncForWorker() { syncOnce(timeoutMs = 12_000L) }

    // ---- Endpoint ----

    private suspend fun ensureEndpoint(): IrohTransport = endpointLock.withLock {
        check((app as? com.spendingbuddy.app.BuddyApplication)?.awaitReady() != false) { "Penny Circle is still getting ready. Try again." }
        transport?.let { if (!it.endpoint.isClosed()) { refreshStatus(it); return@withLock it } }
        status0.update { it.copy(phase = LinkPhase.Starting) }
        val t = try { IrohTransport.bind(keyBytes(), minimal = false) } catch (e: Exception) {
            status0.update { it.copy(phase = LinkPhase.Error, lastError = friendlyError(e, app)) }; throw e
        }
        transport = t
        refreshStatus(t)
        runCatching { repo.mutate { ensureSelf(it, t.endpointId, t.relayUrl, t.directAddresses) } }
        acceptJob?.cancel()
        acceptJob = scope.launch { acceptLoop(t) }
        t
    }

    private fun refreshStatus(t: IrohTransport) { status0.update { it.copy(phase = LinkPhase.Online, endpointIdShort = t.endpointIdShort) } }

    private suspend fun releaseEndpointIfIdle() {
        if (foreground || service || worker || System.currentTimeMillis() < codeHoldUntil || joinPending()) return
        endpointLock.withLock {
            transport?.let { runCatching { it.shutdown() } }
            transport = null; acceptJob?.cancel(); acceptJob = null
            status0.update { it.copy(phase = LinkPhase.Off, endpointIdShort = null) }
        }
    }

    // ---- Incoming ----

    private suspend fun acceptLoop(t: IrohTransport) {
        while (!t.endpoint.isClosed()) {
            try {
                val incoming = t.acceptNext() ?: break
                val conn = runCatching { t.accept(incoming) }.getOrNull() ?: continue
                scope.launch {
                    serveGate.withPermit {
                        try { withTimeout(SERVE_TIMEOUT_MS) { conn.serve { bytes -> processIncoming(conn.remoteId, bytes) } } } catch (_: Exception) { }
                        conn.close()
                    }
                }
            } catch (_: Exception) { if (!t.endpoint.isClosed()) delay(500) }
        }
    }

    private fun errorReply(text: String): ByteArray = encodeMessage(SyncMessage(type = "error", sender = repo.state.value.name.take(MAX_NAME_LENGTH), error = text))

    private suspend fun processIncoming(remoteId: String, bytes: ByteArray): ByteArray {
        val now = System.currentTimeMillis()
        val message = try { decodeMessage(bytes) } catch (e: IllegalArgumentException) {
            return errorReply("That message couldn't be read. Update Penny Circle on both phones and try again.")
        } catch (e: Exception) { return errorReply("That message couldn't be read.") }
        if (message.type == "join_ok") return handleJoinOkPush(remoteId, message, now)
        val state = repo.state.value
        val group = state.group ?: return errorReply(NOT_IN_CIRCLE_TEXT)
        if (message.type == "join_request") {
            var outcome: JoinRequestOutcome? = null
            try { repo.mutate { s -> handleJoinRequest(s, remoteId, message, now).also { outcome = it }.state } } catch (e: Exception) { return errorReply("Couldn't take that request. Try again.") }
            val o = outcome ?: return errorReply("Couldn't take that request. Try again.")
            o.request?.let { BuddyNotifications.notifyJoinRequest(app, it.displayName) }
            return encodeMessage(o.reply)
        }
        if (message.type == "error") return errorReply("Unexpected message.")
        if (message.groupId != group.id) return errorReply(NOT_IN_CIRCLE_TEXT)
        if (!isSenderAllowed(message.type, remoteId, state)) return errorReply(NOT_A_MEMBER_TEXT)
        var applied: Pair<BuddyState, List<MergeNotice>>? = null
        try { repo.mutate { s -> applyIncoming(s, remoteId, message, now).also { applied = it }.first } } catch (e: IllegalArgumentException) {
            return errorReply(e.message ?: "That message was not accepted.")
        } catch (e: Exception) { return errorReply("Couldn't save that update. Try again.") }
        applied?.second?.let { notifyAll(it) }
        markReached(remoteId, now, null)
        val next = repo.state.value
        val reply = fanOut(next, now).firstOrNull { it.member.endpointId == remoteId }?.message
            ?: SyncMessage(type = "respond", groupId = group.id)
        return encodeMessage(reply)
    }

    /** Joiner side: the admin pushes `join_ok` right after approving, so waiting phones do not have to poll first. */
    private fun handleJoinOkPush(remoteId: String, message: SyncMessage, now: Long): ByteArray {
        val attempt = joinAttempts.current()
        return joinAttempts.ifCurrent(attempt) {
        val payload = pendingPayload(attempt) ?: return@ifCurrent errorReply("This phone is not waiting to join a circle.")
        var reply: JoinReply? = null
        try { repo.mutate { s -> applyJoinReply(s, remoteId, message, payload, now).also { reply = it }.let { r -> if (r is JoinReply.Joined) r.state else s } } } catch (e: Exception) {
            return@ifCurrent errorReply("Couldn't save the circle. Try again.")
        }
        when (val r = reply) {
            is JoinReply.Joined -> { finishJoin(attempt); encodeMessage(SyncMessage(type = "respond", groupId = payload.groupId)) }
            is JoinReply.Refused -> errorReply(r.reason)
            else -> errorReply("This phone is not waiting to join a circle.")
        }
        } ?: errorReply("This phone is not waiting to join a circle.")
    }

    private fun notifyAll(notices: List<MergeNotice>) {
        notices.forEach { n ->
            when (n.kind) {
                "nudge" -> n.nudge?.let { BuddyNotifications.showNudge(app, it) }
                "budget_suggestion" -> BuddyNotifications.notifyActivity(app, app.getString(R.string.circle_event_suggest_title, n.name), app.getString(R.string.circle_event_suggest_body), n.memberId)
                "suggestion_reply" -> BuddyNotifications.notifyActivity(app, app.getString(if (n.event?.accepted == true) R.string.circle_event_accept_title else R.string.circle_event_keep_title, n.name), app.getString(R.string.circle_event_reply_body), n.memberId)
                "leave" -> BuddyNotifications.notifyActivity(app, app.getString(R.string.circle_event_leave_title, n.name), app.getString(R.string.circle_event_leave_body), n.memberId)
                "friend_checkin" -> BuddyNotifications.notifyActivity(app, app.getString(R.string.circle_event_finish_title, n.name), app.getString(R.string.circle_event_finish_body), n.memberId)
            }
        }
    }

    // ---- Outgoing sync ----

    private sealed interface MemberResult {
        data object Ok : MemberResult
        data class Fail(val cause: Throwable) : MemberResult
    }

    private fun markReached(id: String, now: Long, viaRelay: Boolean?) = status0.update {
        val prev = it.members[id]
        it.copy(members = it.members + (id to MemberLink(true, now, viaRelay ?: prev?.viaRelay, null)), lastExchangeAt = now)
    }

    private fun markFailed(id: String, e: Throwable) = status0.update {
        val prev = it.members[id] ?: MemberLink()
        it.copy(members = it.members + (id to prev.copy(reachable = false, error = friendlyError(e, app))))
    }

    private fun ticketFor(m: Member): String =
        EndpointTicket.fromAddr(EndpointAddr(EndpointId.fromString(m.endpointId), m.relayUrl, m.directAddresses)).toString()

    /** One request/reply with [member]; returns the decoded reply and whether the path was via relay. */
    private suspend fun exchangeWith(member: Member, message: SyncMessage, timeoutMs: Long): Pair<SyncMessage, Boolean?> {
        val t = ensureEndpoint()
        val conn = withTimeout(timeoutMs) { t.connect(ticketFor(member)) }
        try {
            if (conn.remoteId != member.endpointId) throw IllegalStateException("Connected to an unexpected phone.")
            val reply = decodeMessage(conn.exchange(encodeMessage(message), timeoutMs))
            return reply to conn.viaRelay
        } finally { conn.close() }
    }

    private suspend fun syncMember(item: FanOutItem, timeoutMs: Long): MemberResult {
        val id = item.member.endpointId
        return try {
            val (reply, relay) = exchangeWith(item.member, item.message, timeoutMs)
            val now = System.currentTimeMillis()
            if (isNotInCircleReply(reply)) {
                handleRemoteNotInCircle(item.member, now)
                markFailed(id, IllegalStateException("They are not in the circle right now."))
                return MemberResult.Ok
            }
            if (reply.type == "error") throw IllegalStateException(reply.error ?: "That phone sent an unexpected reply.")
            var applied: Pair<BuddyState, List<MergeNotice>>? = null
            repo.mutate { s -> applyIncoming(s, id, reply, now).also { applied = it }.first }
            applied?.second?.let { notifyAll(it) }
            markReached(id, now, relay)
            MemberResult.Ok
        } catch (e: Exception) {
            rethrowIfCancelled(e); markFailed(id, e); MemberResult.Fail(e)
        }
    }

    /** A member answered "not in a circle": mark them as left once they have had time to finish joining. */
    private fun handleRemoteNotInCircle(member: Member, now: Long) {
        val current = repo.state.value.members.firstOrNull { it.endpointId == member.endpointId } ?: return
        if (!shouldTreatAsLeft(current, now)) return
        val g = repo.state.value.group ?: return
        val synthetic = SyncMessage(type = "leave", groupId = g.id, events = listOf(newEvent("leave", now, target = "")))
        runCatching { repo.mutate { s -> applyIncoming(s, member.endpointId, synthetic, now).first } }
    }

    private suspend fun syncOnce(timeoutMs: Long = 20_000L, only: String? = null): Unit = exchangeLock.withLock {
        val first = repo.state.value
        if (first.group == null) {
            val attempt = joinAttempts.current()
            if (joinPending()) runCatching { requestJoinOnce(pendingPayload(attempt), attempt) }
            return@withLock
        }
        val t = runCatching { ensureEndpoint() }.getOrElse { setError(friendlyError(it, app)); return@withLock }
        val now = System.currentTimeMillis()
        runCatching { repo.mutate { purgeExpiredJoins(ensureSelf(it, t.endpointId, t.relayUrl, t.directAddresses, now), now) } }
        val items = fanOut(repo.state.value, now).filter { only == null || it.member.endpointId == only }
        if (items.isEmpty()) return@withLock
        status0.update { it.copy(syncing = true) }
        val gate = Semaphore(MAX_CONCURRENT)
        val results = try {
            coroutineScope { items.map { item -> async { gate.withPermit { syncMember(item, timeoutMs) } } }.awaitAll() }
        } finally { status0.update { it.copy(syncing = false) } }
        val failures = results.filterIsInstance<MemberResult.Fail>()
        status0.update { s ->
            s.copy(phase = if (failures.size == results.size && failures.all { it.cause.message?.contains("network", true) == true }) LinkPhase.NoNetwork else LinkPhase.Online,
                lastError = if (failures.isNotEmpty() && failures.size == results.size) friendlyError(failures.first().cause, app) else null)
        }
    }

    // ---- Joining ----

    private sealed interface JoinStep {
        data object Joined : JoinStep
        data class Waiting(val expiresAt: Long) : JoinStep
        data class Refused(val reason: String) : JoinStep
        data class Failed(val cause: Throwable) : JoinStep
    }

    private fun pendingPayload(attempt: Long): JoinCodePayload? = joinAttempts.ifCurrent(attempt) {
        val code = joinPrefs.getString(PREF_CODE, null) ?: return@ifCurrent null
        if (System.currentTimeMillis() > joinPrefs.getLong(PREF_POLL_UNTIL, 0)) { clearPendingJoin(); return@ifCurrent null }
        runCatching { parseJoinCode(code).payload }.getOrNull()
    }

    private fun joinPending(): Boolean = repo.state.value.group == null && joinPrefs.getString(PREF_CODE, null) != null &&
        System.currentTimeMillis() <= joinPrefs.getLong(PREF_POLL_UNTIL, 0)

    private fun clearPendingJoin() { joinPrefs.edit().remove(PREF_CODE).remove(PREF_POLL_UNTIL).apply() }

    private fun refuseJoin(reason: String, attempt: Long, clear: Boolean = true) {
        joinAttempts.ifCurrent(attempt) {
        val message = userFacingCircleError(IllegalStateException(reason), app)
        if (clear) clearPendingJoin()
        pollJob?.cancel(); pollJob = null; pollGeneration = null
        status0.update { it.copy(joinProgress = JoinProgress.Refused(message), lastError = message) }
        scope.launch { releaseEndpointIfIdle() }
        }
    }

    private fun finishJoin(attempt: Long) {
        joinAttempts.ifCurrent(attempt) {
        clearPendingJoin()
        pollJob?.cancel(); pollJob = null; pollGeneration = null
        status0.update { it.copy(joinProgress = JoinProgress.Joined, lastError = null) }
        scope.launch { syncOnce() }
        }
    }

    /** One `join_request` to the admin named in [payload]. Updates `joinProgress` for Waiting/Joined/Refused; transport failures come back as [JoinStep.Failed] for the caller to decide. */
    private suspend fun requestJoinOnce(payload: JoinCodePayload?, attempt: Long): JoinStep = joinLock.withLock {
        if (joinAttempts.ifCurrent(attempt) { true } != true) throw CancellationException("Join request cancelled")
        if (payload == null) return@withLock JoinStep.Refused("No join request is pending.")
        if (pendingPayload(attempt) != payload) throw CancellationException("Join request cancelled")
        if (repo.state.value.group != null) return@withLock JoinStep.Joined
        try {
            val t = ensureEndpoint()
            val now = System.currentTimeMillis()
            val request = buildJoinRequest(repo.state.value, payload, t.relayUrl, t.directAddresses, now)
            val conn = withTimeout(20_000) { t.connect(payload.ticket) }
            val reply = try {
                if (conn.remoteId != payload.adminId) throw IllegalStateException("Connected to an unexpected phone.")
                decodeMessage(conn.exchange(encodeMessage(request), 20_000L))
            } finally { conn.close() }
            var result: JoinReply? = null
            joinAttempts.ifCurrent(attempt) {
            if (pendingPayload(attempt) != payload) throw CancellationException("Join request cancelled")
            repo.mutate { s -> if (s.group != null) s else applyJoinReply(s, payload.adminId, reply, payload, now).also { result = it }.let { r -> if (r is JoinReply.Joined) r.state else s } }
            when (val r = result) {
                is JoinReply.Pending -> { status0.update { it.copy(joinProgress = JoinProgress.Waiting(r.expiresAt), lastError = null) }; JoinStep.Waiting(r.expiresAt) }
                is JoinReply.Joined -> { finishJoin(attempt); JoinStep.Joined }
                is JoinReply.Refused -> { refuseJoin(r.reason, attempt); JoinStep.Refused(r.reason) }
                null -> JoinStep.Joined
            }
            } ?: throw CancellationException("Join request cancelled")
        } catch (e: Exception) { rethrowIfCancelled(e); currentCoroutineContext().ensureActive(); JoinStep.Failed(e) }
    }

    private fun startPolling(generation: Long) {
        joinAttempts.ifCurrent(generation) {
        if (pollJob?.isActive == true && pollGeneration == generation) return@ifCurrent
        pollJob?.cancel()
        pollGeneration = generation
        pollJob = scope.launch {
            val operation = currentCoroutineContext()[Job]
            try {
            var pollNumber = 0
            while (isActive) {
                delay(joinPollDelayMs(pollNumber++))
                val payload = pendingPayload(generation)
                if (joinAttempts.ifCurrent(generation) { true } != true) break
                if (payload == null || repo.state.value.group != null) {
                    if (repo.state.value.group == null) refuseJoin("The request expired. Ask the admin for a new code.", generation)
                    break
                }
                when (requestJoinOnce(payload, generation)) {
                    is JoinStep.Waiting, is JoinStep.Failed -> Unit   // transient failures keep waiting until the request lapses
                    else -> break
                }
            }
            } finally {
                joinAttempts.ifCurrent(generation) {
                    if (pollJob === operation) { pollJob = null; pollGeneration = null }
                }
            }
        }
        }
    }

    private fun resumeJoinIfPending() {
        val attempt = joinAttempts.current()
        joinAttempts.ifCurrent(attempt) {
        if (joinPending() && pendingPayload(attempt) != null) {
            if (status0.value.joinProgress !is JoinProgress.Waiting) status0.update { it.copy(joinProgress = JoinProgress.Requesting) }
            startPolling(attempt)
        }
        }
    }

    // ---- Helpers ----

    /** A withTimeout timeout is also a CancellationException; only a real cancellation must propagate. */
    private fun rethrowIfCancelled(e: Exception) { if (e is CancellationException && e !is TimeoutCancellationException) throw e }

    private fun setError(message: String) { status0.update { it.copy(lastError = message) } }

    private suspend fun guarded(block: suspend () -> Unit) = withContext(Dispatchers.IO) {
        try { block() } catch (e: Exception) { rethrowIfCancelled(e); val msg = friendlyError(e, app); setError(msg); throw CircleActionException(msg, e) }
    }

    companion object {
        const val KEY_IROH_KEY = "iroh-secret-key"
        private const val PREF_CODE = "pending_code"
        private const val PREF_POLL_UNTIL = "pending_until"
        private const val NUDGE_MINUTES = 30
        private const val MAX_CONCURRENT = 4
        private const val MAX_SERVING = 8
        private const val SERVE_TIMEOUT_MS = 30_000L
        private const val CODE_HOLD_MS = 15 * 60_000L
        @Volatile private var instance: PeerSyncManager? = null
        fun get(context: Context): PeerSyncManager = instance ?: synchronized(this) { instance ?: PeerSyncManager(context.applicationContext).also { instance = it } }
    }
}
