package com.spendingbuddy.app

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

fun finishCountdownStartTime(leadMinutes: Int): LocalTime {
    require(leadMinutes in FINISH_COUNTDOWN_LEAD_MINUTES)
    return LocalTime.of(22, 0).minusMinutes(leadMinutes.toLong())
}

/** A user-opted-in countdown until the fixed 10 PM reminder on [day]. */
data class FinishCountdownWindow(
    val day: LocalDate,
    val startsAt: Instant,
    val reminderAt: Instant
) {
    val durationMillis: Long get() = reminderAt.toEpochMilli() - startsAt.toEpochMilli()
}

/**
 * Select the current lead-time window before the 10 PM reminder for [now], if enabled and that day is
 * unfinished. The separate 10 AM next-day grace window for late logging is not a notification deadline.
 * Times are evaluated in [zone], regardless of the device's configured zone.
 */
fun activeFinishCountdownWindow(
    now: Instant,
    enabled: Boolean,
    completedDays: Set<LocalDate>,
    zone: ZoneId = INDIA_ZONE,
    leadMinutes: Int = DEFAULT_FINISH_COUNTDOWN_LEAD_MINUTES
): FinishCountdownWindow? {
    require(leadMinutes in FINISH_COUNTDOWN_LEAD_MINUTES)
    if (!enabled) return null
    val localNow = now.atZone(zone)
    val reminderTime = LocalTime.of(22, 0)
    val startTime = reminderTime.minusMinutes(leadMinutes.toLong())
    if (localNow.toLocalTime() < startTime || localNow.toLocalTime() >= reminderTime) return null
    val day = localNow.toLocalDate()
    if (day in completedDays) return null
    val start = day.atTime(startTime).atZone(zone).toInstant()
    val reminder = day.atTime(reminderTime).atZone(zone).toInstant()
    return FinishCountdownWindow(day, start, reminder)
}

/** Elapsed fraction for the platform progress indicator, clamped to the selected reminder window. */
fun finishCountdownProgress(window: FinishCountdownWindow, now: Instant): Int {
    val elapsed = (now.toEpochMilli() - window.startsAt.toEpochMilli()).coerceIn(0L, window.durationMillis)
    return ((elapsed * 1000L) / window.durationMillis.coerceAtLeast(1L)).toInt()
}
