package com.spendingbuddy.app.p2p

import com.spendingbuddy.app.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDate
import java.util.Base64
import java.util.UUID

/** Bumped with the group protocol: phones on the old two-person protocol cannot connect, rather than mis-parse. */
const val SYNC_ALPN_TEXT = "pennycircle/sync/2"
val SYNC_ALPN: ByteArray get() = SYNC_ALPN_TEXT.toByteArray(StandardCharsets.UTF_8)
const val MAX_MESSAGE_BYTES = 1024 * 1024

const val PROTOCOL_VERSION = 2
const val MIN_SUPPORTED_VERSION = 2
const val MAX_EVENTS_PER_MESSAGE = 200
const val MAX_ACKS_PER_MESSAGE = 250
const val MAX_SEEN_EVENT_IDS = 1000
const val MAX_EXPENSES_PER_MESSAGE = 2_000
const val MAX_CHECKINS_PER_MESSAGE = 500
const val MAX_ID_LENGTH = 64
const val MAX_NAME_LENGTH = 100
const val MAX_TEXT_LENGTH = 500
const val MAX_TICKET_LENGTH = 4_096
const val MAX_PAISE = 1_000_000_000_000L
const val MIN_NUDGE_MINUTES = 1
const val MAX_NUDGE_MINUTES = 1_440
/**
 * Direct QUIC to the admin: join_request / join_pending / join_ok / error. Broadcast-capable (direct or gossip),
 * self-authenticating via senderId+seq+sig: sync (snapshot+events+acks+members), leave, nudge, suggest, respond.
 */
private val KNOWN_MESSAGE_TYPES = setOf("sync", "join_request", "join_pending", "join_ok", "leave", "nudge", "suggest", "respond", "error")
/** Types that may arrive over a shared channel (gossip) and therefore must verify via [applyBroadcast]. */
val BROADCAST_TYPES = setOf("sync", "leave", "nudge", "suggest", "respond")
const val MAX_SIGNATURE_LENGTH = 256
private val EVENT_TYPES = setOf("nudge", "budget_suggestion", "suggestion_reply", "leave")

internal val wireJson = Json { encodeDefaults = true; ignoreUnknownKeys = true; explicitNulls = false }

@Serializable
data class WireSnapshot(
    val name: String = "",
    val dailyLimitPaise: Long = 0,
    val weeklyLimitPaise: Long = 0,
    val expenses: List<Expense> = emptyList(),
    val checkIns: List<CheckIn> = emptyList(),
    val revision: Long = 0,
    val visibility: Visibility = Visibility.TOTALS_STATUS,
    val dailySpentPaise: Long = 0,
    val weeklySpentPaise: Long = 0,
    val streak: Int = 0
)

@Serializable
data class SyncMessage(
    val v: Int = PROTOCOL_VERSION,
    val type: String,
    /** Sender's display name (untrusted label; identity is the authenticated remote endpoint id). */
    val sender: String = "",
    val groupId: String = "",
    val snapshot: WireSnapshot? = null,
    val events: List<PeerEvent> = emptyList(),
    val acks: List<String> = emptyList(),
    /** Membership list (sync/join_ok) or the sender's own entry (join, leave). */
    val members: List<Member> = emptyList(),
    /** join_ok only: the group being joined. */
    val group: Group? = null,
    /** join_request only: the per-code nonce from the share code (an anti-stale token, not a membership secret). */
    val joinNonce: String? = null,
    /** join_pending: when the request lapses (epoch ms). */
    val expiresAt: Long = 0,
    /** The sender's view of the circle admin; accepted only under the rules in [applyIncoming]. */
    val adminId: String = "",
    val adminEpoch: Long = 0,
    /** Broadcast authentication: sender endpoint id, per-sender increasing sequence, and signature over the message with sig="". */
    val senderId: String = "",
    val seq: Long = 0,
    val sig: String = "",
    val error: String? = null
)

data class MergeNotice(val kind: String, val name: String = "", val event: PeerEvent? = null, val nudge: Nudge? = null, val memberId: String = "")

fun encodeMessage(message: SyncMessage): ByteArray = wireJson.encodeToString(SyncMessage.serializer(), message).toByteArray(StandardCharsets.UTF_8)

fun decodeMessage(bytes: ByteArray, maxBytes: Int = MAX_MESSAGE_BYTES): SyncMessage {
    require(bytes.size <= maxBytes) { "Message is too large" }
    return validateMessage(wireJson.decodeFromString(SyncMessage.serializer(), bytes.toString(StandardCharsets.UTF_8)))
}

