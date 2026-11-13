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
}
