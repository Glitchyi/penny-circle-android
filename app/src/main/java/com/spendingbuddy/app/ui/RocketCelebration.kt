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

    if (play && !reducedMotion) {
        Canvas(modifier.fillMaxSize()) {
            drawCelebration(progress.value, tokens)
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

private fun DrawScope.drawTrail(t: Float, w: Float, h: Float, len: Float, dens: Float, tokens: PennyTokens) {
    val rMin = 7.dp.toPx()
    val rMax = 22.dp.toPx()
    // Dense, overlapping particles so the trail reads as a plume, not a string of beads.
    for (i in 0 until 76) {
        val b = t - i * 0.0052f
        if (b < 0f) continue
        val age = (t - b) / 0.4f
        if (age >= 1f) continue
        val drift = (((i * 37) % 11) - 5) * 3f * (t - b) * dens
        val cx = rocketX(b, w) + drift
        val cy = rocketY(b, h, len) + len * 0.45f
        val radius = (rMin + (rMax - rMin) * age) * (0.85f + ((i * 13) % 5) / 12f)
        if (i < 24) {
            drawCircle(
                color = lerp(Color(0xFFFFE08A), Color(0xFFFF6A00), age),
                radius = radius,
                center = Offset(cx, cy),
                alpha = (1f - age) * 0.9f
            )
        } else {
            drawCircle(
                color = tokens.muted.copy(alpha = 0.35f * (1f - age)),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}

private fun DrawScope.drawRocket(t: Float, w: Float, h: Float, len: Float, tokens: PennyTokens) {
    val cx = rocketX(t, w)
    val cy = rocketY(t, h, len)
    val w2 = 20.dp.toPx()
    val portholeR = 8.dp.toPx()
    val flameLen = len * (0.45f + 0.15f * sin(t * 90f))
    val flameTop = len * 0.5f

    translate(left = cx, top = cy) {
        // Flame under the rocket (drawn first so the body overlaps its top)
        val flame = Path().apply {
            moveTo(-9.dp.toPx(), flameTop)
            cubicTo(-9.dp.toPx(), flameTop + flameLen * 0.5f, -2.dp.toPx(), flameTop + flameLen * 0.7f, 0f, flameTop + flameLen)
            cubicTo(2.dp.toPx(), flameTop + flameLen * 0.7f, 9.dp.toPx(), flameTop + flameLen * 0.5f, 9.dp.toPx(), flameTop)
            close()
        }
        drawPath(
            path = flame,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFE08A), Color(0xFFFF6A00), Color.Transparent),
                startY = flameTop,
                endY = flameTop + flameLen
            )
        )

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
        drawPath(path = body, color = tokens.text)

        // Nose cone: top 30% of the body
        val noseBottom = -len / 2f + len * 0.3f
        val nose = Path().apply {
            moveTo(0f, -len / 2f)
            cubicTo(w2 * 0.6f, -len / 2f, w2 * 0.9f, -len * 0.35f, w2 * 0.95f, noseBottom)
            lineTo(-w2 * 0.95f, noseBottom)
            cubicTo(-w2 * 0.9f, -len * 0.35f, -w2 * 0.6f, -len / 2f, 0f, -len / 2f)
            close()
        }
        drawPath(path = nose, color = tokens.accent)

        // Fins
        val leftFin = Path().apply {
            moveTo(-w2, len * 0.05f)
            lineTo(-w2 - 10.dp.toPx(), len * 0.35f)
            lineTo(-w2, len * 0.3f)
            close()
        }
        val rightFin = Path().apply {
            moveTo(w2, len * 0.05f)
            lineTo(w2 + 10.dp.toPx(), len * 0.35f)
            lineTo(w2, len * 0.3f)
            close()
        }
        drawPath(path = leftFin, color = tokens.accent)
        drawPath(path = rightFin, color = tokens.accent)

        // Porthole
        val portholeCentre = Offset(0f, -len * 0.08f)
        drawCircle(color = tokens.bg, radius = portholeR, center = portholeCentre)
        drawCircle(
            color = tokens.accent,
            radius = portholeR,
            center = portholeCentre,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun DrawScope.drawStarburst(t: Float, w: Float, h: Float, tokens: PennyTokens) {
    val bt = (t - ROCKET_BURST_T) / 0.38f
    if (bt !in 0f..1f) return

    val cx = w / 2f
    val cy = h * 0.32f
    val centre = Offset(cx, cy)

    // Shockwave ring
    drawCircle(
        color = tokens.accent.copy(alpha = (1f - bt) * 0.8f),
        radius = bt * w * 0.55f,
        center = centre,
        style = Stroke(width = 3.dp.toPx())
    )

    val colors = listOf(tokens.accent, tokens.text, Color(0xFFFFD66B), tokens.muted)
    val twoPi = (2.0 * PI).toFloat()
    for (i in 0 until 44) {
        val angle = i * (twoPi / 44f) + (i % 3) * 0.12f
        val speed = (0.35f + ((i * 53) % 10) / 10f * 0.65f) * w * 0.55f
        val pos = Offset(
            cx + cos(angle) * speed * bt,
            cy + sin(angle) * speed * bt + 0.5f * w * 0.35f * bt * bt
        )
        val colour = colors[i % colors.size]
        val s = (4 + i % 4).dp.toPx()
        if (i % 2 == 0) {
            drawCircle(color = colour, radius = s / 2f, center = pos, alpha = 1f - bt)
        } else {
            rotate(degrees = i * 47f + bt * 360f, pivot = pos) {
                drawRect(
                    color = colour,
                    topLeft = Offset(pos.x - s / 2f, pos.y - s / 4f),
                    size = Size(s, s / 2f),
                    alpha = 1f - bt
                )
            }
        }
    }

    // Faint screen flash at the burst
    if (bt <= 0.25f) {
        drawRect(color = tokens.accent, alpha = 0.10f * (1f - bt))
    }
}
