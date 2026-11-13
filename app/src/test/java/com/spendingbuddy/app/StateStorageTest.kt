package com.spendingbuddy.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StateStorageTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun onlyACompletelyAbsentAtomicFileIsANewInstall() {
        assertTrue(stateStorageIsMissing(File(temporary.root, "buddy-state.json")))
    }

    @Test fun anExistingBaseBackupOrUncommittedNewFileMustNotBecomeAnEmptyState() {
        for (suffix in listOf("", ".bak", ".new")) {
            val folder = temporary.newFolder()
            val base = File(folder, "buddy-state.json")
            File(base.path + suffix).writeBytes(byteArrayOf(0xff.toByte(), 0x00))
            assertFalse("Existing state artifact $suffix must remain protected", stateStorageIsMissing(base))
        }
    }
}
