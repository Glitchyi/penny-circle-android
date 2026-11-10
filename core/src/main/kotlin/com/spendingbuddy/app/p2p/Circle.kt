package com.spendingbuddy.app.p2p

import com.spendingbuddy.app.*
import kotlinx.serialization.Serializable
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

const val JOIN_CODE_PREFIX = "pc1."
const val JOIN_CODE_TTL_MS = 24 * 60 * 60_000L
/** A code claiming to expire further out than this is rejected as malformed. */
const val MAX_JOIN_CODE_LIFETIME_MS = 7 * 24 * 60 * 60_000L
const val MAX_PENDING_JOINS = 10
const val PENDING_JOIN_TTL_MS = 24 * 60 * 60_000L
const val MAX_DECLINED_JOINS = 50
const val MAX_TOMBSTONES = 100
const val MAX_GROUP_NAME = 40
const val MAX_JOIN_CODE_LENGTH = 8_192
const val MAX_ADDRESSES = 8
private val ID_REGEX = Regex("[A-Za-z0-9_-]{1,$MAX_ID_LENGTH}")

fun randomSecret(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }.let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

fun isValidId(s: String): Boolean = ID_REGEX.matches(s)

/** Trimmed, 1..[MAX_DISPLAY_NAME] chars, no control characters. */
fun isValidDisplayName(s: String): Boolean = s == s.trim() && s.length in 1..MAX_DISPLAY_NAME && s.none { Character.isISOControl(it) }
fun sanitizeDisplayName(raw: String): String = raw.filterNot { Character.isISOControl(it) }.trim().take(MAX_DISPLAY_NAME).trim().ifEmpty { "Friend" }
fun isValidGroupName(s: String): Boolean = s == s.trim() && s.length in 1..MAX_GROUP_NAME && s.none { Character.isISOControl(it) }

fun isValidMember(m: Member): Boolean =
    isValidId(m.endpointId) && isValidDisplayName(m.displayName) &&
        m.status in setOf(MEMBER_ACTIVE, MEMBER_LEFT) &&
        (m.relayUrl?.length ?: 0) <= MAX_NAME_LENGTH * 2 &&
        m.directAddresses.size <= MAX_ADDRESSES && m.directAddresses.all { it.length <= MAX_NAME_LENGTH } &&
        m.joinedAt >= 0 && m.leftAt >= 0

fun activeMembers(state: BuddyState): List<Member> = state.members.filter { it.active }
fun isActiveMember(state: BuddyState, id: String): Boolean = state.group != null && state.members.any { it.endpointId == id && it.active }

// ---- Join code and admin approval -----------------------------------------------------------------------------

/**
 * What a `pc1.` share code carries: group id and name, the admin's endpoint id and dialable ticket, an expiry and a
 * per-code [nonce]. The code grants NOTHING by itself: it only lets a phone send a `join_request` to the admin, who
 * must approve it. Until approval the requester receives no circle data and sends none.
 *
 * Threat model: anyone holding the code can ask to join, so the worst a leaked code does is put a request in the
 * admin's queue (capped at [MAX_PENDING_JOINS], one per endpoint, never burning the code), which the admin can
 * decline. [rotateJoinCode] replaces the nonce so outstanding unused codes stop working; requests already pending
 * stay pending. The checksum catches typos and truncation only; it is not a MAC. Admitted members are authenticated
 * by their iroh endpoint key. Members are trusted: any member can read what others share under their visibility.
 */
@Serializable
data class JoinCodePayload(
    val groupId: String,
    val groupName: String,
    val adminId: String,
    val ticket: String,
    val nonce: String,
    val expiresAt: Long
)

data class JoinCodeParseResult(val payload: JoinCodePayload, val expired: Boolean)

private fun checksum(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes).copyOf(4)

fun encodeJoinCode(payload: JoinCodePayload): String {
    val body = wireJson.encodeToString(JoinCodePayload.serializer(), payload).toByteArray(StandardCharsets.UTF_8)
    return JOIN_CODE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(body + checksum(body))
}

