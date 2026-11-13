package com.spendingbuddy.app.ui

import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.abs

internal const val ROCKET_DURATION_MS = 2600
internal const val ROCKET_BURST_T = 0.58f

/** Vertical centre of the rocket for progress t (0..1, clamped). Accelerates from below the bottom edge to above the top edge. */
internal fun rocketY(t: Float, heightPx: Float, rocketLengthPx: Float): Float {
    val c = t.coerceIn(0f, 1f)
    val e = c * c * (1.6f - 0.6f * c)
    val start = heightPx + 0.6f * rocketLengthPx
    val end = -rocketLengthPx
    return start + (end - start) * e
}

/** Horizontal centre of the rocket: slight wobble that dies out by t = 1. */
internal fun rocketX(t: Float, widthPx: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return widthPx / 2f + sin(c * 9f) * widthPx * 0.012f * (1f - c)
}

/**
 * Full-screen rocket launch overlay. Never intercepts touches.
 * Composes nothing while [play] is false.
 */
@Composable
fun RocketCelebration(play: Boolean, haptics: Boolean, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val reducedMotion = LocalReducedMotion.current
    val tokens = LocalPennyTokens.current
    val context = LocalContext.current
    val view = LocalView.current
    val vibrator = remember(context) { context.getSystemService(VibratorManager::class.java)?.defaultVibrator }
    val currentOnFinished = rememberUpdatedState(onFinished)

    LaunchedEffect(play) {
        if (!play) return@LaunchedEffect
        if (reducedMotion) {
            if (haptics) runCatching { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }
            delay(420L)
            currentOnFinished.value()
            return@LaunchedEffect
        }
        val v = vibrator
        if (haptics && v != null) {
            launch { runCelebrationHaptics(v, view) }
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(ROCKET_DURATION_MS, easing = LinearEasing))
        currentOnFinished.value()
    }

    DisposableEffect(play) {
        onDispose { runCatching { vibrator?.cancel() } }
    }

    if (play) {
        Canvas(modifier.fillMaxSize()) {
            if (reducedMotion) drawReducedCelebration(tokens) else drawCelebration(progress.value, tokens)
        }
    }
}

private suspend fun runCelebrationHaptics(vibrator: Vibrator, view: View) {
    val attrs = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    // a. Ignition
    runCatching {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK), attrs)
    }
    // b. Engine rumble ramp, starts ~120 ms after ignition
    delay(120)
    runCatching {
        val effect = if (vibrator.hasAmplitudeControl()) {
            val segments = 22
            val timings = LongArray(1 + segments * 2)
            val amplitudes = IntArray(1 + segments * 2)
            for (k in 0 until segments) {
                timings[2 * k + 1] = 45L   // ON
                timings[2 * k + 2] = 25L   // OFF
                amplitudes[2 * k + 1] = (40 + (200 - 40) * k / (segments - 1))
                amplitudes[2 * k + 2] = 0
            }
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        } else {
            val segments = 22
            val timings = LongArray(1 + segments * 2)
            for (k in 0 until segments) {
                timings[2 * k + 1] = 45L
                timings[2 * k + 2] = 25L
            }
            VibrationEffect.createWaveform(timings, -1)
        }
        vibrator.vibrate(effect, attrs)
    }
    // c. Burst, measured from ignition
    delay((ROCKET_DURATION_MS * ROCKET_BURST_T).toLong() - 120L)
    runCatching {
        val quick = VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
        val click = VibrationEffect.Composition.PRIMITIVE_CLICK
        val thud = VibrationEffect.Composition.PRIMITIVE_THUD
        val effect = if (vibrator.areAllPrimitivesSupported(quick, click, thud)) {
            VibrationEffect.startComposition()
                .addPrimitive(quick, 0.8f)
                .addPrimitive(click, 1.0f, 40)
                .addPrimitive(thud, 1.0f, 70)
                .compose()
        } else {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
        vibrator.vibrate(effect, attrs)
    }
    // d. Success tap
    delay(350)
    runCatching { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }
}

private fun DrawScope.drawCelebration(t: Float, tokens: PennyTokens) {
    val w = size.width
    val h = size.height
    val len = 96.dp.toPx()
    drawTrail(t, w, h, len, density, tokens)
    drawRocket(t, w, h, len, tokens)
    drawStarburst(t, w, h, tokens)
}

/** A still, compact acknowledgement for users who prefer reduced motion. */
private fun DrawScope.drawReducedCelebration(tokens: PennyTokens) {
    val len = 92.dp.toPx()
    drawRocket(0.62f, size.width, size.height, len, tokens)
    drawConfetti(0.16f, size.width, size.height, tokens, particleCount = 18, spread = 0.34f)
}

