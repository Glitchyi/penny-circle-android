package com.spendingbuddy.app

import java.time.LocalDate

/** Most weeks the Activity pager will offer (page 0 = current week). */
const val MAX_ACTIVITY_WEEKS = 52

/** Monday-Sunday range containing a week's [start]. */
data class WeekRange(val start: LocalDate, val end: LocalDate)

/** Purchases of one day, newest first, with the net total (refunds subtract, transfers count 0). */
data class DayGroup(val date: LocalDate, val purchases: List<Expense>, val totalPaise: Long)

/** Monday-Sunday range for the week starting at (or containing) [anyDay]. */
fun weekRange(anyDay: LocalDate): WeekRange {
    val start = weekStart(anyDay)
    return WeekRange(start, start.plusDays(6))
}

/** Monday of the week [pagesBack] weeks before the week containing [today]. */
fun weekStartForPage(today: LocalDate, pagesBack: Int): LocalDate = weekStart(today).minusWeeks(pagesBack.toLong())

private fun parseDate(s: String): LocalDate? = runCatching { LocalDate.parse(s) }.getOrNull()

/**
 * Number of pager pages: the current week plus every earlier week back to the earliest purchase, at least 1
 * and at most [max]. Purchases dated in the future do not add pages.
 */
fun weeksBack(expenses: List<Expense>, today: LocalDate, max: Int = MAX_ACTIVITY_WEEKS): Int {
    val earliest = expenses.mapNotNull { parseDate(it.date) }.filter { !it.isAfter(today) }.minOrNull() ?: return 1
    val weeks = java.time.temporal.ChronoUnit.WEEKS.between(weekStart(earliest), weekStart(today)).toInt() + 1
    return weeks.coerceIn(1, max.coerceAtLeast(1))
}

/** Purchases in the week of [anyDay], grouped by day, newest day first; days without purchases are omitted. */
fun groupByDay(expenses: List<Expense>, anyDay: LocalDate): List<DayGroup> {
    val range = weekRange(anyDay)
    return expenses
        .mapNotNull { e -> parseDate(e.date)?.let { it to e } }
        .filter { (d, _) -> !d.isBefore(range.start) && !d.isAfter(range.end) }
        .groupBy({ it.first }, { it.second })
        .map { (d, list) -> DayGroup(d, list.sortedByDescending { it.updatedAt }, netSpending(list)) }
        .sortedByDescending { it.date }
}

/** Net spending for each of the seven days Monday..Sunday of the week of [anyDay]. */
fun dayTotals(expenses: List<Expense>, anyDay: LocalDate): List<Long> {
    val start = weekStart(anyDay)
    return (0..6).map { i -> spentOn(expenses, start.plusDays(i.toLong())) }
}

private val dayMonth = java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH)
private val dayMonthYear = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH)

/** "3 \u2013 9 Nov"; adds the year when the week is not in [today]'s year ("29 Dec 2025 \u2013 4 Jan 2026" across years). */
fun weekRangeLabel(range: WeekRange, today: LocalDate): String {
    val s = range.start; val e = range.end
    val currentYear = s.year == today.year && e.year == today.year
    return when {
        s.year != e.year -> "${s.format(dayMonthYear)} \u2013 ${e.format(dayMonthYear)}"
        s.month == e.month -> "${s.dayOfMonth} \u2013 ${e.format(if (currentYear) dayMonth else dayMonthYear)}"
        else -> "${s.format(dayMonth)} \u2013 ${e.format(if (currentYear) dayMonth else dayMonthYear)}"
    }
}
