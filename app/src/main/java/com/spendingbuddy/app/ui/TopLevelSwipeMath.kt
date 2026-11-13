package com.spendingbuddy.app.ui

import kotlin.math.abs
import kotlin.math.sign

/** Returns the adjacent tab index when a physical drag passes the threshold, accounting for RTL order. */
internal fun topLevelSwipeTargetIndex(
    currentIndex: Int,
    dragXPx: Float,
    widthPx: Float,
    layoutDirectionSign: Float,
    thresholdFraction: Float = 0.22f,
    itemCount: Int = 3
): Int? {
    if (itemCount <= 1 || currentIndex !in 0 until itemCount || widthPx <= 0f) return null
    val logicalDrag = dragXPx * layoutDirectionSign
    if (abs(logicalDrag) < widthPx * thresholdFraction) return null
    val target = currentIndex - sign(logicalDrag).toInt()
    return target.takeIf { it in 0 until itemCount }
}

/** Keeps the pane tracking the finger, then adds a gentle elastic resistance past 55% of its width. */
internal fun resistedPaneOffset(dragXPx: Float, widthPx: Float): Float {
    if (widthPx <= 0f) return 0f
    val magnitude = abs(dragXPx)
    val resistanceStart = widthPx * 0.55f
    val resisted = if (magnitude <= resistanceStart) magnitude
    else resistanceStart + (magnitude - resistanceStart) * 0.34f
    return sign(dragXPx) * resisted.coerceAtMost(widthPx * 0.88f)
}
