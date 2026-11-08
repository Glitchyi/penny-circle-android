package com.spendingbuddy.app.p2p

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.spendingbuddy.app.BuddyRepository
import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.Nudge
import com.spendingbuddy.app.FriendSnapshot
import com.spendingbuddy.app.PeerEvent
import com.spendingbuddy.app.PeerLink
import com.spendingbuddy.app.BudgetSuggestion
import com.spendingbuddy.app.OutgoingNudge
import com.spendingbuddy.app.SecretVault
import com.spendingbuddy.app.BuddyNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import computer.iroh.EndpointTicket
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean

/** Lifecycle of this phone's iroh endpoint. */
enum class LinkPhase { Off, Starting, Online, NoNetwork, Error }

data class InviteInfo(val code: String, val expiresAt: Long)

data class PeerStatus(
    val phase: LinkPhase = LinkPhase.Off,
    val endpointIdShort: String? = null,
    val friendReachable: Boolean = false,
    val lastExchangeAt: Long = 0,
    val viaRelay: Boolean? = null,
    val syncing: Boolean = false,
    val lastError: String? = null,
    val invite: InviteInfo? = null,
    val serviceRunning: Boolean = false
)

class PeerSyncManager private constructor(private val app: Context) {
    private val repo = BuddyRepository.get(app)
    private val vault = SecretVault(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val endpointLock = Mutex()
    private val exchangeLock = Mutex()
    private var transport: IrohTransport? = null
    private var acceptJob: Job? = null
    private var periodicJob: Job? = null
    private var pendingSecret: String? = null
    private var pendingExpiry: Long = 0
    private var foreground = false
    private var service = false
    private var worker = false
    private val started = AtomicBoolean(false)
    private val status0 = MutableStateFlow(PeerStatus())
    val status: StateFlow<PeerStatus> get() = status0

    init {
        repo.addSharedChangeListener { if (repo.state.value.peer != null) scope.launch { delay(2_000); syncOnce() } }
    }

    suspend fun createInvite(): Result<InviteInfo> = withContext(Dispatchers.IO) {
        val state = repo.state.value
        val linkedPeer = state.peer
        if (linkedPeer != null) return@withContext Result.failure(IllegalStateException("Unlink ${linkedPeer.name.ifBlank { "your friend" }} first."))
        try {
            val t = ensureEndpoint()
            runCatching { withTimeout(10_000) { t.online() } }
            val expires = System.currentTimeMillis() + 15 * 60_000L
            val secret = randomPairingSecret()
            pendingSecret = secret
            pendingExpiry = expires
            val invite = InviteInfo(encodeInvite(t.ticket, secret, state.name, expires), expires)
            status0.value = status0.value.copy(invite = invite, phase = LinkPhase.Online, lastError = null)
            scope.launch {
                delay(15 * 60_000L)
                if (pendingSecret == secret && pendingExpiry == expires) cancelInvite()
            }
            Result.success(invite)
        } catch (e: Exception) {
            status0.value = status0.value.copy(phase = LinkPhase.Error, lastError = friendly(e))
            Result.failure(e)
        }
    }

    fun cancelInvite() {
        pendingSecret = null; pendingExpiry = 0
        status0.value = status0.value.copy(invite = null)
        scope.launch { releaseEndpointIfIdle() }
    }

    suspend fun joinWithCode(code: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val parsed = parseInvite(code)
            if (parsed.expired) return@withContext Result.failure(IllegalArgumentException("This code expired. Ask ${parsed.payload.name.ifBlank { "your friend" }} to show a new one."))
            val t = ensureEndpoint()
            val conn = withTimeout(20_000) { t.connect(parsed.payload.ticket) }
            val request = SyncMessage("1".toInt(), "pair", repo.state.value.name, snapshotOf(repo.state.value), endpointAddr = t.ticket, pairingSecret = parsed.payload.secret)
            val reply = decodeMessage(conn.exchange(encodeMessage(request)))
            conn.close()
            if (reply.type != "pair_ok" || reply.snapshot == null) throw IllegalStateException(reply.error ?: "Your friend rejected this invite.")
            val replySnapshot = reply.snapshot ?: throw IllegalStateException("Your friend sent an incomplete pairing reply.")
            repo.mutate { s -> s.copy(peer = PeerLink(conn.remoteId, reply.sender, System.currentTimeMillis(), null, emptyList()), friend = FriendSnapshot(replySnapshot.name, replySnapshot.expenses, replySnapshot.checkIns, replySnapshot.dailyLimitPaise, replySnapshot.weeklyLimitPaise, replySnapshot.revision, System.currentTimeMillis())) }
            status0.value = status0.value.copy(phase = LinkPhase.Online, lastError = null)
            syncOnce()
            Result.success(reply.sender)
        } catch (e: Exception) {
            status0.value = status0.value.copy(phase = if (transport == null) LinkPhase.Off else LinkPhase.Error, lastError = friendly(e))
            Result.failure(IllegalStateException(friendly(e), e))
        }
    }

