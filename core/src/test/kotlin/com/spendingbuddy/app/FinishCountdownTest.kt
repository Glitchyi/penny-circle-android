package com.spendingbuddy.app

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinishCountdownTest {
    private val day = LocalDate.parse("2026-11-13")
    private fun instant(time: String): Instant = java.time.ZonedDateTime.parse(time).toInstant()
    private fun active(time: String, enabled: Boolean = true, completed: Set<LocalDate> = emptySet()) =
        activeFinishCountdownWindow(instant(time), enabled, completed)

    @Test fun startsThirtyMinutesBeforeTenPmReminderAndEndsAtReminder() {
        val window = active("2026-11-13T21:30:00+05:30")!!
        assertEquals(day, window.day)
        assertEquals(30 * 60 * 1000L, window.durationMillis)
        assertEquals(0, finishCountdownProgress(window, window.startsAt))
        assertEquals(500, finishCountdownProgress(window, window.startsAt.plusMillis(window.durationMillis / 2)))
        assertEquals(instant("2026-11-13T22:00:00+05:30"), window.reminderAt)
        assertEquals(1000, finishCountdownProgress(window, window.reminderAt))
        assertEquals(0, finishCountdownProgress(window, window.startsAt.minusMillis(1)))
        assertEquals(1000, finishCountdownProgress(window, window.reminderAt.plusMillis(1)))
        assertNull(active("2026-11-13T21:29:59+05:30"))
        assertEquals(day, active("2026-11-13T21:59:59+05:30")!!.day)
        assertNull(active("2026-11-13T22:00:00+05:30"))
        assertNull(active("2026-11-14T09:59:59+05:30"))
    }

    @Test fun followingDayGetsItsOwnReminderWindowNotTheGracePeriod() {
        val nextDay = LocalDate.parse("2026-11-14")
        assertEquals(nextDay, active("2026-11-14T21:30:00+05:30")!!.day)
        assertNull(active("2026-11-14T00:00:00+05:30"))
        assertNull(active("2026-11-14T09:59:59+05:30"))
    }

    @Test fun eachLeadPresetUsesTheExpectedStartAndWindowBoundary() {
        listOf(15 to "21:45:00", 30 to "21:30:00", 60 to "21:00:00").forEach { (minutes, start) ->
            val window = activeFinishCountdownWindow(
                instant("2026-11-13T${start}+05:30"), true, emptySet(), leadMinutes = minutes
            )!!
            assertEquals(minutes * 60_000L, window.durationMillis)
            assertEquals(instant("2026-11-13T${start}+05:30"), window.startsAt)
            assertNull(activeFinishCountdownWindow(
                instant("2026-11-13T${start}+05:30").minusMillis(1), true, emptySet(), leadMinutes = minutes
            ))
        }
    }

    @Test fun unsupportedLeadIsRejected() {
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            activeFinishCountdownWindow(instant("2026-11-13T21:30:00+05:30"), true, emptySet(), leadMinutes = 20)
        }
    }

    @Test fun disabledAndCompletedDaysHaveNoWindow() {
        assertNull(active("2026-11-13T21:45:00+05:30", enabled = false))
        assertNull(active("2026-11-13T21:45:00+05:30", completed = setOf(day)))
    }

    @Test fun deviceTimeZoneDoesNotChangeIstWindow() {
        val utcWindow = activeFinishCountdownWindow(
            instant("2026-11-13T16:00:00Z"), true, emptySet(), ZoneId.of("Asia/Kolkata")
        )
        assertNotNull(utcWindow)
        assertEquals(day, utcWindow!!.day)
        assertEquals(instant("2026-11-13T16:00:00Z"), utcWindow.startsAt)
        assertEquals(instant("2026-11-13T16:30:00Z"), utcWindow.reminderAt)
    }
}
