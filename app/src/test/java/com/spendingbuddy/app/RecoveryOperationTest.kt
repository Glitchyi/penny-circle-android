package com.spendingbuddy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RecoveryOperationTest {
    @Test fun reservationBlocksFreshStartWhileAProviderIsStillOpening() {
        val gate = RecoveryOperationGate()
        val restore = gate.begin(RecoveryKind.Restore)!!
        assertEquals(restore, gate.operation.value)
        assertNull(gate.begin(RecoveryKind.FreshStart))
        assertNull(gate.begin(RecoveryKind.Restore))
        gate.finish(restore)
        assertNotNull(gate.begin(RecoveryKind.FreshStart))
    }

    @Test fun cancellationOrFailureCleanupCannotReleaseALaterRecovery() {
        val gate = RecoveryOperationGate()
        val old = gate.begin(RecoveryKind.Restore)!!
        gate.finish(old)
        val replacement = gate.begin(RecoveryKind.FreshStart)!!
        // An activity completion callback may arrive after the repository's finally already ran.
        gate.finish(old)
        assertEquals(replacement, gate.operation.value)
        assertNull(gate.begin(RecoveryKind.Restore))
        gate.requireCurrent(replacement)
    }
}