private fun check(ok: Boolean, what: String) { require(ok) { "Invalid sync message: $what" } }

/** Throws IllegalArgumentException when [m] is unsupported or exceeds sane bounds. Returns [m] for chaining. */
fun validateMessage(m: SyncMessage): SyncMessage {
    check(m.v in MIN_SUPPORTED_VERSION..PROTOCOL_VERSION, "unsupported protocol version ${m.v}")
    check(m.type in KNOWN_MESSAGE_TYPES, "unknown message type")
    check(m.sender.length <= MAX_NAME_LENGTH, "sender too long")
    check(m.events.size <= MAX_EVENTS_PER_MESSAGE, "too many events")
    check(m.acks.size <= MAX_ACKS_PER_MESSAGE, "too many acks")
    check(m.acks.all { it.length <= MAX_ID_LENGTH }, "ack id too long")
    check(m.groupId.length <= MAX_ID_LENGTH, "group id too long")
    check((m.joinNonce?.length ?: 0) <= MAX_ID_LENGTH * 2, "join nonce too long")
    check(m.adminId.isEmpty() || isValidId(m.adminId), "bad admin id")
    check(m.adminEpoch >= 0 && m.seq >= 0 && m.expiresAt >= 0, "bad counter")
    check(m.senderId.isEmpty() || isValidId(m.senderId), "bad sender id")
    check(m.sig.length <= MAX_SIGNATURE_LENGTH, "signature too long")
    check(m.members.size <= MAX_MEMBERS + MAX_TOMBSTONES, "too many members")
    check(m.members.all { isValidMember(it) }, "bad member entry")
    check(m.members.map { it.endpointId }.toSet().size == m.members.size, "duplicate member")
    m.group?.let { g -> check(isValidId(g.id) && isValidGroupName(g.name) && g.secret.length in 1..MAX_ID_LENGTH * 2, "bad group") }
    if (m.type == "join_request") check(m.joinNonce != null && m.members.size == 1 && m.groupId.isNotEmpty(), "bad join request")
    if (m.type == "join_ok") check(m.group != null && m.members.isNotEmpty(), "bad join reply")
    check((m.error?.length ?: 0) <= MAX_TEXT_LENGTH, "error text too long")
    m.events.forEach { e ->
        check(e.id.length in 1..MAX_ID_LENGTH, "bad event id")
        check(e.type in EVENT_TYPES, "unknown event type")
        check(e.target.length <= MAX_ID_LENGTH, "event target too long")
        check(e.type == "leave" || isValidId(e.target), "event needs a target")
        check(e.suggestionId.length <= MAX_ID_LENGTH, "suggestion id too long")
        check(e.dailyPaise in 0..MAX_PAISE && e.weeklyPaise in 0..MAX_PAISE, "bad event amount")
        check(e.createdAt >= 0, "bad event time")
        if (e.type == "nudge") check(e.minutes in MIN_NUDGE_MINUTES..MAX_NUDGE_MINUTES, "nudge minutes out of range")
        else check(e.minutes in 0..MAX_NUDGE_MINUTES, "minutes out of range")
    }
    m.snapshot?.let { s ->
        check(s.name.length <= MAX_NAME_LENGTH, "name too long")
        check(s.dailyLimitPaise in 0..MAX_PAISE && s.weeklyLimitPaise in 0..MAX_PAISE, "bad limit")
        check(s.revision >= 0, "bad revision")
        check(s.dailySpentPaise in 0..MAX_PAISE && s.weeklySpentPaise in 0..MAX_PAISE && s.streak in 0..100_000, "bad totals")
        check(s.visibility == Visibility.FULL_DETAILS || s.expenses.isEmpty(), "totals-only snapshot carries purchase records")
        check(s.expenses.size <= MAX_EXPENSES_PER_MESSAGE, "too many expenses")
        check(s.checkIns.size <= MAX_CHECKINS_PER_MESSAGE, "too many check-ins")
        s.expenses.forEach { x ->
            check(x.id.length in 1..MAX_ID_LENGTH, "bad expense id")
            check(x.amountPaise in 0..MAX_PAISE, "bad expense amount")
            check(x.date.length <= 10 && x.category.length <= MAX_NAME_LENGTH && x.kind.length <= 32, "expense field too long")
            check(x.merchant.length <= MAX_TEXT_LENGTH && x.reason.length <= MAX_TEXT_LENGTH && x.note.length <= MAX_TEXT_LENGTH, "expense text too long")
        }
        s.checkIns.forEach { c -> check(c.date.length <= 10 && c.completedAt >= 0, "bad check-in") }
    }
    return m
}

