package com.spendingbuddy.app.ui

/** Widest the main column is allowed to grow, so foldables and tablets do not stretch the layout. */
const val MAX_CONTENT_WIDTH_DP = 640

/** Screens shorter than this (in dp), or users with a large font scale, get the compact hero. */
const val COMPACT_HEIGHT_DP = 780
const val COMPACT_FONT_SCALE = 1.3f

/** True when vertical space is tight: short screens (small phones, split screen) or large font settings. */
fun isCompactLayout(screenHeightDp: Int, fontScale: Float): Boolean =
    screenHeightDp < COMPACT_HEIGHT_DP || fontScale > COMPACT_FONT_SCALE

/** Number of grouping commas in an Indian-grouped integer with [digits] digits (12,34,567 has 2). */
internal fun indianCommaCount(digits: Int): Int = if (digits <= 3) 0 else (digits - 2) / 2

/**
 * Picks the hero amount's font size in sp so that "₹", the digits and (when present) the paise fit
 * [availableWidthDp]. Digits are about 0.57em wide with the tight letter spacing used, a comma about 0.28em,
 * the "₹" about 0.25em, and the paise text is a fixed ~52dp. [fontScale] is the (capped) user font scale,
 * because 1sp is [fontScale] dp. The result never exceeds [maxSp] or drops below [minSp].
 */
internal fun heroFontSizeSp(
    availableWidthDp: Float,
    digitCount: Int,
    hasPaise: Boolean,
    fontScale: Float = 1f,
    maxSp: Float = 96f,
    minSp: Float = 44f
): Float {
    val digits = digitCount.coerceAtLeast(1)
    val em = digits * 0.57f + indianCommaCount(digits) * 0.28f + 0.25f
    val room = (availableWidthDp - if (hasPaise) 52f else 0f).coerceAtLeast(0f)
    val sp = room / em / fontScale.coerceAtLeast(0.5f)
    return sp.coerceIn(minSp, maxSp)
}
