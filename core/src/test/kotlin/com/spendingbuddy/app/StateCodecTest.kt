package com.spendingbuddy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StateCodecTest {
    private val v01 = """
        {"name":"Ravi","expenses":[{"id":"e1","date":"2026-11-07","amountPaise":25000,"merchant":"Chai Point","category":"Food","reason":"Social","note":"","kind":"expense","updatedAt":5}],
         "checkIns":[{"date":"2026-11-07","completedAt":9,"grace":false,"onTime":true,"noSpend":false}],
         "dailyLimitPaise":40000,"weeklyLimitPaise":250000,"categories":["Food","Transport","Shopping","Bills","Other","Gym"],
         "reasons":["Essential","Convenience","Social","Impulse"],"haptics":true,"reminders":false,
         "importImages":[],"importCandidates":[],"friend":{"name":"Old","expenses":[],"checkIns":[],"dailyLimitPaise":0,"weeklyLimitPaise":0,"lastSeen":0},
         "incomingNudge":{"id":"n","sender":"x","endsAt":1,"createdAt":1},"suggestions":[],"inviteCode":"","syncStatus":"Friend sync needs setup",
         "lastSync":0,"backendUrl":"","deviceToken":"","lastNudgedDate":""}
    """.trimIndent()

    @Test fun migratesV01WithoutLosingData() {
        val s = StateCodec.decode(v01)
        assertEquals(STATE_SCHEMA_VERSION, s.schemaVersion)
        assertEquals("Ravi", s.name)
        assertEquals(1, s.expenses.size)
        assertEquals(25000L, s.expenses[0].amountPaise)
        assertEquals(40000L, s.dailyLimitPaise)
        assertTrue("Gym" in s.categories)
        assertFalse(s.reminders)
        assertTrue("existing users skip onboarding", s.onboardingDone)
        assertNull("backend-era friend data is dropped", s.friend)
        assertNull(s.incomingNudge)
        assertEquals("keypad", s.amountInput)
        assertEquals("Food", s.lastCategory)
        assertTrue(s.outbox.isEmpty())
    }

    @Test fun freshV01FileStillNeedsOnboarding() {
        val s = StateCodec.decode("""{"name":"","expenses":[],"checkIns":[]}""")
        assertFalse(s.onboardingDone)
        assertEquals(DEFAULT_CATEGORIES, s.categories)
    }

    @Test fun roundTripsCurrentSchema() {
        val s = BuddyState(name = "Asha", onboardingDone = true, peer = PeerLink("abc", "Ravi", 1),
            outbox = listOf(PeerEvent(type = "nudge", minutes = 30)), revision = 7)
        assertEquals(s, StateCodec.decode(StateCodec.encode(s)))
    }

    @Test fun backupExcludesPeerSecretsAndDrafts() {
        val s = BuddyState(name = "Asha", peer = PeerLink("abc"), importImages = listOf(ImportImage(path = "/x")))
        val restored = StateCodec.decode(StateCodec.encodeBackup(s))
        assertNull(restored.peer)
        assertTrue(restored.importImages.isEmpty())
        assertEquals("Asha", restored.name)
    }
}
