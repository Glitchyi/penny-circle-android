package com.spendingbuddy.app.p2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JoinAttemptGateTest {
    @Test fun cancelledReplyCannotApplyLocalMembership() {
        val gate = JoinAttemptGate()
        val attempt = gate.begin()
        var membership = "waiting"
        gate.cancel { membership = "none" }
        assertNull(gate.ifCurrent(attempt) { membership = "joined" })
        assertEquals("none", membership)
    }

    @Test fun replacementRequestRejectsPreviousReplyButAcceptsCurrentReply() {
        val gate = JoinAttemptGate()
        val old = gate.begin()
        val replacement = gate.begin()
        assertNull(gate.ifCurrent(old) { "old circle" })
        assertEquals("new circle", gate.ifCurrent(replacement) { "new circle" })
    }

    @Test fun cancelAfterWaitingReplyBeforePollingStartsCannotRestartPolling() {
        val gate = JoinAttemptGate()
        val attempt = gate.begin()
        var status = "requesting"
        var pollingStarts = 0
        gate.ifCurrent(attempt) { status = "waiting" }
        // The reply has returned, but the caller has not yet scheduled its global polling job.
        gate.cancel { status = "none" }
        gate.ifCurrent(attempt) { pollingStarts++; status = "polling" }
        assertEquals(0, pollingStarts)
        assertEquals("none", status)
    }

    @Test fun cancelBeforeExpiryWriteCannotReplaceTheNewRequestStatusOrClearItsCode() {
        val gate = JoinAttemptGate()
        val oldAttempt = gate.begin()
        var code = "old code"
        var status = "waiting"
        // The old polling job observed expiry, then yielded before applying its refusal.
        gate.cancel { code = ""; status = "none" }
        val replacement = gate.begin()
        gate.ifCurrent(replacement) { code = "new code"; status = "requesting" }
        gate.ifCurrent(oldAttempt) { code = ""; status = "expired" }
        assertEquals("new code", code)
        assertEquals("requesting", status)
    }

    @Test fun replacementClearsThePreviousBootstrapCodeBeforeTheNewGenerationCanReadIt() {
        val gate = JoinAttemptGate()
        gate.begin()
        var pendingCode: String? = "previous code"
        val replacement = gate.begin { pendingCode = null }
        // A pushed reply arriving now sees no payload until the replacement request stores its code.
        assertNull(gate.ifCurrent(replacement) { pendingCode })
        gate.ifCurrent(replacement) { pendingCode = "replacement code" }
        assertEquals("replacement code", gate.ifCurrent(replacement) { pendingCode })
    }
}
