package com.spendingbuddy.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import com.spendingbuddy.app.p2p.*
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
        assertNull("no peer, so no group", s.group)
        assertTrue(s.friends.isEmpty())
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
        val s = BuddyState(name = "Asha", onboardingDone = true, selfId = "me", group = Group("g", "Crew", "sec", 1),
            members = listOf(Member("me", "Asha"), Member("abc", "Ravi", joinedAt = 1)),
            outbox = listOf(PeerEvent(type = "nudge", minutes = 30, target = "abc")), seenEventIds = mapOf("abc" to listOf("e1")), revision = 7)
        assertEquals(s, StateCodec.decode(StateCodec.encode(s)))
    }

    @Test fun timerPreferencesDefaultForOlderStateAndRoundTripLocally() {
        val older = StateCodec.decode("""{"schemaVersion":3,"name":"A"}""")
        assertEquals(DEFAULT_PAYMENT_UPDATE_MINUTES, older.paymentUpdateMinutes)
        assertEquals(DEFAULT_FINISH_COUNTDOWN_LEAD_MINUTES, older.finishCountdownLeadMinutes)
        val chosen = BuddyState(paymentUpdateMinutes = 5, finishCountdownLeadMinutes = 60)
        assertEquals(chosen, StateCodec.decode(StateCodec.encode(chosen)))
    }

    @Test fun persistedUnsupportedTimerPreferencesFallBackButRestoreRejectsThem() {
        val invalid = BuddyState(name = "A", paymentUpdateMinutes = 3, finishCountdownLeadMinutes = 45)
        val loaded = StateCodec.load(StateCodec.encode(invalid)) as StateCodec.LoadResult.Loaded
        assertEquals(DEFAULT_PAYMENT_UPDATE_MINUTES, loaded.state.paymentUpdateMinutes)
        assertEquals(DEFAULT_FINISH_COUNTDOWN_LEAD_MINUTES, loaded.state.finishCountdownLeadMinutes)
        assertEquals(BackupRestoreIssue.UnsupportedTimers, StateCodec.validateRestore(BuddyState(), invalid))
    }

    @Test fun backupExcludesPeerSecretsAndDrafts() {
        val s = BuddyState(name = "Asha", selfId = "me", group = Group("g", "Crew", "sec"), members = listOf(Member("abc")), importImages = listOf(ImportImage(path = "/x")))
        val restored = StateCodec.decode(StateCodec.encodeBackup(s))
        assertNull(restored.group)
        assertTrue(restored.members.isEmpty())
        assertEquals("", restored.selfId)
        assertTrue(restored.importImages.isEmpty())
        assertEquals("Asha", restored.name)
    }

    @Test fun newerSchemaIsUnreadableNotEmpty() {
        val r = StateCodec.load("""{"schemaVersion":${STATE_SCHEMA_VERSION + 1},"name":"X"}""")
        assertTrue(r is StateCodec.LoadResult.Unreadable)
    }

    @Test fun backupDecodeDistinguishesANewerSchemaFromMalformedJson() {
        val newerVersion = STATE_SCHEMA_VERSION + 1
        try {
            StateCodec.decode("""{"schemaVersion":$newerVersion,"name":"X"}""")
            fail("A newer backup must be rejected before restore")
        } catch (newer: UnsupportedSchemaException) {
            assertEquals(newerVersion, newer.version)
        }
        try {
            StateCodec.decode("{not json")
            fail("Malformed data must be rejected before restore")
        } catch (malformed: Exception) {
            assertFalse(malformed is UnsupportedSchemaException)
        }
    }

    @Test fun corruptTextIsUnreadableAndNullIsMissing() {
        assertTrue(StateCodec.load("{not json") is StateCodec.LoadResult.Unreadable)
        assertTrue(StateCodec.load(null) is StateCodec.LoadResult.Missing)
        assertTrue(StateCodec.load(StateCodec.encode(BuddyState(name = "A"))) is StateCodec.LoadResult.Loaded)
    }

    @Test fun invalidSchemaVersionIsRejected() {
        assertTrue(StateCodec.load("""{"schemaVersion":0}""") is StateCodec.LoadResult.Unreadable)
    }

    @Test fun restoreRejectsEmptyBackupOverExistingData() {
        val current = BuddyState(name = "Asha", onboardingDone = true)
        assertEquals(BackupRestoreIssue.EmptyBackup, StateCodec.validateRestore(current, BuddyState()))
    }

    @Test fun restoreAllowsEmptyOverEmptyAndValidOverExisting() {
        assertNull(StateCodec.validateRestore(BuddyState(), BuddyState()))
        val restored = BuddyState(name = "B", onboardingDone = true)
        assertNull(StateCodec.validateRestore(BuddyState(name = "Asha", onboardingDone = true), restored))
    }

    @Test fun restoreRejectsWrongSchemaAndNegativeLimits() {
        assertEquals(BackupRestoreIssue.UnsupportedVersion, StateCodec.validateRestore(BuddyState(), BuddyState(name = "B", schemaVersion = 1)))
        assertEquals(BackupRestoreIssue.InvalidLimits, StateCodec.validateRestore(BuddyState(), BuddyState(name = "B", dailyLimitPaise = -1)))
    }

    @Test fun restoreGivesDistinctReasonsForInvalidPurchasesAndAnEmptyPurchaseHistory() {
        assertEquals(BackupRestoreIssue.InvalidPurchases, StateCodec.validateRestore(BuddyState(), BuddyState(expenses = listOf(Expense(amountPaise = 0)))))
        val current = BuddyState(name = "Asha", expenses = listOf(Expense(amountPaise = 100)))
        assertEquals(BackupRestoreIssue.NoPurchases, StateCodec.validateRestore(current, BuddyState(name = "Asha")))
        assertEquals(100L, current.expenses.single().amountPaise)
    }

    private val v2 = """
        {"schemaVersion":2,"name":"Asha","onboardingDone":true,"revision":4,"dailyLimitPaise":40000,
         "expenses":[{"id":"e1","date":"2026-11-07","amountPaise":25000,"merchant":"Chai","updatedAt":5}],
         "checkIns":[{"date":"2026-11-07","completedAt":9}],
         "peer":{"endpointId":"peer1","name":"Ravi","pairedAt":123,"relayUrl":"https://r.example","directAddresses":["1.2.3.4:5"]},
         "friend":{"name":"Ravi","dailyLimitPaise":1000,"weeklyLimitPaise":7000,"revision":3,"receivedAt":50},
         "outbox":[{"id":"o1","type":"nudge","minutes":30},{"id":"o2","type":"unlink"}],
         "seenEventIds":["s1","s2"],
         "incomingNudge":{"id":"n1","sender":"Ravi","minutes":20,"endsAt":9},
         "outgoingNudge":{"id":"o1","minutes":30,"sentAt":1},
         "suggestions":[{"id":"sg1","dailyPaise":100,"weeklyPaise":700,"sender":"Ravi"}],
         "sentSuggestion":{"id":"sg2","dailyPaise":5,"weeklyPaise":35}}
    """.trimIndent()

    @Test fun migratesV2PeerToTwoMemberGroup() {
        val s = StateCodec.decode(v2)
        assertEquals(3, s.schemaVersion)
        assertEquals(1, s.expenses.size); assertEquals(1, s.checkIns.size)
        assertEquals(40000L, s.dailyLimitPaise)
        assertEquals(StateCodec.MIGRATED_GROUP_NAME, s.group?.name)
        assertEquals("existing behaviour preserved", Visibility.FULL_DETAILS, s.visibility)
        assertEquals("id/admin are derived by ensureSelf", "", s.group?.id)
        val m = s.members.single()
        assertEquals("peer1", m.endpointId); assertEquals("Ravi", m.displayName)
        assertEquals("https://r.example", m.relayUrl); assertEquals(123L, m.joinedAt); assertTrue(m.active)
        assertEquals(3L, s.friends["peer1"]?.revision)
        assertEquals(listOf("s1", "s2"), s.seenEventIds["peer1"])
        assertTrue(s.outbox.all { it.target == "peer1" })
        assertEquals("leave", s.outbox.first { it.id == "o2" }.type)
        assertEquals("peer1", s.incomingNudge?.senderId)
        assertEquals("peer1", s.outgoingNudges.single().targetId)
        assertEquals("peer1", s.suggestions.single().senderId)
        assertEquals("peer1", s.sentSuggestions.single().targetId)
    }

    @Test fun bothMigratedPhonesDeriveTheSameGroupAndAdmin() {
        val mine = ensureSelf(StateCodec.decode(v2), "me")
        val theirs = ensureSelf(StateCodec.decode(v2.replace("peer1", "me")).copy(), "peer1")
        assertEquals(mine.group?.id, theirs.group?.id); assertEquals(mine.group?.secret, theirs.group?.secret)
        assertTrue(mine.group!!.id.startsWith("mig-")); assertEquals("me", mine.group?.adminId); assertEquals("me", theirs.group?.adminId)
        assertTrue(isAdmin(mine)); assertFalse(isAdmin(theirs))
        assertArrayEquals(gossipTopicId(mine.group!!), gossipTopicId(theirs.group!!))
    }

    @Test fun ensureSelfCompletesMigration() {
        val s = com.spendingbuddy.app.p2p.ensureSelf(StateCodec.decode(v2), "me")
        assertEquals(2, s.members.size)
        assertEquals("me", s.members.first().endpointId)
        assertEquals("me", s.suggestions.single().targetId)
        assertNotNull(com.spendingbuddy.app.p2p.respondToSuggestion(s, "sg1", true, 10))
    }

    @Test fun v2WithoutPeerHasNoGroupAndMigratedStateRoundTrips() {
        val s = StateCodec.decode("""{"schemaVersion":2,"name":"A","onboardingDone":true}""")
        assertNull(s.group)
        val m = StateCodec.decode(v2)
        assertEquals(m, StateCodec.decode(StateCodec.encode(m)))
    }

    @Test fun restoreRejectsOversizedMemberList() {
        val many = List(MAX_MEMBERS + 101) { Member("m$it") }
        assertEquals(BackupRestoreIssue.TooManyMembers, StateCodec.validateRestore(BuddyState(), BuddyState(name = "B", members = many)))
    }
}