/** Strict parse of share text containing a code. Throws IllegalArgumentException with a user-presentable message. */
fun parseJoinCode(input: String, now: Long = System.currentTimeMillis()): JoinCodeParseResult {
    val bad = "That circle code is not valid. Ask for a new one."
    require(input.length <= MAX_JOIN_CODE_LENGTH) { bad }
    val code = Regex("pc1\\.[A-Za-z0-9_-]+").find(input.trim())?.value ?: throw IllegalArgumentException(bad)
    val raw = runCatching { Base64.getUrlDecoder().decode(code.removePrefix(JOIN_CODE_PREFIX)) }.getOrNull()
    require(raw != null && raw.size > 4) { bad }
    val body = raw.copyOfRange(0, raw.size - 4)
    require(MessageDigest.isEqual(raw.copyOfRange(raw.size - 4, raw.size), checksum(body))) { bad }
    val p = runCatching { wireJson.decodeFromString(JoinCodePayload.serializer(), body.toString(StandardCharsets.UTF_8)) }.getOrElse { throw IllegalArgumentException(bad) }
    require(isValidId(p.groupId) && isValidGroupName(p.groupName) && isValidId(p.adminId)) { bad }
    require(p.ticket.isNotBlank() && p.ticket.length <= MAX_TICKET_LENGTH && p.nonce.length in 16..MAX_ID_LENGTH * 2) { bad }
    require(p.expiresAt > 0 && p.expiresAt <= now + MAX_JOIN_CODE_LIFETIME_MS) { bad }
    return JoinCodeParseResult(p, p.expiresAt <= now)
}

/** The admin: the recorded admin if still active, else the oldest active member by joinedAt (tie: smallest endpoint id). Null outside a group. */
fun currentAdminId(state: BuddyState): String? {
    val g = state.group ?: return null
    if (g.adminId.isNotEmpty() && state.members.any { it.endpointId == g.adminId && it.active }) return g.adminId
    return successorCandidates(state, excluding = null).firstOrNull()?.endpointId
}

fun isAdmin(state: BuddyState): Boolean = state.selfId.isNotEmpty() && currentAdminId(state) == state.selfId

private fun successorCandidates(state: BuddyState, excluding: String?): List<Member> =
    state.members.filter { it.active && it.endpointId != excluding }.sortedWith(compareBy<Member> { if (it.joinedAt == 0L) Long.MAX_VALUE else it.joinedAt }.thenBy { it.endpointId })

/** Admin only: starts a new code nonce valid for [JOIN_CODE_TTL_MS]. Outstanding unused codes stop working. */
fun rotateJoinCode(state: BuddyState, now: Long = System.currentTimeMillis()): BuddyState {
    require(isAdmin(state)) { "Only the circle admin can make a code." }
    return state.copy(joinNonce = randomSecret(), joinNonceExpiresAt = now + JOIN_CODE_TTL_MS)
}

/** Admin only: the share code for the current nonce, rotating first if there is none or it has expired. */
fun issueJoinCode(state: BuddyState, ticket: String, now: Long = System.currentTimeMillis()): Pair<BuddyState, String> {
    require(isAdmin(state)) { "Only the circle admin can make a code." }
    val s = if (state.joinNonce.isEmpty() || state.joinNonceExpiresAt <= now) rotateJoinCode(state, now) else state
    val g = s.group!!
    return s to encodeJoinCode(JoinCodePayload(g.id, g.name, s.selfId, ticket, s.joinNonce, s.joinNonceExpiresAt))
}

/** The `join_request` a would-be member sends to the code's admin. Carries only a name and endpoint info. */
fun buildJoinRequest(state: BuddyState, payload: JoinCodePayload, relayUrl: String? = null, directAddresses: List<String> = emptyList(), now: Long = System.currentTimeMillis()): SyncMessage {
    require(state.group == null) { "Already in a circle. Leave it first." }
    require(state.selfId.isNotEmpty()) { "Endpoint id not set; call ensureSelf first" }
    val me = Member(state.selfId, sanitizeDisplayName(state.name), relayUrl, directAddresses.take(MAX_ADDRESSES), now)
    return SyncMessage(type = "join_request", sender = me.displayName, groupId = payload.groupId, joinNonce = payload.nonce, members = listOf(me))
}

fun purgeExpiredJoins(state: BuddyState, now: Long): BuddyState =
    if (state.pendingJoins.any { it.expiresAt <= now }) state.copy(pendingJoins = state.pendingJoins.filter { it.expiresAt > now }) else state

