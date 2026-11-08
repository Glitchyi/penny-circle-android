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

const val SYNC_ALPN_TEXT = "spendingbuddy/sync/1"
val SYNC_ALPN: ByteArray get() = SYNC_ALPN_TEXT.toByteArray(StandardCharsets.UTF_8)
const val MAX_MESSAGE_BYTES = 1024 * 1024
private const val INVITE_PREFIX = "sb1."
private const val INVITE_TTL_MS = 15 * 60_000L

private val wireJson = Json { encodeDefaults = true; ignoreUnknownKeys = true; explicitNulls = false }

@Serializable
data class WireSnapshot(
    val name: String = "",
    val dailyLimitPaise: Long = 0,
    val weeklyLimitPaise: Long = 0,
    val expenses: List<Expense> = emptyList(),
    val checkIns: List<CheckIn> = emptyList(),
    val revision: Long = 0
)

@Serializable
data class SyncMessage(
    val v: Int = 1,
    val type: String,
    val sender: String = "",
    val snapshot: WireSnapshot? = null,
    val events: List<PeerEvent> = emptyList(),
    val acks: List<String> = emptyList(),
    val pairingSecret: String? = null,
    val endpointAddr: String? = null,
    val error: String? = null
)

@Serializable
data class InvitePayload(
    val ticket: String,
    val secret: String,
    val name: String,
    val expiresAt: Long
)

data class InviteParseResult(val payload: InvitePayload, val expired: Boolean)
data class MergeNotice(val kind: String, val name: String = "", val event: PeerEvent? = null, val nudge: Nudge? = null)

data class InviteCode(val code: String, val payload: InvitePayload)

fun encodeMessage(message: SyncMessage): ByteArray = wireJson.encodeToString(SyncMessage.serializer(), message).toByteArray(StandardCharsets.UTF_8)

fun decodeMessage(bytes: ByteArray, maxBytes: Int = MAX_MESSAGE_BYTES): SyncMessage {
    require(bytes.size <= maxBytes) { "Message is too large" }
    return wireJson.decodeFromString(SyncMessage.serializer(), bytes.toString(StandardCharsets.UTF_8))
}

fun snapshotOf(state: BuddyState, now: Long = System.currentTimeMillis()): WireSnapshot {
    val cutoff = now - 120L * 24 * 60 * 60_000L
    val since = LocalDate.now(INDIA_ZONE).minusDays(120)
    return WireSnapshot(
        name = state.name, dailyLimitPaise = state.dailyLimitPaise, weeklyLimitPaise = state.weeklyLimitPaise,
        expenses = state.expenses.filter { runCatching { !LocalDate.parse(it.date).isBefore(since) }.getOrDefault(it.updatedAt >= cutoff) },
        checkIns = state.checkIns.filter { runCatching { !LocalDate.parse(it.date).isBefore(since) }.getOrDefault(true) },
        revision = state.revision
    )
}

fun buildRequest(state: BuddyState, now: Long = System.currentTimeMillis()): SyncMessage = SyncMessage(
    type = "sync", sender = state.name, snapshot = snapshotOf(state, now),
    events = state.outbox, acks = state.seenEventIds
)