/** Every message except `join_request` (approval-gated by the admin) must come from an active member of the group. */
fun isSenderAllowed(type: String, remoteId: String, state: BuddyState): Boolean =
    if (type == "join_request") true else isActiveMember(state, remoteId) && remoteId != state.selfId

fun istDate(ms: Long): String = java.time.Instant.ofEpochMilli(ms).atZone(INDIA_ZONE).toLocalDate().toString()

/**
 * The snapshot this phone shares, built from [visibility]. TOTALS_STATUS carries today's and this week's net totals,
 * limits, check-in status records and the streak, and no purchase records (so no merchants, reasons, categories or
 * notes). FULL_DETAILS adds the reviewed records of the last 120 days. Pure: switching visibility (see
 * [setVisibility]) changes only what is sent afterwards; copies already delivered to other phones cannot be recalled,
 * though the next sync overwrites them with the new, smaller snapshot.
 */
fun snapshotFor(state: BuddyState, visibility: Visibility, now: Long = System.currentTimeMillis()): WireSnapshot {
    val zoned = java.time.Instant.ofEpochMilli(now).atZone(INDIA_ZONE)
    val today = zoned.toLocalDate()
    val since = today.minusDays(120)
    val cutoff = now - 120L * 24 * 60 * 60_000L
    return WireSnapshot(
        name = state.name, dailyLimitPaise = state.dailyLimitPaise, weeklyLimitPaise = state.weeklyLimitPaise,
        expenses = if (visibility == Visibility.FULL_DETAILS)
            state.expenses.filter { runCatching { !LocalDate.parse(it.date).isBefore(since) }.getOrDefault(it.updatedAt >= cutoff) } else emptyList(),
        checkIns = state.checkIns.filter { runCatching { !LocalDate.parse(it.date).isBefore(since) }.getOrDefault(true) },
        revision = state.revision, visibility = visibility,
        dailySpentPaise = spentOn(state.expenses, today).coerceAtLeast(0), weeklySpentPaise = spentInWeek(state.expenses, today).coerceAtLeast(0),
        streak = streak(state.checkIns, zoned)
    )
}

fun snapshotOf(state: BuddyState, now: Long = System.currentTimeMillis()): WireSnapshot = snapshotFor(state, state.visibility, now)

internal fun friendOf(s: WireSnapshot, now: Long) = FriendSnapshot(s.name, s.expenses, s.checkIns, s.dailyLimitPaise, s.weeklyLimitPaise, s.revision, now, s.dailySpentPaise, s.weeklySpentPaise, s.streak, s.visibility)

// ---- Self-authenticating broadcast ---------------------------------------------------------------------------

/**
 * Signs and verifies broadcast payloads with the phone's iroh ed25519 key. The app implements this over the
 * bindings (or a JCA Ed25519 key); core only depends on the interface so it stays pure and testable.
 */
interface MessageSigner {
    fun sign(payload: ByteArray): String
    /** True only if [signature] is a valid signature by the key behind [endpointId]. Must not throw. */
    fun verify(endpointId: String, payload: ByteArray, signature: String): Boolean
}

private fun canonicalBytes(m: SyncMessage): ByteArray = encodeMessage(m.copy(sig = ""))

/** Stamps [m] with [senderId] and [seq] and signs it. */
fun signMessage(m: SyncMessage, senderId: String, seq: Long, signer: MessageSigner): SyncMessage {
    val stamped = m.copy(senderId = senderId, seq = seq, sig = "")
    return stamped.copy(sig = signer.sign(canonicalBytes(stamped)))
}

fun verifyMessage(m: SyncMessage, signer: MessageSigner): Boolean =
    m.senderId.isNotEmpty() && m.sig.isNotEmpty() && m.seq > 0 && runCatching { signer.verify(m.senderId, canonicalBytes(m), m.sig) }.getOrDefault(false)

/**
 * Per-circle gossip topic id: HMAC-SHA256 keyed by the group secret over a domain label and the group id (32 bytes).
 * Deterministic on every member's phone; the topic id alone does not authenticate anyone, so every broadcast is also
 * signed and checked with [applyBroadcast].
 */