/** [request] is non-null only when a new request was queued (so the app can notify the admin). */
data class JoinRequestOutcome(val state: BuddyState, val reply: SyncMessage, val request: JoinRequest?)

/**
 * Admin side of a `join_request` received from [fromId] (the authenticated remote id). Replies:
 * `join_pending` (queued or already queued), `join_ok` (the sender was already approved: polling picks it up),
 * or `error` (not admin, wrong group, expired/rotated code, declined, left before, pending queue full).
 * A request is queued only with the live nonce; a full queue refuses new requests but does not burn the code.
 */
fun handleJoinRequest(state: BuddyState, fromId: String, message: SyncMessage, now: Long = System.currentTimeMillis()): JoinRequestOutcome {
    fun refuse(why: String) = JoinRequestOutcome(state, SyncMessage(type = "error", error = why), null)
    val group = state.group ?: return refuse("This phone is not in a circle.")
    if (message.type != "join_request" || message.groupId != group.id) return refuse("That code is for a different circle.")
    if (!isAdmin(state)) return refuse("Only the circle admin can take join requests.")
    if (fromId == state.selfId) return refuse("You are already in this circle.")
    val existing = state.members.firstOrNull { it.endpointId == fromId }
    if (existing != null) return if (existing.active) JoinRequestOutcome(state, joinOkFor(state, now), null) else refuse("This phone left the circle and cannot rejoin with the same identity.")
    val s = purgeExpiredJoins(state, now)
    s.pendingJoins.firstOrNull { it.endpointId == fromId }?.let { return JoinRequestOutcome(s, SyncMessage(type = "join_pending", groupId = group.id, expiresAt = it.expiresAt), null) }
    if (fromId in s.declinedJoinIds) return JoinRequestOutcome(s, SyncMessage(type = "error", error = "The admin declined this request."), null)
    val nonceOk = s.joinNonce.isNotEmpty() && s.joinNonceExpiresAt > now && message.joinNonce != null && constantTimeEquals(s.joinNonce, message.joinNonce)
    if (!nonceOk) return JoinRequestOutcome(s, SyncMessage(type = "error", error = "That circle code has expired or been replaced. Ask for a new one."), null)
    val joiner = message.members.singleOrNull()?.takeIf { it.endpointId == fromId && it.active && isValidMember(it) }
        ?: return JoinRequestOutcome(s, SyncMessage(type = "error", error = "The join request was not valid."), null)
    if (s.pendingJoins.size >= MAX_PENDING_JOINS) return JoinRequestOutcome(s, SyncMessage(type = "error", error = "Too many requests are waiting. Try again later."), null)
    val req = JoinRequest(UUID.randomUUID().toString(), fromId, joiner.displayName, joiner.relayUrl, joiner.directAddresses, now, now + PENDING_JOIN_TTL_MS)
    return JoinRequestOutcome(s.copy(pendingJoins = s.pendingJoins + req), SyncMessage(type = "join_pending", groupId = group.id, expiresAt = req.expiresAt), req)
}

data class ApproveResult(val state: BuddyState, val member: Member, val joinOk: SyncMessage)

/**
 * Admin only (throws otherwise). Adds the requester as an active member and returns the `join_ok` to send them
 * (push it, or let them pick it up when they re-send their request). Other members learn through membership merge.
 */
fun approveJoin(state: BuddyState, requestId: String, now: Long = System.currentTimeMillis()): ApproveResult {
    require(isAdmin(state)) { "Only the circle admin can approve join requests." }
    val req = purgeExpiredJoins(state, now).pendingJoins.firstOrNull { it.id == requestId } ?: throw IllegalArgumentException("That request is no longer pending.")
    require(activeMembers(state).size < MAX_MEMBERS) { "This circle is full ($MAX_MEMBERS members)." }
    require(state.members.none { it.endpointId == req.endpointId }) { "Already a member." }
    val m = Member(req.endpointId, req.displayName, req.relayUrl, req.directAddresses, now)
    val next = state.copy(members = state.members + m, pendingJoins = state.pendingJoins.filterNot { it.id == requestId })
    return ApproveResult(next, m, joinOkFor(next, now))
}

