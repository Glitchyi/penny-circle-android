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

/** A neighbor must be present from the first drag pixel, before the commit threshold is reached. */
internal fun topLevelSwipePreviewIndex(currentIndex: Int, dragXPx: Float, layoutDirectionSign: Float): Int? =
    if (dragXPx == 0f) null else topLevelSwipeTargetIndex(
        currentIndex, dragXPx, 1f, layoutDirectionSign, thresholdFraction = 0f
    )

/** The two opaque panes share this edge throughout the gesture, including its final frame. */
internal fun panePreviewOffset(sourceOffsetPx: Float, widthPx: Float): Float = when {
    sourceOffsetPx < 0f -> sourceOffsetPx + widthPx
    sourceOffsetPx > 0f -> sourceOffsetPx - widthPx
    else -> widthPx
}