fun gossipTopicId(group: Group): ByteArray {
    val mac = javax.crypto.Mac.getInstance("HmacSHA256")
    mac.init(javax.crypto.spec.SecretKeySpec(group.secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
    return mac.doFinal("pennycircle/gossip/v1|${group.id}".toByteArray(StandardCharsets.UTF_8))
}

/** Builds this phone's signed group-wide `sync` broadcast (snapshot, membership, admin view, all queued events, acks) and bumps [BuddyState.outSeq]. */
fun buildBroadcast(state: BuddyState, signer: MessageSigner, now: Long = System.currentTimeMillis()): Pair<BuddyState, SyncMessage?> {
    val g = state.group ?: return state to null
    val seq = state.outSeq + 1
    val msg = SyncMessage(type = "sync", sender = sanitizeDisplayName(state.name), groupId = g.id, snapshot = snapshotOf(state, now), members = wireMembers(state),
        events = state.outbox.take(MAX_EVENTS_PER_MESSAGE), acks = state.seenEventIds.values.flatten().takeLast(MAX_ACKS_PER_MESSAGE), adminId = g.adminId, adminEpoch = g.adminEpoch)
    return state.copy(outSeq = seq) to signMessage(msg, state.selfId, seq, signer)
}

/**
 * Entry point for anything received on a shared channel (gossip). Rejects unsigned/forged messages, non-broadcast
 * types, non-members, and replays (seq must exceed the highest seen from that sender), then applies it as if it had
 * come directly from the verified sender. A leaked topic id therefore does not let outsiders inject anything.
 */
fun applyBroadcast(state: BuddyState, message: SyncMessage, signer: MessageSigner, now: Long = System.currentTimeMillis()): Pair<BuddyState, List<MergeNotice>> {
    validateMessage(message)
    require(message.type in BROADCAST_TYPES) { "Not a broadcast message" }
    require(verifyMessage(message, signer)) { "Bad signature" }
    val from = message.senderId
    require(isSenderAllowed(message.type, from, state)) { "Sender is not a member of this circle" }
    require(message.seq > (state.lastSeqSeen[from] ?: 0L)) { "Replayed or out-of-order message" }
    val (next, notices) = applyIncoming(state, from, message, now)
    return next.copy(lastSeqSeen = next.lastSeqSeen + (from to message.seq)) to notices
}

private fun seenAdd(seen: Map<String, List<String>>, id: String, ids: Collection<String>): Map<String, List<String>> =
    seen + (id to ids.toList().takeLast(MAX_SEEN_EVENT_IDS))

/**
 * Applies a message received from [fromId], the authenticated remote endpoint id of the connection.
 * Throws IllegalArgumentException if the message is invalid, is for another group, or [fromId] is not an active
 * member (so non-members cannot inject data). Join messages are handled by [handleJoinRequest]/[applyJoinReply], not here.
 * Events are de-duplicated per sender; targeted events (nudge, budget_suggestion) are ignored unless addressed to
 * this phone; a suggestion_reply is honoured only from the member the suggestion was sent to.
 */
fun applyIncoming(state: BuddyState, fromId: String, message: SyncMessage, now: Long = System.currentTimeMillis()): Pair<BuddyState, List<MergeNotice>> {
    validateMessage(message)
    val group = requireNotNull(state.group) { "Not in a circle" }
    require(message.type in BROADCAST_TYPES) { "Unexpected message type" }
    require(message.groupId == group.id) { "Message is for a different circle" }
    require(isSenderAllowed(message.type, fromId, state)) { "Sender is not a member of this circle" }
    var next = state
    val notices = mutableListOf<MergeNotice>()
    next = next.copy(members = mergeMembers(next.members, message.members, fromId, state.selfId))
    // Admin changes: higher epoch only, and only from the current admin, or from the claimed successor once the current admin has left.
    if (message.adminEpoch > group.adminEpoch && message.adminId.isNotEmpty() && next.members.any { it.endpointId == message.adminId && it.active }) {
        val currentActive = next.members.any { it.endpointId == group.adminId && it.active }
        if (fromId == group.adminId || (fromId == message.adminId && !currentActive))
            next = next.copy(group = group.copy(adminId = message.adminId, adminEpoch = message.adminEpoch))
    }
    val senderName = next.members.firstOrNull { it.endpointId == fromId }?.displayName ?: sanitizeDisplayName(message.sender)
    val incoming = message.snapshot
    val prior = state.friends[fromId]
    if (incoming != null && incoming.revision >= (prior?.revision ?: -1L)) {
        next = next.copy(friends = next.friends + (fromId to friendOf(incoming, now)))
        if (prior != null && state.friendAlerts) {
            val newChecks = incoming.checkIns.filter { fresh -> prior.checkIns.none { it.date == fresh.date && it.completedAt == fresh.completedAt } }
            newChecks.forEach { c -> notices += MergeNotice("friend_checkin", senderName, PeerEvent(type = "checkin", createdAt = c.completedAt), memberId = fromId) }
        }
    }
    if (message.acks.isNotEmpty()) {
        val acked = message.acks.toSet()
        next = next.copy(
            outbox = next.outbox.filterNot { it.id in acked && it.target == fromId },
            outgoingNudges = next.outgoingNudges.map { if (it.id in acked && it.targetId == fromId && it.deliveredAt == 0L) it.copy(deliveredAt = now) else it },
            sentSuggestions = next.sentSuggestions.map { if (it.id in acked && it.targetId == fromId) it.copy(delivered = true) else it }
        )
    }
    val known = LinkedHashSet(next.seenEventIds[fromId].orEmpty())
    for (event in message.events) {
        if (!known.add(event.id)) continue
        val forMe = event.target == state.selfId
        when (event.type) {
            "nudge" -> if (forMe) {
                // One nudge per sender per Asia/Kolkata day; stale ones, or any once this phone has checked in today, are dropped silently.
                val today = istDate(now)
                val checkedIn = next.checkIns.any { it.date == today }
                if (next.lastNudgeFrom[fromId] != today && !checkedIn && now - event.createdAt <= 12 * 60 * 60_000L) {
                    val nudge = Nudge(event.id, senderName, event.minutes, event.createdAt, now, now + event.minutes * 60_000L, fromId)
                    next = next.copy(incomingNudge = nudge, lastNudgeFrom = next.lastNudgeFrom + (fromId to today))
                    notices += MergeNotice("nudge", senderName, event, nudge, fromId)
                }
            }
            "budget_suggestion" -> if (forMe) {
                val s = BudgetSuggestion(event.id, event.dailyPaise, event.weeklyPaise, senderName, event.createdAt, "pending", false, fromId, state.selfId)
                next = next.copy(suggestions = (next.suggestions.filterNot { it.id == s.id } + s).takeLast(100))
                notices += MergeNotice("budget_suggestion", senderName, event, memberId = fromId)
            }
            "suggestion_reply" -> if (forMe) {
                val match = next.sentSuggestions.firstOrNull { it.id == event.suggestionId && it.targetId == fromId && it.status == "pending" }
                if (match != null) {
                    next = next.copy(sentSuggestions = next.sentSuggestions.map { if (it === match) it.copy(status = if (event.accepted) "accepted" else "declined") else it })
                    notices += MergeNotice("suggestion_reply", senderName, event, memberId = fromId)
                }
            }
            "leave" -> {
                notices += MergeNotice("leave", senderName, event, memberId = fromId)
                next = next.copy(
                    members = next.members.map { if (it.endpointId == fromId && it.active) it.copy(status = MEMBER_LEFT, leftAt = now) else it },
                    friends = next.friends - fromId,
                    outbox = next.outbox.filterNot { it.target == fromId },
                    outgoingNudges = next.outgoingNudges.filterNot { it.targetId == fromId },
                    suggestions = next.suggestions.filterNot { it.senderId == fromId },
                    sentSuggestions = next.sentSuggestions.filterNot { it.targetId == fromId },
                    incomingNudge = next.incomingNudge?.takeUnless { it.senderId == fromId }
                )
            }
        }
    }
    next = next.copy(seenEventIds = seenAdd(next.seenEventIds, fromId, known).filterKeys { id -> next.members.any { it.endpointId == id && it.active } })
    return next to notices
}

fun newEvent(type: String, now: Long = System.currentTimeMillis(), minutes: Int = 0, dailyPaise: Long = 0, weeklyPaise: Long = 0, suggestionId: String = "", accepted: Boolean = false, target: String = "") =
    PeerEvent(UUID.randomUUID().toString(), type, now, minutes, dailyPaise, weeklyPaise, suggestionId, accepted, target)

fun constantTimeEquals(a: String, b: String): Boolean = MessageDigest.isEqual(a.toByteArray(StandardCharsets.UTF_8), b.toByteArray(StandardCharsets.UTF_8))
