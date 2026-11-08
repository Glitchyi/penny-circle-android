package com.spendingbuddy.app.p2p

import com.spendingbuddy.app.*
import org.junit.Assert.*
import org.junit.Test

class SyncProtocolTest {
    @Test fun messageRoundTripAndSizeLimit() {
        val message = SyncMessage(type = "sync", sender = "Asha", snapshot = WireSnapshot(name = "Asha", revision = 4), events = listOf(PeerEvent(type = "nudge", minutes = 30)), acks = listOf("x"))
        assertEquals(message, decodeMessage(encodeMessage(message)))
        assertThrows(IllegalArgumentException::class.java) { decodeMessage(ByteArray(MAX_MESSAGE_BYTES + 1)) }
    }

    @Test fun inviteToleratesWhitespaceAndShareTextAndDetectsTamperingAndExpiry() {
        val code = encodeInvite("ticket", "secret", "Ravi", 2_000)
        assertEquals("Ravi", parseInvite("  Share this with Ravi: $code  ", 1_000).payload.name)
        assertTrue(parseInvite(code, 2_001).expired)
        assertThrows(IllegalArgumentException::class.java) { parseInvite(code.dropLast(1) + "x", 1_000) }
        assertFalse(constantTimeEquals("secret", "secrex"))
        assertTrue(constantTimeEquals("secret", "secret"))
    }

    @Test fun incomingSnapshotUsesRevisionOrderingAndEventsAreDeduped() {
        val old = BuddyState(name = "A", friend = FriendSnapshot(name = "Ravi", revision = 5, receivedAt = 1))
        val stale = SyncMessage(type = "sync", sender = "Ravi", snapshot = WireSnapshot(name = "Ravi", revision = 4))
        assertEquals(5L, applyIncoming(old, stale, 10).first.friend?.revision)
        val event = PeerEvent(id = "n1", type = "nudge", minutes = 30, createdAt = 100)
        val once = applyIncoming(old, SyncMessage(type = "sync", sender = "Ravi", events = listOf(event)), 500).first
        val twice = applyIncoming(once, SyncMessage(type = "sync", sender = "Ravi", events = listOf(event)), 600).first
        assertEquals(1, twice.seenEventIds.count { it == "n1" })
        assertEquals(500L + 30 * 60_000L, once.incomingNudge?.endsAt)
    }

    @Test fun acksClearOutboxAndMarkDelivery() {
        val nudge = PeerEvent(id = "n", type = "nudge", minutes = 30)
        val state = BuddyState(outbox = listOf(nudge), outgoingNudge = OutgoingNudge("n", 30, 1))
        val next = applyIncoming(state, SyncMessage(type = "sync", acks = listOf("n")), 99).first
        assertTrue(next.outbox.isEmpty()); assertEquals(99L, next.outgoingNudge?.deliveredAt)
    }

    @Test fun suggestionAcceptDeclineAndUnlink() {
        val suggestion = PeerEvent(id = "s", type = "budget_suggestion", dailyPaise = 100, weeklyPaise = 700)
        val received = applyIncoming(BuddyState(), SyncMessage(type = "sync", sender = "Ravi", events = listOf(suggestion)), 20).first
        assertEquals("pending", received.suggestions.single().status)
        val reply = PeerEvent(type = "suggestion_reply", suggestionId = "s", accepted = true)
        val sent = BuddyState(peer = PeerLink("id"), sentSuggestion = BudgetSuggestion("s", 100, 700))
        assertEquals("accepted", applyIncoming(sent, SyncMessage(type = "sync", sender = "Ravi", events = listOf(reply)), 30).first.sentSuggestion?.status)
        val linked = received.copy(peer = PeerLink("id"), outbox = listOf(PeerEvent(type = "nudge", minutes = 30)))
        val unlinked = applyIncoming(linked, SyncMessage(type = "sync", sender = "Ravi", events = listOf(PeerEvent(type = "unlink"))), 40).first
        assertNull(unlinked.peer); assertNull(unlinked.friend); assertTrue(unlinked.outbox.isEmpty())
    }
}
