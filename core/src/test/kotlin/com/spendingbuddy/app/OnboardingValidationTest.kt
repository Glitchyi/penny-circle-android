package com.spendingbuddy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingValidationTest {
    @Test fun parsesExactPaiseWithoutRounding() {
        assertEquals(OnboardingLimits(50050L, 350000L), parseOnboardingLimits("500.50", "3,500"))
        assertNull(onboardingLimitsError("500.50", "3,500"))
    }

    @Test fun rejectsInvalidPrecisionNegativeAndOverflowingLimits() {
        assertNull(parseOnboardingLimits("500.001", "3500"))
        assertNull(parseOnboardingLimits("-1", "3500"))
        assertNull(parseOnboardingLimits("500", "100000000.00"))
        assertNull(parseOnboardingLimits("", "3500"))
    }

    @Test fun acceptsZeroGoalsAndIndependentDailyWeeklyLimits() {
        assertEquals(OnboardingLimits(0, 0), parseOnboardingLimits("0", "0"))
        assertEquals(OnboardingLimits(50100, 50099), parseOnboardingLimits("501", "500.99"))
        assertNull(onboardingLimitsError("0", "0"))
    }

    @Test fun prefilledLimitsRoundTripWithoutLosingPaise() {
        val values = listOf(0L, 1L, 10L, 99L, 100L, 30_050L, 50_001L, 9_999_999_999L)
        values.forEach { paise ->
            val prefill = formatLimitInput(paise)
            assertEquals("Round trip for $paise paise", paise, amountToPaise(prefill))
            assertEquals(OnboardingLimits(paise, paise), parseOnboardingLimits(prefill, prefill))
        }
        assertEquals("300.5", formatLimitInput(30_050L))
        assertEquals("500.01", formatLimitInput(50_001L))
        assertEquals("0", formatLimitInput(0L))
    }

    @Test fun malformedOrOutOfRangeInputHasTheRelevantFieldError() {
        val invalid = listOf("", " ", "abc", "1.2.3", "-0.01", "0.001", "100000000", "9223372036854775807")
        invalid.forEach { text ->
            assertNull(parseOnboardingLimits(text, "0"))
            assertNull(parseOnboardingLimits("0", text))
            assertEquals("Enter a valid daily amount, with up to two decimal places.", dailyLimitInputError(text))
            assertEquals("Enter a valid weekly amount, with up to two decimal places.", weeklyLimitInputError(text))
        }
        assertNull(dailyLimitInputError("0"))
        assertNull(weeklyLimitInputError("0.01"))
    }

    @Test fun sevenDayHelperKeepsExactPaiseAndExplicitZero() {
        assertEquals("2103.5", weeklyLimitFromDailyInput("300.50"))
        assertEquals("0.07", weeklyLimitFromDailyInput("0.01"))
        assertEquals("0", weeklyLimitFromDailyInput("0"))
        assertEquals("99999999.96", weeklyLimitFromDailyInput("14285714.28"))
    }

    @Test fun sevenDayHelperRejectsInvalidDailyAndOutOfRangeWeeklyAmounts() {
        listOf("", "abc", "-1", "1.001", "14285714.29", "99999999.99", "9223372036854775807").forEach {
            assertNull("Unsafe weekly helper for $it", weeklyLimitFromDailyInput(it))
        }
    }
}
