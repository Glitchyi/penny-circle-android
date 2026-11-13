package com.spendingbuddy.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PennyMathTest {
    @Test
    fun formatIndianGroupsDigitsLikeIndianNumbering() {
        assertEquals("0", formatIndian(0L))
        assertEquals("7", formatIndian(7L))
        assertEquals("999", formatIndian(999L))
        assertEquals("1,000", formatIndian(1000L))
        assertEquals("12,345", formatIndian(12345L))
        assertEquals("1,00,000", formatIndian(100000L))
        assertEquals("12,34,567", formatIndian(1234567L))
        assertEquals("1,23,45,678", formatIndian(12345678L))
    }

    @Test
    fun formatIndianHandlesNegativeValues() {
        assertEquals("-1,234", formatIndian(-1234L))
        assertEquals("-1,00,000", formatIndian(-100000L))
    }

    @Test
    fun formatIndianHandlesLongMaxValue() {
        assertEquals("92,23,37,20,36,85,47,75,807", formatIndian(Long.MAX_VALUE))
    }

    @Test
    fun isCountingUpIsTrueWhenCurrentIsNotLower() {
        assertTrue(isCountingUp(100L, 100L))
        assertTrue(isCountingUp(199L, 200L))
        assertTrue(isCountingUp(0L, 0L))
    }

    @Test
    fun isCountingUpIsFalseWhenCurrentDrops() {
        assertFalse(isCountingUp(200L, 199L))
    }

    @Test
    fun budgetIntensityMapsSpendToZeroToOne() {
        val delta = 0.0001f
        assertEquals(0f, budgetIntensity(0L, 50000L), delta)
        assertEquals(0.5f, budgetIntensity(25000L, 50000L), delta)
        assertEquals(1f, budgetIntensity(50000L, 50000L), delta)
    }

    @Test
    fun budgetIntensityClampsAboveLimit() {
        assertEquals(1f, budgetIntensity(90000L, 50000L), 0.0001f)
    }

    @Test
    fun budgetIntensityTreatsZeroLimitAsOne() {
        assertEquals(1f, budgetIntensity(1000L, 0L), 0.0001f)
    }

    @Test
    fun budgetIntensityClampsNegativeSpendToZero() {
        assertEquals(0f, budgetIntensity(-500L, 50000L), 0.0001f)
    }
}