/** Admin only (throws otherwise). The requester's next poll gets an error and the same endpoint cannot re-request. */
fun declineJoin(state: BuddyState, requestId: String): BuddyState {
    require(isAdmin(state)) { "Only the circle admin can decline join requests." }
    val req = state.pendingJoins.firstOrNull { it.id == requestId } ?: return state
    return state.copy(pendingJoins = state.pendingJoins.filterNot { it.id == requestId }, declinedJoinIds = (state.declinedJoinIds + req.endpointId).takeLast(MAX_DECLINED_JOINS))
}

/** The `join_ok` carrying the group (with admin), the full member list and the admin's snapshot. */
fun joinOkFor(state: BuddyState, now: Long = System.currentTimeMillis()): SyncMessage {
    val g = requireNotNull(state.group)
    return SyncMessage(type = "join_ok", sender = sanitizeDisplayName(state.name), groupId = g.id, group = g, members = wireMembers(state),
        snapshot = snapshotOf(state, now), adminId = g.adminId, adminEpoch = g.adminEpoch)
}

sealed class JoinReply {
    data class Pending(val expiresAt: Long) : JoinReply()
    data class Joined(val state: BuddyState) : JoinReply()
    data class Refused(val reason: String) : JoinReply()
}

/**
 * Joiner side: interprets the admin's reply to a `join_request`. [fromId] must be the authenticated remote id and
 * equal [expected].adminId. `join_pending` means keep waiting (re-send the request to poll); `join_ok` returns the new state.
 */
fun applyJoinReply(state: BuddyState, fromId: String, message: SyncMessage, expected: JoinCodePayload, now: Long = System.currentTimeMillis()): JoinReply {
    if (state.group != null) return JoinReply.Refused("Already in a circle.")
    if (state.selfId.isEmpty()) return JoinReply.Refused("Endpoint id not set")
    if (fromId != expected.adminId) return JoinReply.Refused("Reply came from an unexpected phone.")
    return when (message.type) {
        "join_pending" -> JoinReply.Pending(message.expiresAt)
        "error" -> JoinReply.Refused(message.error ?: "The circle did not accept the request.")
        "join_ok" -> runCatching {
            validateMessage(message)
            val g = requireNotNull(message.group) { "Missing circle details." }
            require(g.id == expected.groupId && message.groupId == g.id) { "Reply is for a different circle." }
            val merged = mergeMembers(emptyList(), message.members, fromId, state.selfId)
            require(merged.any { it.endpointId == fromId && it.active }) { "The admin is not an active member." }
            var s = state.copy(group = g, members = merged, friends = emptyMap(), outbox = emptyList(), seenEventIds = emptyMap(),
                incomingNudge = null, outgoingNudges = emptyList(), suggestions = emptyList(), sentSuggestions = emptyList(), lastSeqSeen = emptyMap())
            s = ensureSelf(s, state.selfId, now = now)
            message.snapshot?.let { s = s.copy(friends = mapOf(fromId to friendOf(it, now))) }
            JoinReply.Joined(s)
        }.getOrElse { JoinReply.Refused(it.message ?: "Invalid reply.") }
        else -> JoinReply.Refused("Unexpected reply.")
    }
}

// ---- Membership merge ----------------------------------------------------------------------------------------

/**
 * Pure membership merge: union by endpointId.
 * - Invalid entries (bad id/name/addresses) are skipped.
 * - Tombstones win: once left, a member stays left (no resurrection under the same endpoint id). A tombstone is only
 *   accepted from the member it is about ([fromId]): members can leave only themselves, nobody can remove another.
 * - Active entries beyond [cap] active members are dropped; tombstones are kept up to [MAX_TOMBSTONES].
 * - Entries about [selfId] are ignored (nobody else can remove or rename this phone).
 * - Name and address updates for an existing member are only trusted when [fromId] is that member.
* A joinedAt is only ever lowered to the smallest positive value seen, so phones converge on the same ordering
 * (used for admin fallback).
 */
