package com.spendingbuddy.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsiveTest {
    @Test fun commaCountFollowsIndianGrouping() {
        assertEquals(0, indianCommaCount(1))
        assertEquals(0, indianCommaCount(3))
        assertEquals(1, indianCommaCount(4))   // 1,000
        assertEquals(1, indianCommaCount(5))   // 10,000
        assertEquals(2, indianCommaCount(6))   // 1,00,000
        assertEquals(2, indianCommaCount(7))   // 10,00,000
        assertEquals(3, indianCommaCount(8))   // 1,00,00,000
    }

    @Test fun shortAmountsGetTheMaximumSize() {
        assertEquals(96f, heroFontSizeSp(324f, 3, hasPaise = false), 0.001f)
    }

    @Test fun longerAmountsGetSmallerButStayInsideTheBounds() {
        var previous = Float.MAX_VALUE
        for (digits in 1..9) {
            val sp = heroFontSizeSp(324f, digits, hasPaise = false)
            assertTrue("digits=$digits sp=$sp", sp <= previous)
            assertTrue(sp in 44f..96f)
            previous = sp
        }
    }

    @Test fun amountAlwaysFitsTheAvailableWidthUnlessAtTheMinimum() {
        for (width in listOf(300f, 324f, 360f, 411f)) {
            for (digits in 1..8) {
                val sp = heroFontSizeSp(width, digits, hasPaise = true)
                val em = digits * 0.57f + indianCommaCount(digits) * 0.28f + 0.25f
                if (sp > 44f) assertTrue("width=$width digits=$digits", sp * em + 52f <= width + 0.01f)
            }
        }
    }

    @Test fun paiseAndBigFontScaleShrinkTheAmount() {
        val plain = heroFontSizeSp(300f, 6, hasPaise = false)
        assertTrue(heroFontSizeSp(300f, 6, hasPaise = true) < plain)
        assertTrue(heroFontSizeSp(300f, 6, hasPaise = false, fontScale = 1.3f) < plain)
    }

    @Test fun compactLayoutForShortScreensAndLargeFonts() {
        assertFalse(isCompactLayout(914, 1.0f))   // OnePlus Nord CE class
        assertFalse(isCompactLayout(823, 1.0f))   // Xiaomi 15 class
        assertTrue(isCompactLayout(740, 1.0f))    // short phone or split screen
        assertTrue(isCompactLayout(914, 1.5f))    // large font
        assertFalse(isCompactLayout(780, 1.3f))   // boundaries are exclusive
    }
}
