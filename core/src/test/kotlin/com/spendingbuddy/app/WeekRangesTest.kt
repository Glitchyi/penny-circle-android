package com.spendingbuddy.app

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekRangesTest {
    private fun e(date: String, paise: Long, kind: String = "expense", at: Long = 0) =
        Expense(id = "$date-$paise-$kind", date = date, amountPaise = paise, kind = kind, updatedAt = at)

    private val mon = LocalDate.of(2026, 11, 9)
    private val sun = LocalDate.of(2026, 11, 15)

    @Test fun rangeIsMondayToSundayFromAnyDay() {
        assertEquals(WeekRange(mon, sun), weekRange(mon))
        assertEquals(WeekRange(mon, sun), weekRange(sun))
        assertEquals(WeekRange(mon, sun), weekRange(LocalDate.of(2026, 11, 12)))
    }

    @Test fun sundayBelongsToOldWeekMondayToNew() {
        val ex = listOf(e("2026-11-08", 100), e("2026-11-09", 200), e("2026-11-15", 300), e("2026-11-16", 400))
        assertEquals(listOf(200L, 0L, 0L, 0L, 0L, 0L, 300L), dayTotals(ex, mon))
        assertEquals(listOf(100L), groupByDay(ex, mon.minusWeeks(1)).map { it.totalPaise })
        assertEquals(listOf(LocalDate.of(2026, 11, 15), mon), groupByDay(ex, mon).map { it.date })
    }

    @Test fun refundsSubtractAndTransfersAreIgnored() {
        val ex = listOf(e("2026-11-10", 500), e("2026-11-10", 200, "refund"), e("2026-11-10", 900, "transfer"))
        assertEquals(300L, dayTotals(ex, mon)[1])
        val g = groupByDay(ex, mon).single()
        assertEquals(300L, g.totalPaise)
        assertEquals(3, g.purchases.size)
    }

    @Test fun groupsSortedNewestFirstWithinDay() {
        val ex = listOf(e("2026-11-10", 1, at = 1), e("2026-11-10", 2, at = 5))
        assertEquals(listOf(2L, 1L), groupByDay(ex, mon).single().purchases.map { it.amountPaise })
    }

    @Test fun weeksBackBounds() {
        assertEquals(1, weeksBack(emptyList(), sun))
        assertEquals(1, weeksBack(listOf(e("2026-11-09", 1)), sun))
        assertEquals(2, weeksBack(listOf(e("2026-11-08", 1)), mon))
        assertEquals(1, weeksBack(listOf(e("2026-12-01", 1)), mon)) // future ignored
        assertEquals(1, weeksBack(listOf(e("bad", 1)), mon))
        assertEquals(52, weeksBack(listOf(e("2020-01-01", 1)), mon))
        assertEquals(3, weeksBack(listOf(e("2020-01-01", 1)), mon, max = 3))
    }

    @Test fun pageStartsStepBackByWeek() {
        assertEquals(mon, weekStartForPage(sun, 0))
        assertEquals(mon.minusWeeks(2), weekStartForPage(sun, 2))
    }

    @Test fun rangeLabels() {
        val today = LocalDate.of(2026, 11, 12)
        assertEquals("9 – 15 Nov", weekRangeLabel(weekRange(mon), today))
        assertEquals("26 Oct – 1 Nov", weekRangeLabel(weekRange(mon.minusWeeks(2)), today))
        assertEquals("29 Dec 2025 – 4 Jan 2026", weekRangeLabel(weekRange(LocalDate.of(2025, 12, 29)), today))
        assertEquals("1 – 7 Dec 2025", weekRangeLabel(weekRange(LocalDate.of(2025, 12, 1)), today))
    }
}