private fun DrawScope.drawTrail(t: Float, w: Float, h: Float, len: Float, dens: Float, tokens: PennyTokens) {
    if (t <= 0f) return
    val startY = rocketY(t, h, len) + len * 0.44f
    val past = (t - 0.13f).coerceAtLeast(0f)
    val endY = rocketY(past, h, len) + len * 0.46f
    val cx = rocketX(t, w)
    val drift = (rocketX(past, w) - cx) * 0.72f
    val plumeLength = (endY - startY).coerceAtLeast(1f)
    val densityScale = dens.coerceIn(0.7f, 1.4f)

    // Two soft, tapered wisps blend into the flame instead of reading as a dotted trail.
    for ((layer, halfWidthDp) in listOf(0 to 13f, 1 to 7f)) {
        val topHalfWidth = halfWidthDp * 0.34f * densityScale
        val middleHalfWidth = halfWidthDp * densityScale
        val plume = Path().apply {
            moveTo(cx - topHalfWidth, startY)
            cubicTo(
                cx - middleHalfWidth * 0.88f, startY + plumeLength * 0.34f,
                cx + drift - middleHalfWidth * 0.7f, endY - plumeLength * 0.28f,
                cx + drift, endY
            )
            cubicTo(
                cx + drift + middleHalfWidth * 0.7f, endY - plumeLength * 0.28f,
                cx + middleHalfWidth * 0.88f, startY + plumeLength * 0.34f,
                cx + topHalfWidth, startY
            )
            close()
        }
        val outer = layer == 0
        drawPath(
            plume,
            brush = Brush.verticalGradient(
                colors = if (outer) listOf(tokens.accent.copy(alpha = 0.34f), tokens.accent.copy(alpha = 0.12f), Color.Transparent)
                else listOf(tokens.text.copy(alpha = 0.64f), tokens.accent.copy(alpha = 0.24f), Color.Transparent),
                startY = startY,
                endY = endY
            )
        )
    }
}

