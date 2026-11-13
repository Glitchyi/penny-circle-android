package com.spendingbuddy.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Soft scroll edges: content dissolves into the background instead of being cut mid-line.
 * A fade only appears on an edge when there is more content in that direction, so a list that
 * is at its top (or fits on screen) shows no fade at all.
 */
fun Modifier.verticalFadingEdges(topFade: Dp, bottomFade: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val t = topFade.toPx().coerceIn(0f, size.height / 2f)
        val b = bottomFade.toPx().coerceIn(0f, size.height / 2f)
        if (t > 0f) {
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = 0f, endY = t),
                size = Size(size.width, t),
                blendMode = BlendMode.DstIn
            )
        }
        if (b > 0f) {
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Black, Color.Transparent), startY = size.height - b, endY = size.height),
                topLeft = Offset(0f, size.height - b),
                size = Size(size.width, b),
                blendMode = BlendMode.DstIn
            )
        }
    }

/** Scroll-aware fading edges for a LazyColumn / LazyRow state. [bottom] should cover any floating bar the list scrolls under. */
@Composable
fun Modifier.scrollFadingEdges(state: LazyListState, top: Dp = 28.dp, bottom: Dp = 72.dp): Modifier =
    fadeFor(state.canScrollBackward, state.canScrollForward, top, bottom)

/** Same for a `verticalScroll` content area. */
@Composable
fun Modifier.scrollFadingEdges(state: ScrollState, top: Dp = 28.dp, bottom: Dp = 72.dp): Modifier =
    fadeFor(state.canScrollBackward, state.canScrollForward, top, bottom)

@Composable
private fun Modifier.fadeFor(canBack: Boolean, canForward: Boolean, top: Dp, bottom: Dp): Modifier {
    val reduced = LocalReducedMotion.current
    val spec = if (reduced) snap<Dp>() else tween<Dp>(160)
    val t by animateDpAsState(if (canBack) top else 0.dp, spec, label = "fadeTop")
    val b by animateDpAsState(if (canForward) bottom else 0.dp, spec, label = "fadeBottom")
    return verticalFadingEdges(t, b)
}
