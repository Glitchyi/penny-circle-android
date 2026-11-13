package com.spendingbuddy.app.p2p

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CircleErrorsTest {
    @Test fun replyDiagnosticsGiveUpdateRecoveryWithoutLeakingPeerText() {
        val message = userFacingCircleError(IllegalArgumentException("Invalid sync message: bad counter secret-endpoint"))
        assertEquals("The phones could not read this update. Update Penny Circle on both phones, then try again.", message)
    }

    @Test fun identityDiagnosticGivesLocalRecovery() {
        assertEquals("This phone’s circle connection could not be prepared. Reopen Penny Circle and try again.",
            userFacingCircleError(IllegalStateException("Endpoint id not set; call ensureSelf first")))
    }

    @Test fun unknownDiagnosticIsHiddenButActionableDomainReasonSurvives() {
        assertFalse(userFacingCircleError(IllegalStateException("/private/path: secret-key" )).contains("secret"))
        assertEquals("That request is no longer pending.", userFacingCircleError(IllegalArgumentException("That request is no longer pending.")))
    }
}