private fun DrawScope.drawRocket(t: Float, w: Float, h: Float, len: Float, tokens: PennyTokens) {
    val cx = rocketX(t, w)
    val cy = rocketY(t, h, len)
    val w2 = 19.dp.toPx()
    val portholeR = 7.dp.toPx()
    val flameLen = len * (0.42f + 0.10f * sin(t * 90f))
    val flameTop = len * 0.46f

    translate(left = cx, top = cy) {
        // Flame under the rocket (drawn first so the body overlaps its top)
        val flame = Path().apply {
            moveTo(-10.dp.toPx(), flameTop)
            cubicTo(-12.dp.toPx(), flameTop + flameLen * 0.28f, -4.dp.toPx(), flameTop + flameLen * 0.78f, 0f, flameTop + flameLen)
            cubicTo(4.dp.toPx(), flameTop + flameLen * 0.78f, 12.dp.toPx(), flameTop + flameLen * 0.28f, 10.dp.toPx(), flameTop)
            close()
        }
        drawPath(
            path = flame,
            brush = Brush.verticalGradient(
                colors = listOf(tokens.text, tokens.accent.copy(alpha = 0.94f), Color.Transparent),
                startY = flameTop,
                endY = flameTop + flameLen
            )
        )
        val innerFlame = Path().apply {
            moveTo(-4.dp.toPx(), flameTop)
            cubicTo(-4.dp.toPx(), flameTop + flameLen * 0.30f, -1.dp.toPx(), flameTop + flameLen * 0.56f, 0f, flameTop + flameLen * 0.70f)
            cubicTo(1.dp.toPx(), flameTop + flameLen * 0.56f, 4.dp.toPx(), flameTop + flameLen * 0.30f, 4.dp.toPx(), flameTop)
            close()
        }
        drawPath(innerFlame, color = tokens.text.copy(alpha = 0.9f))

        // Body: capsule with pointed nose
        val body = Path().apply {
            moveTo(0f, -len / 2f)
            cubicTo(w2 * 0.6f, -len / 2f, w2, -len * 0.25f, w2, len * 0.25f)
            lineTo(w2, len * 0.32f)
            lineTo(w2 * 0.5f, len * 0.5f)
            lineTo(-w2 * 0.5f, len * 0.5f)
            lineTo(-w2, len * 0.32f)
            lineTo(-w2, len * 0.25f)
            cubicTo(-w2, -len * 0.25f, -w2 * 0.6f, -len / 2f, 0f, -len / 2f)
            close()
        }
        drawPath(
            path = body,
            brush = Brush.horizontalGradient(
                listOf(lerp(tokens.text, tokens.bg, 0.10f), tokens.text, lerp(tokens.text, tokens.bg, 0.18f)),
                startX = -w2,
                endX = w2
            )
        )
        drawPath(path = body, color = tokens.accent.copy(alpha = 0.82f), style = Stroke(width = 1.5.dp.toPx()))

        // Nose cone: top 30% of the body
        val noseBottom = -len / 2f + len * 0.3f
        val nose = Path().apply {
            moveTo(0f, -len / 2f)
            cubicTo(w2 * 0.6f, -len / 2f, w2 * 0.9f, -len * 0.35f, w2 * 0.95f, noseBottom)
            lineTo(-w2 * 0.95f, noseBottom)
            cubicTo(-w2 * 0.9f, -len * 0.35f, -w2 * 0.6f, -len / 2f, 0f, -len / 2f)
            close()
        }
        drawPath(path = nose, brush = Brush.verticalGradient(listOf(tokens.accent, tokens.accent), startY = -len / 2f, endY = noseBottom))

        // Fins
        val leftFin = Path().apply {
            moveTo(-w2, len * 0.05f)
            lineTo(-w2 - 12.dp.toPx(), len * 0.36f)
            lineTo(-w2 + 2.dp.toPx(), len * 0.29f)
            close()
        }
        val rightFin = Path().apply {
            moveTo(w2, len * 0.05f)
            lineTo(w2 + 12.dp.toPx(), len * 0.36f)
            lineTo(w2 - 2.dp.toPx(), len * 0.29f)
            close()
        }
        drawPath(path = leftFin, color = tokens.accent)
        drawPath(path = rightFin, brush = Brush.horizontalGradient(listOf(tokens.accent, tokens.accent)))

        // A slim body band gives the tail a clear visual anchor.
        drawRoundRect(
            color = tokens.accent.copy(alpha = 0.88f),
            topLeft = Offset(-w2 * 0.78f, len * 0.27f),
            size = Size(w2 * 1.56f, 5.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5.dp.toPx())
        )

        // Porthole
        val portholeCentre = Offset(0f, -len * 0.08f)
        drawCircle(color = tokens.bg, radius = portholeR, center = portholeCentre)
        drawCircle(
            color = tokens.accent,
            radius = portholeR + 1.dp.toPx(),
            center = portholeCentre,
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = tokens.accent,
            radius = portholeR,
            center = portholeCentre,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(color = tokens.text.copy(alpha = 0.18f), radius = portholeR * 0.28f, center = portholeCentre + Offset(-2.dp.toPx(), -2.dp.toPx()))
    }
}

private fun DrawScope.drawStarburst(t: Float, w: Float, h: Float, tokens: PennyTokens) {
    val bt = (t - ROCKET_BURST_T) / 0.29f
    if (bt !in 0f..1f) return

    val cx = w / 2f
    val cy = h * 0.32f
    val centre = Offset(cx, cy)

    // One restrained expanding ring and a short, seeded confetti burst.
    drawCircle(
        color = tokens.accent.copy(alpha = (1f - bt) * 0.28f),
        radius = bt * w * 0.31f,
        center = centre,
        style = Stroke(width = 2.dp.toPx())
    )
    drawConfetti(bt, w, h, tokens, particleCount = 44)
}

private fun DrawScope.drawConfetti(
    progress: Float,
    width: Float,
    height: Float,
    tokens: PennyTokens,
    particleCount: Int = 44,
    spread: Float = 1f
) {
    val p = progress.coerceIn(0f, 1f)
    val eased = p * (2f - p)
    val cx = width / 2f
    val cy = height * 0.32f
    val palette = listOf(
        tokens.accent,
        tokens.accent,
        lerp(tokens.accent, tokens.text, 0.28f),
        lerp(tokens.accent, tokens.text, 0.34f)
    )
    val twoPi = (2.0 * PI).toFloat()
    for (i in 0 until particleCount) {
        val a = seededParticle(i, 11)
        val b = seededParticle(i, 29)
        val c = seededParticle(i, 47)
        val angle = twoPi * a
        val speed = (0.24f + b * 0.58f) * width * 0.48f * spread
        val drift = sin((p * 5f + c * twoPi) * 2f) * width * 0.012f
        val pos = Offset(
            cx + cos(angle) * speed * eased + drift,
            cy + sin(angle) * speed * eased + 0.5f * height * 0.16f * eased * eased
        )
        val alpha = (1f - p).coerceIn(0f, 1f)
        val length = (5f + c * 3f).dp.toPx()
        val thickness = (2.2f + b * 1.8f).dp.toPx()
        val rotation = (a * 360f + p * (b - 0.5f) * 380f)
        val color = palette[i % palette.size]
        if (i % 5 == 0) {
            drawCircle(color = color, radius = thickness * 0.62f, center = pos, alpha = alpha)
        } else {
            rotate(degrees = rotation, pivot = pos) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(pos.x - length * 0.5f, pos.y - thickness * 0.5f),
                    size = Size(length, thickness),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(thickness * 0.22f),
                    alpha = alpha
                )
            }
        }
    }
}

/** Stable 0..1 value from an integer hash, so every recomposition has identical particle paths. */
private fun seededParticle(index: Int, seed: Int): Float {
    var x = index * 0x45d9f3b + seed * 0x119de1f3
    x = (x xor (x ushr 16)) * 0x45d9f3b
    x = (x xor (x ushr 16)) * 0x45d9f3b
    x = x xor (x ushr 16)
    return (abs(x % 10000) / 10000f)
}