fun mergeMembers(local: List<Member>, incoming: List<Member>, fromId: String, selfId: String, cap: Int = MAX_MEMBERS): List<Member> {
    val map = LinkedHashMap<String, Member>()
    local.forEach { map[it.endpointId] = it }
    for (m in incoming) {
        if (!isValidMember(m) || m.endpointId == selfId) continue
        val cur = map[m.endpointId]
        if (cur == null) {
            if (m.status == MEMBER_LEFT) { if (m.endpointId == fromId && map.values.count { !it.active } < MAX_TOMBSTONES) map[m.endpointId] = m }
            else if (map.values.count { it.active } < cap) map[m.endpointId] = m
            continue
        }
        var next = cur
        if (m.status == MEMBER_LEFT && cur.active && m.endpointId == fromId) next = cur.copy(status = MEMBER_LEFT, leftAt = m.leftAt)
        if (next.active && m.active && m.endpointId == fromId)
            next = next.copy(displayName = m.displayName, relayUrl = m.relayUrl ?: next.relayUrl, directAddresses = m.directAddresses.ifEmpty { next.directAddresses })
        if (m.joinedAt > 0 && (next.joinedAt == 0L || m.joinedAt < next.joinedAt)) next = next.copy(joinedAt = m.joinedAt)
        map[m.endpointId] = next
    }
    return map.values.toList()
}

// ---- Group lifecycle -----------------------------------------------------------------------------------------

/**
 * Records this phone's iroh id and display name as the self member. Safe to call on every start. Also completes a
 * schema 2 -> 3 migration: a migrated group has no id/secret/admin yet, so both phones derive the same ones from the
 * two endpoint ids (id and secret by hashing the sorted pair; admin = the smaller endpoint id), and migrated
 * suggestion targets are filled in.
 */