fun applyIncoming(state: BuddyState, message: SyncMessage, now: Long = System.currentTimeMillis()): Pair<BuddyState, List<MergeNotice>> {
    var next = state
    val notices = mutableListOf<MergeNotice>()
    val incoming = message.snapshot
    if (incoming != null && incoming.revision >= (state.friend?.revision ?: -1L)) {
        val wasBaseline = state.friend == null
        next = next.copy(friend = FriendSnapshot(incoming.name, incoming.expenses, incoming.checkIns, incoming.dailyLimitPaise, incoming.weeklyLimitPaise, incoming.revision, now))
        if (!wasBaseline && state.friendAlerts) {
            val newChecks = incoming.checkIns.filter { fresh -> state.friend?.checkIns?.none { it.date == fresh.date && it.completedAt == fresh.completedAt } == true }
            newChecks.forEach { check -> notices += MergeNotice("friend_checkin", incoming.name, PeerEvent(type = "checkin", createdAt = check.completedAt)) }
        }
    }
    if (message.acks.isNotEmpty()) {
        val acked = message.acks.toSet()
        next = next.copy(
            outbox = next.outbox.filterNot { it.id in acked },
            outgoingNudge = next.outgoingNudge?.let { if (it.id in acked && it.deliveredAt == 0L) it.copy(deliveredAt = now) else it },
            sentSuggestion = next.sentSuggestion?.let { if (it.id in acked) it.copy(delivered = true) else it }
        )
    }
    val known = next.seenEventIds.toMutableList()
    for (event in message.events) {
        if (event.id in known) continue
        known += event.id
        when (event.type) {
            "nudge" -> {
                val nudge = Nudge(event.id, message.sender, event.minutes, event.createdAt, now, now + event.minutes * 60_000L)
                next = next.copy(incomingNudge = nudge)
                if (now - event.createdAt <= 12 * 60 * 60_000L) notices += MergeNotice("nudge", message.sender, event, nudge)
            }
            "budget_suggestion" -> {
                val suggestion = BudgetSuggestion(event.id, event.dailyPaise, event.weeklyPaise, message.sender, event.createdAt, "pending", false)
                next = next.copy(suggestions = (next.suggestions.filterNot { it.id == suggestion.id } + suggestion).takeLast(100))
                notices += MergeNotice("budget_suggestion", message.sender, event)
            }
            "suggestion_reply" -> {
                next = next.copy(sentSuggestion = next.sentSuggestion?.let { if (it.id == event.suggestionId) it.copy(status = if (event.accepted) "accepted" else "declined") else it })
                notices += MergeNotice("suggestion_reply", message.sender, event)
            }
            "unlink" -> {
                notices += MergeNotice("unlink", message.sender, event)
                next = next.copy(peer = null, friend = null, outbox = emptyList(), seenEventIds = emptyList(), incomingNudge = null, outgoingNudge = null, suggestions = emptyList(), sentSuggestion = null)
            }
        }
    }
    next = next.copy(seenEventIds = known.takeLast(500))
    return next to notices
}

fun newEvent(type: String, now: Long = System.currentTimeMillis(), minutes: Int = 0, dailyPaise: Long = 0, weeklyPaise: Long = 0, suggestionId: String = "", accepted: Boolean = false) =
    PeerEvent(UUID.randomUUID().toString(), type, now, minutes, dailyPaise, weeklyPaise, suggestionId, accepted)

fun constantTimeEquals(a: String, b: String): Boolean = MessageDigest.isEqual(a.toByteArray(StandardCharsets.UTF_8), b.toByteArray(StandardCharsets.UTF_8))
fun randomPairingSecret(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

fun encodeInvite(ticket: String, secret: String, name: String, expiresAt: Long): String {
    val payload = wireJson.encodeToString(InvitePayload.serializer(), InvitePayload(ticket, secret, name, expiresAt))
    return INVITE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray(StandardCharsets.UTF_8))
}

fun parseInvite(input: String, now: Long = System.currentTimeMillis()): InviteParseResult {
    val code = Regex("sb1\\.[A-Za-z0-9_-]+").find(input.trim())?.value ?: throw IllegalArgumentException("That invite code is not valid. Ask your friend to share it again.")
    val encoded = code.removePrefix(INVITE_PREFIX)
    val payload = runCatching { wireJson.decodeFromString(InvitePayload.serializer(), Base64.getUrlDecoder().decode(encoded).toString(StandardCharsets.UTF_8)) }
        .getOrElse { throw IllegalArgumentException("That invite code is not valid. Ask your friend to create a new one.") }
    require(payload.ticket.isNotBlank() && payload.secret.isNotBlank() && payload.expiresAt > 0) { "That invite code is incomplete. Ask your friend to create a new one." }
    return InviteParseResult(payload, payload.expiresAt <= now)
}