    fun syncNow() { scope.launch { syncOnce() } }

    fun sendNudge(minutes: Int) {
        require(minutes == 30 || minutes == 60)
        scope.launch {
            val now = System.currentTimeMillis(); val today = LocalDate.now(com.spendingbuddy.app.INDIA_ZONE).toString()
            repo.mutate { s ->
                val event = newEvent("nudge", now, minutes)
                s.copy(outbox = (s.outbox + event).takeLast(500), outgoingNudge = OutgoingNudge(event.id, minutes, now), lastNudgedDate = today)
            }
            syncOnce()
        }
    }

    fun suggestBudget(dailyPaise: Long, weeklyPaise: Long) {
        scope.launch {
            val now = System.currentTimeMillis(); val event = newEvent("budget_suggestion", now, dailyPaise = dailyPaise, weeklyPaise = weeklyPaise)
            repo.mutate { s -> s.copy(outbox = (s.outbox + event).takeLast(500), sentSuggestion = BudgetSuggestion(event.id, dailyPaise, weeklyPaise, s.name, now)) }
            syncOnce()
        }
    }

    fun respondToSuggestion(id: String, accept: Boolean) {
        scope.launch {
            val suggestion = repo.state.value.suggestions.firstOrNull { it.id == id } ?: return@launch
            if (accept) repo.setBudgets(suggestion.dailyPaise, suggestion.weeklyPaise)
            val reply = newEvent("suggestion_reply", suggestionId = id, accepted = accept)
            repo.mutate { s -> s.copy(suggestions = s.suggestions.map { if (it.id == id) it.copy(status = if (accept) "accepted" else "declined") else it }, outbox = (s.outbox + reply).takeLast(500)) }
            syncOnce()
        }
    }

    fun setStayReachable(enabled: Boolean) {
        scope.launch {
            if (enabled) {
                try {
                    androidx.core.content.ContextCompat.startForegroundService(app, android.content.Intent(app, BuddyLinkService::class.java))
                    service = true
                    repo.mutate { it.copy(stayReachable = true) }
                    status0.value = status0.value.copy(serviceRunning = true)
                } catch (e: Exception) {
                    repo.mutate { it.copy(stayReachable = false) }
                    status0.value = status0.value.copy(lastError = "Couldn't keep the link reachable. Try again while Spending Buddy is open.", serviceRunning = false)
                }
            } else {
                repo.mutate { it.copy(stayReachable = false) }
                service = false
                app.stopService(android.content.Intent(app, BuddyLinkService::class.java))
                status0.value = status0.value.copy(serviceRunning = false)
                releaseEndpointIfIdle()
            }
        }
    }

