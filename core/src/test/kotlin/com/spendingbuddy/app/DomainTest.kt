package com.spendingbuddy.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZonedDateTime

class DomainTest {
    @Test fun moneyUsesExactPaise() {
        assertEquals(25000L, amountToPaise("250"))
        assertEquals(25075L, amountToPaise("250.75"))
        assertEquals(123450L, amountToPaise("₹1,234.50"))
        assertNull(amountToPaise("-1")); assertNull(amountToPaise("1.001")); assertNull(amountToPaise(""))
    }
    @Test fun graceEndsAtTenIndiaTime() {
        val date = LocalDate.parse("2026-11-07")
        assertTrue(eligibleCheckIn(date, ZonedDateTime.parse("2026-11-08T09:59:59+05:30[Asia/Kolkata]")))
        assertTrue(eligibleCheckIn(date, ZonedDateTime.parse("2026-11-08T10:00:00+05:30[Asia/Kolkata]")))
        assertFalse(eligibleCheckIn(date, ZonedDateTime.parse("2026-11-08T10:00:01+05:30[Asia/Kolkata]")))
    }
    @Test fun lateEntriesDoNotRestoreStreak() {
        val now = ZonedDateTime.parse("2026-11-08T12:00:00+05:30[Asia/Kolkata]")
        val entries = listOf(CheckIn("2026-11-06",0,false,true,false),CheckIn("2026-11-07",0,true,false,false))
        assertEquals(0, streak(entries,now))
    }
    @Test fun graceDoesNotPrematurelyBreakStreak() {
        val entries = listOf(CheckIn("2026-11-05",0,false,true,false),CheckIn("2026-11-06",0,false,true,false))
        assertEquals(2, streak(entries,ZonedDateTime.parse("2026-11-08T09:00:00+05:30[Asia/Kolkata]")))
        assertEquals(0, streak(entries,ZonedDateTime.parse("2026-11-08T10:01:00+05:30[Asia/Kolkata]")))
    }
    @Test fun mondayStartsBudgetWeek() { assertEquals(LocalDate.parse("2026-11-02"), weekStart(LocalDate.parse("2026-11-08"))) }
    @Test fun transfersExcludedAndRefundsReduceNet() {
        assertEquals(800L,netSpending(listOf(Expense(amountPaise=1000),Expense(amountPaise=200,kind="refund"),Expense(amountPaise=500,kind="transfer"))))
    }
}
