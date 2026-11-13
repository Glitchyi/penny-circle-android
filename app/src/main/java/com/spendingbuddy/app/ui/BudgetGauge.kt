package com.spendingbuddy.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** The gauge is an arc of this many degrees with its opening facing down. */
internal const val GAUGE_SWEEP_DEGREES = 240f
private const val GAUGE_START_DEGREES = 150f

/** Height of the gauge's bounding box for a given [diameter] and [stroke] width. */
internal fun gaugeHeight(diameter: Dp, stroke: Dp): Dp = diameter * 0.75f + stroke * 0.25f

/**
 * Budget gauge: a thick arc that fills with the accent gradient as [progress] (0..1) grows, with a glowing
 * knob at the end. [content] sits inside the arc. The gauge is decorative; the text inside carries the meaning.
 */
@Composable
fun BudgetGauge(
    progress: Float,
    over: Boolean,
    modifier: Modifier = Modifier,
    diameter: Dp = 288.dp,
    stroke: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val t = LocalPennyTokens.current
    val reduced = LocalReducedMotion.current
    // Starts empty and sweeps up to the value each time the screen is shown.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    // Read only inside the draw lambda, so the animation never recomposes the content.
    val level = animateFloatAsState(
        targetValue = if (shown || reduced) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = if (reduced) snap() else PennyMotion.spatialSlow(),
        label = "gauge"
    )
    // When spending goes up, a ring ripples out from the knob. 1 means the ripple is finished.
    val pulse = remember { Animatable(1f) }
    val previous = remember { floatArrayOf(progress) }
    LaunchedEffect(progress) {
        if (!reduced && progress > previous[0]) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, spring(dampingRatio = 1f, stiffness = 28f))
        }
        previous[0] = progress
    }
    val from = if (over) t.danger else t.accent
    val to = if (over) lerp(t.danger, t.accent2, 0.45f) else t.accent2
    val track = t.text.copy(alpha = if (t.dark) 0.10f else 0.08f)
    val knob = if (t.dark) Color.White else t.surface
    Box(
        modifier
            .size(width = diameter, height = gaugeHeight(diameter, stroke))
            .drawWithCache {
                val s = stroke.toPx()
                val r = (size.width - s) / 2f
                val center = Offset(size.width / 2f, s / 2f + r)
                val topLeft = Offset(center.x - r, center.y - r)
                val arcSize = Size(r * 2f, r * 2f)
                // The brush is defined in a frame rotated by 90 degrees so that its seam sits in the opening.
                val first = (GAUGE_START_DEGREES - 90f) / 360f
                val last = (GAUGE_START_DEGREES - 90f + GAUGE_SWEEP_DEGREES) / 360f
                val fill = Brush.sweepGradient(0f to from, first to from, last to to, 1f to to, center = center)
                val line = Stroke(width = s, cap = StrokeCap.Round)
                val halo = Stroke(width = s * 2f, cap = StrokeCap.Round)
                onDrawBehind {
                    val p = level.value
                    drawArc(track, GAUGE_START_DEGREES, GAUGE_SWEEP_DEGREES, false, topLeft, arcSize, style = line)
                    if (p > 0.002f) {
                        rotate(90f, center) {
                            val start = GAUGE_START_DEGREES - 90f
                            drawArc(fill, start, GAUGE_SWEEP_DEGREES * p, false, topLeft, arcSize, alpha = 0.16f, style = halo)
                            drawArc(fill, start, GAUGE_SWEEP_DEGREES * p, false, topLeft, arcSize, style = line)
                        }
                    }
                    val angle = Math.toRadians((GAUGE_START_DEGREES + GAUGE_SWEEP_DEGREES * p).toDouble())
                    val end = Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
                    val tip = lerp(from, to, p)
                    val ripple = pulse.value
                    if (ripple < 0.995f) {
                        drawCircle(
                            tip,
                            radius = s * (0.7f + 3.6f * ripple),
                            center = end,
                            alpha = (1f - ripple) * 0.6f,
                            style = Stroke(width = s * 0.28f * (1f - ripple) + 1f)
                        )
                    }
                    drawCircle(
                        Brush.radialGradient(listOf(tip.copy(alpha = 0.55f), Color.Transparent), center = end, radius = s * 2.2f),
                        radius = s * 2.2f,
                        center = end
                    )
                    drawCircle(tip, radius = s * 0.62f, center = end)
                    drawCircle(knob, radius = s * 0.34f, center = end)
                }
            },
        content = content
    )
}