    fun unlink() {
        scope.launch {
            val old = repo.state.value.peer
            if (old != null) {
                repo.mutate { s -> s.copy(outbox = (s.outbox + newEvent("unlink")).takeLast(500)) }
                runCatching { syncOnce(timeoutMs = 5_000) }
            }
            repo.mutate { it.copy(peer = null, friend = null, outbox = emptyList(), seenEventIds = emptyList(), incomingNudge = null, outgoingNudge = null, suggestions = emptyList(), sentSuggestion = null, stayReachable = false) }
            cancelInvite()
        }
    }

    /** Called by BuddyApplication's process lifecycle observer. */
    fun onForeground() {
        foreground = true
        scope.launch {
            ensureEndpoint()
            if (repo.state.value.peer != null) syncOnce()
            periodicJob?.cancel()
            periodicJob = launch { while (foreground) { delay(60_000); if (repo.state.value.peer != null) syncOnce() } }
        }
    }
    fun onBackground() {
        foreground = false
        scope.launch { delay(20_000); releaseEndpointIfIdle() }
    }
    fun setServiceHolder(running: Boolean) { service = running; status0.value = status0.value.copy(serviceRunning = running); scope.launch { if (running) ensureEndpoint() else releaseEndpointIfIdle() } }
    fun setWorkerHolder(running: Boolean) { worker = running; scope.launch { if (running) ensureEndpoint() else releaseEndpointIfIdle() } }
    suspend fun syncForWorker() { syncOnce(timeoutMs = 25_000L) }

    private suspend fun ensureEndpoint(): IrohTransport = endpointLock.withLock {
        transport?.let { if (!it.endpoint.isClosed()) { refreshStatus(it); return@withLock it } }
        status0.value = status0.value.copy(phase = LinkPhase.Starting)
        val key = vault.getBytes(KEY_IROH_KEY) ?: computer.iroh.SecretKey.generate().toBytes().also { vault.putBytes(KEY_IROH_KEY, it) }
        val t = IrohTransport.bind(key, minimal = false)
        transport = t
        refreshStatus(t)
        acceptJob?.cancel()
        acceptJob = scope.launch { acceptLoop(t) }
        t
    }

    private fun refreshStatus(t: IrohTransport) { status0.value = status0.value.copy(phase = LinkPhase.Online, endpointIdShort = t.endpointIdShort) }

    private suspend fun acceptLoop(t: IrohTransport) {
        while (!t.endpoint.isClosed()) {
            try {
                val incoming = t.acceptNext() ?: break
                val conn = runCatching { t.accept(incoming) }.getOrNull() ?: continue
                val state = repo.state.value
                val allowed = state.peer?.endpointId == conn.remoteId || (pendingSecret != null && pendingExpiry > System.currentTimeMillis())
                if (!allowed) { conn.close(); continue }
                scope.launch {
                    runCatching { conn.serve { bytes -> processIncoming(t, conn.remoteId, bytes) } }.onFailure { conn.close() }
                }
            } catch (_: Exception) { if (!t.endpoint.isClosed()) delay(500) }
        }
    }

