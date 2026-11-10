package com.spendingbuddy.app

import com.spendingbuddy.app.p2p.*
import org.junit.Assert.*
import org.junit.Test

class CircleRuntimeTest {
    @Test fun notInCircleReplyIsDetected() {
        assertTrue(isNotInCircleReply(SyncMessage(type = "error", error = NOT_IN_CIRCLE_TEXT)))
        assertFalse(isNotInCircleReply(SyncMessage(type = "error", error = NOT_A_MEMBER_TEXT)))
        assertFalse(isNotInCircleReply(SyncMessage(type = "sync")))
    }

    @Test fun leftOnlyAfterJoinGrace() {
        val now = 10 * JOIN_GRACE_MS
        assertFalse(shouldTreatAsLeft(Member("a", "A", joinedAt = now - 1000), now))
        assertTrue(shouldTreatAsLeft(Member("a", "A", joinedAt = now - JOIN_GRACE_MS), now))
        assertFalse(shouldTreatAsLeft(Member("a", "A", joinedAt = 1, status = MEMBER_LEFT), now))
    }

    @Test fun pollDelayBacksOff() {
        assertEquals(5_000L, joinPollDelayMs(0))
        assertEquals(30_000L, joinPollDelayMs(10))
        assertEquals(60_000L, joinPollDelayMs(100))
    }

    @Test fun nudgeCandidatesAreFreshUnfinishedAndNudgeable() {
        val now = 1_000_000_000_000L
        val today = istDate(now)
        val s = BuddyState(selfId = "me", group = Group("g", "G", "s"),
            members = listOf(Member("me", "Me"), Member("a", "A"), Member("b", "B"), Member("c", "C")),
            friends = mapOf(
                "a" to FriendSnapshot(receivedAt = now - 1000),
                "b" to FriendSnapshot(receivedAt = now - 1000, checkIns = listOf(CheckIn(today, now))),
                "c" to FriendSnapshot(receivedAt = now - 48 * 3_600_000L)))
        assertEquals(listOf("a"), nudgeCandidates(s, today, now).map { it.endpointId })
        assertTrue(nudgeCandidates(s.copy(group = null), today, now).isEmpty())
    }
}
