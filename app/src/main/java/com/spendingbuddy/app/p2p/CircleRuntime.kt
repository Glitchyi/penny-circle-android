package com.spendingbuddy.app.p2p

import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.Member

/*
 * Pure helpers for the circle runtime (no Android or iroh types) so they can be unit tested on the JVM.
 */

/** The exact error text a phone replies with when it is not in the circle the sender thinks it is in. */
const val NOT_IN_CIRCLE_TEXT = "This phone is not in a circle."
const val NOT_A_MEMBER_TEXT = "You are not a member of this circle."

/**
 * A member who answers "not in a circle" is treated as having left, but only once they have had time to finish joining:
 * an approved joiner picks up its `join_ok` by polling or by the admin's push, and polling stops when the request lapses
 * ([PENDING_JOIN_TTL_MS]). Until then a "not in a circle" reply just means "not finished joining yet".
 */
const val JOIN_GRACE_MS = PENDING_JOIN_TTL_MS

fun isNotInCircleReply(m: SyncMessage): Boolean = m.type == "error" && m.error?.contains("not in a circle", ignoreCase = true) == true

fun shouldTreatAsLeft(member: Member, now: Long): Boolean = member.active && (member.joinedAt <= 0L || now - member.joinedAt >= JOIN_GRACE_MS)

/** Delay before join poll number [attempt] (0-based): quick at first, then every 30 s, then every minute. */
fun joinPollDelayMs(attempt: Int): Long = when {
    attempt < 6 -> 5_000L
    attempt < 30 -> 30_000L
    else -> 60_000L
}

/** What this phone is doing to join a circle. */
sealed interface JoinProgress {
    data object None : JoinProgress
    data object Requesting : JoinProgress
    /** The admin has the request; it lapses at [expiresAt] (epoch ms). Approval arrives while the app is open, by push, or on the next approximate background pass. */
    data class Waiting(val expiresAt: Long) : JoinProgress
    data object Joined : JoinProgress
    data class Refused(val reason: String) : JoinProgress
}

/** Reachability of one other member as of the last attempt or incoming exchange. */
data class MemberLink(
    val reachable: Boolean = false,
    val lastExchangeAt: Long = 0,
    val viaRelay: Boolean? = null,
    val error: String? = null
)

enum class LinkPhase { Off, Starting, Online, NoNetwork, Error }

data class PeerStatus(
    val phase: LinkPhase = LinkPhase.Off,
    val endpointIdShort: String? = null,
    /** Keyed by member endpoint id. Absent means "not tried yet". */
    val members: Map<String, MemberLink> = emptyMap(),
    /** Most recent successful exchange with any member (epoch ms), 0 if none. */
    val lastExchangeAt: Long = 0,
    val syncing: Boolean = false,
    val lastError: String? = null,
    val serviceRunning: Boolean = false,
    val joinProgress: JoinProgress = JoinProgress.None,
    /** Admin only: when the code last issued by [PeerSyncManager.issueJoinCode] stops working (epoch ms), 0 if none. */
    val joinCodeExpiresAt: Long = 0
) {
    /** Convenience view of [members]: endpoint id -> reachable. */
    val reachable: Map<String, Boolean> get() = members.mapValues { it.value.reachable }
    val anyReachable: Boolean get() = members.values.any { it.reachable }
}

/** Active members other than this phone whose last shared check-ins show today unfinished, whose data is fresh enough to say so, and who can be nudged today. */
fun nudgeCandidates(state: BuddyState, today: String, now: Long, maxAgeMs: Long = 18 * 60 * 60_000L): List<Member> {
    if (state.group == null) return emptyList()
    return activeMembers(state).filter { m ->
        if (m.endpointId == state.selfId) return@filter false
        val snap = state.friends[m.endpointId] ?: return@filter false
        now - snap.receivedAt <= maxAgeMs && snap.checkIns.none { it.date == today } && canNudge(state, m.endpointId, now)
    }
}

fun friendlyError(e: Throwable): String {
    val text = e.message.orEmpty()
    return when {
        e is kotlinx.coroutines.TimeoutCancellationException || text.contains("timeout", true) || text.contains("timed out", true) ||
            text.contains("network", true) || text.contains("connect", true) || text.contains("unreachable", true) ->
            "Couldn't reach that phone. Delivery is approximate and needs their app to be open or online; it will retry."
        else -> text.ifBlank { "Couldn't complete the sync. It will retry." }
    }
}