fun ensureSelf(state: BuddyState, endpointId: String, relayUrl: String? = null, directAddresses: List<String> = emptyList(), now: Long = System.currentTimeMillis()): BuddyState {
    require(isValidId(endpointId)) { "Invalid endpoint id" }
    var s = state.copy(selfId = endpointId)
    var g = s.group
    if (g != null) {
        val me = s.members.firstOrNull { it.endpointId == endpointId }
        val updated = (me ?: Member(endpointId, joinedAt = now)).copy(
            displayName = sanitizeDisplayName(s.name), relayUrl = relayUrl ?: me?.relayUrl,
            directAddresses = directAddresses.take(MAX_ADDRESSES).ifEmpty { me?.directAddresses.orEmpty() })
        s = s.copy(members = if (me == null) listOf(updated) + s.members else s.members.map { if (it.endpointId == endpointId) updated else it },
            suggestions = s.suggestions.map { if (it.targetId.isEmpty()) it.copy(targetId = endpointId) else it })
        if (g.id.isEmpty()) {
            val pair = s.members.filter { it.active }.map { it.endpointId }.sorted().joinToString("|")
            fun h(label: String) = MessageDigest.getInstance("SHA-256").digest("pennycircle/migrate/$label|$pair".toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
            g = g.copy(id = "mig-" + h("id").take(32), secret = h("secret").take(32), adminId = s.members.map { it.endpointId }.min())
            s = s.copy(group = g)
        }
    }
    return s
}

/** Creates a new group with this phone as its first member and its admin. Fails if already in a group or the name is invalid. */
fun createGroup(state: BuddyState, groupName: String, now: Long = System.currentTimeMillis()): BuddyState {
    require(state.group == null) { "Already in a circle. Leave it first." }
    require(state.selfId.isNotEmpty()) { "Endpoint id not set; call ensureSelf first" }
    val name = groupName.trim()
    require(isValidGroupName(name)) { "Circle names are 1 to $MAX_GROUP_NAME characters." }
    val group = Group(UUID.randomUUID().toString(), name, randomSecret(), now, adminId = state.selfId, adminEpoch = 0)
    return ensureSelf(state.copy(group = group, members = emptyList(), friends = emptyMap(), outbox = emptyList(), seenEventIds = emptyMap(),
        pendingJoins = emptyList(), declinedJoinIds = emptyList(), joinNonce = "", joinNonceExpiresAt = 0, lastSeqSeen = emptyMap()), state.selfId, now = now)
}

/**
 * Admin only. Hands the admin role to another active member with a higher epoch; it reaches members in the next
 * sync (`adminId`/`adminEpoch` ride on every sync). The old admin keeps no extra powers afterwards.
 */
fun transferAdmin(state: BuddyState, newAdminId: String): BuddyState {
    val g = requireNotNull(state.group) { "Not in a circle" }
    require(isAdmin(state)) { "Only the circle admin can transfer the role." }
    require(newAdminId != state.selfId && isActiveMember(state, newAdminId)) { "Pick another member of your circle." }
    return state.copy(group = g.copy(adminId = newAdminId, adminEpoch = g.adminEpoch + 1), pendingJoins = emptyList(), joinNonce = "", joinNonceExpiresAt = 0)
}

data class LeaveNotice(val member: Member, val message: SyncMessage)
/** [notices]: one direct message per remaining member; [broadcast]: the same leave as one group-wide message. Both are signed when a signer was given. */
data class LeaveResult(val state: BuddyState, val notices: List<LeaveNotice>, val broadcast: SyncMessage? = null)

/**
 * Leaves the circle (members can only leave themselves). Personal data is kept; all group state is cleared. When the
 * leaver is the admin the role passes to [successorId] if it is an active member, otherwise to the oldest remaining
 * member by joinedAt (tie: smallest endpoint id), carried in the leave message with a higher epoch. Messages are
 * best-effort and not queued: a member that misses it learns later from the tombstone the leaver itself no longer
 * sends, so apps should treat a failed dial to a member who replies "not in a circle" as left.
 */
fun leaveGroup(state: BuddyState, successorId: String? = null, signer: MessageSigner? = null, now: Long = System.currentTimeMillis()): LeaveResult {
    val g = state.group ?: return LeaveResult(state, emptyList())
    val me = Member(state.selfId, sanitizeDisplayName(state.name), status = MEMBER_LEFT, leftAt = now)
    val others = activeMembers(state).filter { it.endpointId != state.selfId }
    var adminId = ""; var epoch = 0L
    if (isAdmin(state) && others.isNotEmpty()) {
        adminId = successorId?.takeIf { id -> others.any { it.endpointId == id } } ?: successorCandidates(state, excluding = state.selfId).first().endpointId
        epoch = g.adminEpoch + 1
    }
    var seq = state.outSeq
    fun finish(m: SyncMessage) = if (signer != null) signMessage(m, state.selfId, ++seq, signer) else m
    val notices = others.map { m ->
        LeaveNotice(m, finish(SyncMessage(type = "leave", sender = me.displayName, groupId = g.id, members = listOf(me),
            events = listOf(newEvent("leave", now, target = m.endpointId)), adminId = adminId, adminEpoch = epoch)))
    }
    val broadcast = if (others.isEmpty()) null else finish(SyncMessage(type = "leave", sender = me.displayName, groupId = g.id, members = listOf(me),
        events = listOf(newEvent("leave", now, target = "")), adminId = adminId, adminEpoch = epoch))
    val cleared = state.copy(group = null, members = emptyList(), friends = emptyMap(), outbox = emptyList(), seenEventIds = emptyMap(),
        incomingNudge = null, outgoingNudges = emptyList(), suggestions = emptyList(), sentSuggestions = emptyList(),
        pendingJoins = emptyList(), declinedJoinIds = emptyList(), joinNonce = "", joinNonceExpiresAt = 0, lastNudgeFrom = emptyMap(), lastSeqSeen = emptyMap(), outSeq = seq)
    return LeaveResult(cleared, notices, broadcast)
}

/** Changes what is shared from now on and bumps [BuddyState.revision] so the next sync replaces what peers hold with the new snapshot. Already-delivered copies cannot be recalled. */
fun setVisibility(state: BuddyState, visibility: Visibility): BuddyState =
    if (state.visibility == visibility) state else state.copy(visibility = visibility, revision = state.revision + 1)

// ---- Outgoing actions ----------------------------------------------------------------------------------------

/** True if [targetId] is another active member and no nudge to them has been sent today (Asia/Kolkata). */
fun canNudge(state: BuddyState, targetId: String, now: Long = System.currentTimeMillis()): Boolean =
    targetId != state.selfId && isActiveMember(state, targetId) && state.outgoingNudges.none { it.targetId == targetId && istDate(it.sentAt) == istDate(now) }

/** The incoming nudge worth showing: unexpired and not superseded by a check-in today (Asia/Kolkata). */
fun activeNudge(state: BuddyState, now: Long = System.currentTimeMillis()): Nudge? =
    state.incomingNudge?.takeIf { n -> n.endsAt > now && state.checkIns.none { it.date == istDate(now) || it.date == istDate(n.deliveredAt) } }

/** Queues a nudge for [targetId]. Throws if the target is not another active member, or you already nudged them today. */
fun queueNudge(state: BuddyState, targetId: String, minutes: Int, now: Long = System.currentTimeMillis()): BuddyState {
    require(targetId != state.selfId && isActiveMember(state, targetId)) { "Pick a member of your circle." }
    require(canNudge(state, targetId, now)) { "You already nudged them today." }
    require(minutes in MIN_NUDGE_MINUTES..MAX_NUDGE_MINUTES) { "Nudge length is out of range." }
    val e = newEvent("nudge", now, minutes = minutes, target = targetId)
    return state.copy(outbox = state.outbox + e, outgoingNudges = (state.outgoingNudges + OutgoingNudge(e.id, minutes, now, 0, targetId)).takeLast(50))
}

/** Queues a limits suggestion for [targetId] (the owner of those limits). */
fun queueSuggestion(state: BuddyState, targetId: String, dailyPaise: Long, weeklyPaise: Long, now: Long = System.currentTimeMillis()): BuddyState {
    require(targetId != state.selfId && isActiveMember(state, targetId)) { "Pick a member of your circle." }
    require(dailyPaise in 0..MAX_PAISE && weeklyPaise in 0..MAX_PAISE) { "Amounts are out of range." }
    val e = newEvent("budget_suggestion", now, dailyPaise = dailyPaise, weeklyPaise = weeklyPaise, target = targetId)
    val sug = BudgetSuggestion(e.id, dailyPaise, weeklyPaise, sanitizeDisplayName(state.name), now, "pending", false, state.selfId, targetId)
    return state.copy(outbox = state.outbox + e, sentSuggestions = (state.sentSuggestions + sug).takeLast(50))
}

/**
 * Accepts or declines a suggestion addressed to this phone. Returns null when [suggestionId] is unknown, not
 * pending, or not addressed to [BuddyState.selfId] (so nobody but the target can change their limits). Accepting
 * applies the limits and bumps [BuddyState.revision]; either way a reply is queued for the suggester.
 */
fun respondToSuggestion(state: BuddyState, suggestionId: String, accepted: Boolean, now: Long = System.currentTimeMillis()): BuddyState? {
    val sug = state.suggestions.firstOrNull { it.id == suggestionId } ?: return null
    if (sug.status != "pending" || state.selfId.isEmpty() || sug.targetId != state.selfId || !isActiveMember(state, sug.senderId)) return null
    val reply = newEvent("suggestion_reply", now, suggestionId = suggestionId, accepted = accepted, target = sug.senderId)
    val updated = state.suggestions.map { if (it.id == suggestionId) it.copy(status = if (accepted) "accepted" else "declined") else it }
    val base = state.copy(suggestions = updated, outbox = state.outbox + reply)
    return if (accepted) base.copy(dailyLimitPaise = sug.dailyPaise, weeklyLimitPaise = sug.weeklyPaise, revision = state.revision + 1) else base
}

// ---- Fan-out -------------------------------------------------------------------------------------------------

/** Full member list for the wire (self entry carries the current display name), tombstones included, bounded. */
fun wireMembers(state: BuddyState): List<Member> {
    val all = state.members.map { if (it.endpointId == state.selfId) it.copy(displayName = sanitizeDisplayName(state.name)) else it }
    return all.filter { it.active }.take(MAX_MEMBERS) + all.filter { !it.active }.takeLast(MAX_TOMBSTONES)
}

data class FanOutItem(val member: Member, val message: SyncMessage)

/** The members to sync with (all active members except this phone) and the `sync` message for each. */
fun fanOut(state: BuddyState, now: Long = System.currentTimeMillis()): List<FanOutItem> {
    val g = state.group ?: return emptyList()
    val snap = snapshotOf(state, now)
    val members = wireMembers(state)
    return activeMembers(state).filter { it.endpointId != state.selfId }.map { m ->
        FanOutItem(m, SyncMessage(
            type = "sync", sender = sanitizeDisplayName(state.name), groupId = g.id, snapshot = snap, members = members, adminId = g.adminId, adminEpoch = g.adminEpoch,
            events = state.outbox.filter { it.target == m.endpointId }.take(MAX_EVENTS_PER_MESSAGE),
            acks = state.seenEventIds[m.endpointId].orEmpty().takeLast(MAX_ACKS_PER_MESSAGE)))
    }
}