    private suspend fun processIncoming(t: IrohTransport, remoteId: String, bytes: ByteArray): ByteArray {
        val message = decodeMessage(bytes)
        if (message.type == "pair") {
            val secret = pendingSecret
            if (repo.state.value.peer != null || secret == null || pendingExpiry <= System.currentTimeMillis() || !constantTimeEquals(secret, message.pairingSecret ?: "")) {
                return encodeMessage(SyncMessage(type = "error", sender = repo.state.value.name, error = "This invite is no longer valid. Ask for a new one."))
            }
            val remoteAddr: computer.iroh.EndpointAddr? = runCatching { message.endpointAddr?.let { EndpointTicket.fromString(it).endpointAddr() } }.getOrNull()
            repo.mutate { s -> s.copy(peer = PeerLink(remoteId, message.sender, System.currentTimeMillis(), remoteAddr?.relayUrl(), remoteAddr?.directAddresses() ?: emptyList()), friend = message.snapshot?.let { FriendSnapshot(it.name, it.expenses, it.checkIns, it.dailyLimitPaise, it.weeklyLimitPaise, it.revision, System.currentTimeMillis()) }) }
            pendingSecret = null; pendingExpiry = 0
            status0.value = status0.value.copy(invite = null, phase = LinkPhase.Online)
            return encodeMessage(SyncMessage(type = "pair_ok", sender = repo.state.value.name, snapshot = snapshotOf(repo.state.value), endpointAddr = t.ticket))
        }
        val (merged, notices) = applyIncoming(repo.state.value, message)
        repo.mutate { merged }
        notices.forEach { notice ->
            when (notice.kind) {
                "nudge" -> notice.nudge?.let { BuddyNotifications.showNudge(app, it) }
                "budget_suggestion" -> BuddyNotifications.notifyActivity(app, "Budget suggestion from ${notice.name}", "Open Buddy to review their new limits.")
                "suggestion_reply" -> BuddyNotifications.notifyActivity(app, "${notice.name} ${if (notice.event?.accepted == true) "accepted your new limits" else "kept their limits"}", "Open Buddy to see the update.")
                "unlink" -> BuddyNotifications.notifyActivity(app, "${notice.name} unlinked", "Your peer link has been removed.")
                "friend_checkin" -> if (repo.state.value.friendAlerts) BuddyNotifications.notifyActivity(app, "${notice.name} finished today", "Your friend completed a check-in.")
            }
        }
        return encodeMessage(buildRequest(repo.state.value))
    }

    private suspend fun syncOnce(timeoutMs: Long = 20_000L) = exchangeLock.withLock {
        val link = repo.state.value.peer ?: return@withLock
        val t = runCatching { ensureEndpoint() }.getOrElse { setError(friendly(it)); return@withLock }
        status0.value = status0.value.copy(syncing = true)
        try {
            val address = EndpointTicket.fromAddr(computer.iroh.EndpointAddr(computer.iroh.EndpointId.fromString(link.endpointId), link.relayUrl, link.directAddresses)).toString()
            val conn = withTimeout(timeoutMs) { t.connect(address) }
            val reply = decodeMessage(conn.exchange(encodeMessage(buildRequest(repo.state.value)), timeoutMs))
            val (merged, notices) = applyIncoming(repo.state.value, reply)
            repo.mutate { merged }
            notices.forEach { n -> if (n.kind == "nudge") n.nudge?.let { BuddyNotifications.showNudge(app, it) } }
            val now = System.currentTimeMillis()
            status0.value = status0.value.copy(phase = LinkPhase.Online, syncing = false, lastExchangeAt = now, friendReachable = true, viaRelay = conn.viaRelay, lastError = null)
            conn.close()
        } catch (e: Exception) {
            setError(friendly(e)); status0.value = status0.value.copy(syncing = false, friendReachable = false)
        }
    }

    private fun setError(message: String) { status0.value = status0.value.copy(phase = if (message.contains("network", true)) LinkPhase.NoNetwork else LinkPhase.Error, lastError = message) }

    private suspend fun releaseEndpointIfIdle() {
        if (foreground || service || worker || pendingSecret != null) return
        endpointLock.withLock { transport?.let { runCatching { it.shutdown() } }; transport = null; acceptJob?.cancel(); acceptJob = null; status0.value = status0.value.copy(phase = LinkPhase.Off, endpointIdShort = null) }
    }

    private fun friendly(e: Throwable): String {
        val text = e.message.orEmpty()
        return when {
            text.contains("expired", true) -> text
            text.contains("timeout", true) || text.contains("network", true) || text.contains("connect", true) -> "Couldn't reach your friend's phone. Keep the invite screen open on their phone and try again."
            else -> text.ifBlank { "Couldn't complete the peer sync. Keep both phones nearby and try again." }
        }
    }

    companion object {
        const val KEY_IROH_KEY = "iroh-secret-key"
        @Volatile private var instance: PeerSyncManager? = null
        fun get(context: Context): PeerSyncManager = instance ?: synchronized(this) { instance ?: PeerSyncManager(context.applicationContext).also { instance = it } }
    }
}
