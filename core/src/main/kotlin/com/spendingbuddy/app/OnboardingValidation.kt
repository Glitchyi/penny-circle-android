package com.spendingbuddy.app

/** Validated daily and Monday–Sunday weekly spending limits, stored in paise. */
data class OnboardingLimits(val dailyPaise: Long, val weeklyPaise: Long)

/** Ungrouped editable rupees, preserving every paise without rounding. */
fun formatLimitInput(paise: Long): String {
    val value = paise.coerceAtLeast(0L)
    val whole = value / 100L
    val fraction = (value % 100L).toString().padStart(2, '0').trimEnd('0')
    return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
}

fun dailyLimitInputError(text: String): String? =
    if (amountToPaise(text) == null) "Enter a valid daily amount, with up to two decimal places." else null

fun weeklyLimitInputError(text: String): String? =
    if (amountToPaise(text) == null) "Enter a valid weekly amount, with up to two decimal places." else null

/** An optional helper; independent weekly limits and explicit zero remain valid. */
fun weeklyLimitFromDailyInput(dailyText: String): String? {
    val daily = amountToPaise(dailyText) ?: return null
    val weekly = try {
        Math.multiplyExact(daily, 7L)
    } catch (_: ArithmeticException) {
        return null
    }
    return formatLimitInput(weekly).takeIf { amountToPaise(it) != null }
}

/** Exact rupee text with the same bounds as other budget entry; zero is a valid no-spend goal. */
fun parseOnboardingLimits(dailyText: String, weeklyText: String): OnboardingLimits? {
    val daily = amountToPaise(dailyText) ?: return null
    val weekly = amountToPaise(weeklyText) ?: return null
    return OnboardingLimits(daily, weekly)
}

/** A user-facing validation reason; null means both amounts are valid. */
fun onboardingLimitsError(dailyText: String, weeklyText: String): String? {
    return dailyLimitInputError(dailyText) ?: weeklyLimitInputError(weeklyText)
}
