package com.spendingbuddy.app.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class LocalOperationTest {
    @Test fun validationIsReturnedToTheActiveFormAndSuccessIsNull() = runBlocking {
        assertEquals("Choose a valid date", localOperationError("Storage failed") { "Choose a valid date" })
        assertNull(localOperationError("Storage failed") { null })
    }

    @Test fun unexpectedStorageFailureUsesRecoveryCopyInsteadOfAFilePath() = runBlocking {
        assertEquals("Couldn't save. Try again.", localOperationError("Couldn't save. Try again.") {
            throw IOException("private/files/buddy-state.json")
        })
    }

    @Test fun cancellationNeverBecomesAFailureOrASuccess() = runBlocking {
        try {
            localOperationError("Couldn't save") { throw CancellationException("Screen disposed") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            // The caller must not clear a draft or report success after cancellation.
        }
    }
}
