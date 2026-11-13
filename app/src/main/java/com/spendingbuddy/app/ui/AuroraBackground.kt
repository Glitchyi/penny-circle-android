package com.spendingbuddy.app.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * The app background: the flat theme colour, one faint glow of the accent behind the top of the screen, and a
 * fine dot grid that fades out down the page. It is static, so it is drawn once and costs nothing afterwards.
 * [animate] is kept for callers and no longer does anything.
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.auroraBackground(animate: Boolean = true): Modifier = composed {
    val t = LocalPennyTokens.current
    drawWithCache {
        val w = size.width
        val h = size.height
        val glow = Brush.radialGradient(
            colors = listOf(t.accent.copy(alpha = if (t.dark) 0.20f else 0.10f), Color.Transparent),
            center = Offset(w * 0.5f, -h * 0.06f),
            radius = w * 0.95f
        )
        val step = 22.dp.toPx()
        val gridBottom = h * 0.46f
        // Each row is one draw call, so the grid can fade row by row.
        val rows = buildList {
            var y = step
            while (y < gridBottom) {
                val points = buildList { var x = step / 2f; while (x < w) { add(Offset(x, y)); x += step } }
                add(points to (1f - y / gridBottom))
                y += step
            }
        }
        val dot = t.text
        val dotAlpha = if (t.dark) 0.11f else 0.09f
        val dotSize = 1.5.dp.toPx()
        onDrawBehind {
            drawRect(t.bg)
            drawRect(glow)
            rows.forEach { (points, fade) ->
                drawPoints(points, PointMode.Points, dot.copy(alpha = dotAlpha * fade), strokeWidth = dotSize, cap = StrokeCap.Round)
            }
        }
    }
}
