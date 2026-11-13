package com.spendingbuddy.app

/** Validated daily and Monday–Sunday weekly spending limits, stored in paise. */
data class OnboardingLimits(val dailyPaise: Long, val weeklyPaise: Long)

/** Exact rupee text with the same bounds as other budget entry; zero is a valid no-spend goal. */
fun parseOnboardingLimits(dailyText: String, weeklyText: String): OnboardingLimits? {
    val daily = amountToPaise(dailyText) ?: return null
    val weekly = amountToPaise(weeklyText) ?: return null
    return OnboardingLimits(daily, weekly)
}

/** A user-facing validation reason; null means both amounts are valid. */
fun onboardingLimitsError(dailyText: String, weeklyText: String): String? {
    val daily = amountToPaise(dailyText)
        ?: return "Enter a valid daily amount, with up to two decimal places."
    val weekly = amountToPaise(weeklyText)
        ?: return "Enter a valid weekly amount, with up to two decimal places."
    return null
}
