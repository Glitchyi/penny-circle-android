package com.spendingbuddy.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun budgetIntensity(spentPaise: Long, limitPaise: Long): Float =
    (spentPaise.toFloat() / limitPaise.coerceAtLeast(1L)).coerceIn(0f, 1f)

/** Fades the whole drawing out toward the bottom edge. */
private val horizonFade = Brush.verticalGradient(
    0f to Color.Transparent, 0.22f to Color.Black, 0.6f to Color.Black, 1f to Color.Transparent
)

/** A wide, shallow arch with a glow. [intensity] (0..1) brightens the horizon. */
@Composable
fun HorizonArc(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    intensity: Float = 0.5f,
    height: Dp = 84.dp,
    secondary: Color = color
) {
    val reduced = LocalReducedMotion.current
    val target = intensity.coerceIn(0f, 1f)
    val level by animateFloatAsState(
        targetValue = target,
        animationSpec = if (reduced) snap() else tween(900),
        label = "horizon"
    )
    Spacer(
        modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(horizonFade, blendMode = BlendMode.DstIn)
            }
            .drawWithCache {
                val w = size.width
                val h = size.height
                // The crest sits a little below the box top so the glow above it can fade out softly.
                val top = 14.dp.toPx()
                val ry = h - top
                // Upper half of an ellipse centred on the bottom edge: endpoints at the bottom corners, apex at y = top.
                val arch = Path().apply { arcTo(Rect(0f, top, w, top + 2 * ry), 180f, 180f, true) }
                val area = Path().apply {
                    arcTo(Rect(0f, top, w, top + 2 * ry), 180f, 180f, true)
                    close()
                }
                val glowRadius = 120.dp.toPx()
                val glowCenter = Offset(w / 2f, top)
                // Brushes use full-alpha colours; the animated level is applied as draw alpha.
                val glowBrush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 1f), Color.Transparent),
                    center = glowCenter,
                    radius = glowRadius
                )
                val areaBrush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 1f), Color.Transparent),
                    center = Offset(w / 2f, 0f),
                    radius = w * 0.6f
                )
                val archBrush = Brush.horizontalGradient(
                    listOf(color.copy(alpha = 0.75f), secondary.copy(alpha = 0.75f), color.copy(alpha = 0.75f))
                )
                val secondaryRadius = 80.dp.toPx()
                val secondaryCenter = Offset(w * 0.3f, top)
                val secondaryBrush = Brush.radialGradient(
                    colors = listOf(secondary.copy(alpha = 1f), Color.Transparent),
                    center = secondaryCenter,
                    radius = secondaryRadius
                )
                val stroke = Stroke(width = 1.dp.toPx())
                onDrawBehind {
                    drawCircle(
                        brush = glowBrush,
                        radius = glowRadius,
                        center = glowCenter,
                        alpha = 0.20f * (0.5f + level)
                    )
                    drawCircle(
                        brush = secondaryBrush,
                        radius = secondaryRadius,
                        center = secondaryCenter,
                        alpha = 0.18f * (0.5f + level)
                    )
                    drawPath(path = area, brush = areaBrush, alpha = 0.10f + 0.25f * level)
                    drawPath(path = arch, brush = archBrush, style = stroke)
                }
            }
    )
}
