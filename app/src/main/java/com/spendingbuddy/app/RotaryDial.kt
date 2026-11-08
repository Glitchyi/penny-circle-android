package com.spendingbuddy.app

import android.graphics.Paint
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

private const val ROTARY_HOLE_COUNT = 9
private const val ROTARY_START_ANGLE = 0.3141593f // 18 degrees: digit 1 starts just before the stop.
private const val ROTARY_HOLE_STEP = -0.5235988f // -30 degrees: each next hole needs 30 degrees more.
private const val ROTARY_STOP_ANGLE = 0.83775806f // 48 degrees: fixed stop at lower-right.
private const val ROTARY_TICK_ANGLE = 0.36651915f // 21 degrees; deliberately gentle, sparse ticks.

private val ForestGreen = Color(0xFF12392E)
private val Cream = Color(0xFFF7F4E9)
private val Lime = Color(0xFFD7F566)
private val MutedGreen = Color(0xFF47685D)

/**
 * Normalizes an angle to [0, 2PI). Kept pure so gesture boundary cases can be unit tested.
 */
internal fun normalizeRotaryAngle(angle: Float): Float {
    val fullTurn = (2.0 * PI).toFloat()
    var result = angle % fullTurn
    if (result < 0f) result += fullTurn
    return result
}

/** Clockwise distance in screen coordinates, where atan2's positive y points down. */
internal fun rotaryClockwiseDistance(from: Float, to: Float): Float =
    normalizeRotaryAngle(to - from)

/** Small signed movement from one pointer angle to the next, crossing atan2's seam safely. */
internal fun rotarySignedAngularDelta(from: Float, to: Float): Float {
    val fullTurn = (2.0 * PI).toFloat()
    var result = (to - from) % fullTurn
    if (result > PI.toFloat()) result -= fullTurn
    if (result < -PI.toFloat()) result += fullTurn
    return result
}

internal fun rotaryHoleAngle(digit: Int): Float =
    ROTARY_START_ANGLE + (digit.coerceIn(1, ROTARY_HOLE_COUNT) - 1) * ROTARY_HOLE_STEP

internal fun rotaryTargetRotation(digit: Int): Float =
    rotaryClockwiseDistance(rotaryHoleAngle(digit), ROTARY_STOP_ANGLE)

/** Appends one dial/keypad digit while enforcing the same two-paise limit as the UI keypad. */
internal fun appendRotaryDigit(current: String, digit: Int): String {
    if (digit !in 0..9) return current
    val clean = current.trim().removePrefix("₹")
    if (clean.contains('.')) {
        if (clean.substringAfter('.').length >= 2) return clean
        return clean + digit
    }
    if (clean == "0") return if (digit == 0) clean else digit.toString()
    return clean + digit
}

internal fun appendRotaryDecimal(current: String): String {
    val clean = current.trim().removePrefix("₹")
    return when {
        clean.contains('.') -> clean
        clean.isEmpty() -> "0."
        else -> "$clean."
    }
}

internal fun removeRotaryLastCharacter(current: String): String =
    current.trim().removePrefix("₹").dropLast(1)

private fun rotaryPoint(center: Offset, radius: Float, angle: Float): Offset =
    Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))

private fun rotaryHitDigit(point: Offset, center: Offset, radius: Float): Int? {
    val holeOrbit = radius * 0.655f
    val holeRadius = radius * 0.115f
    var nearest: Int? = null
    var nearestDistance = Float.MAX_VALUE
    for (digit in 1..ROTARY_HOLE_COUNT) {
        val hole = rotaryPoint(center, holeOrbit, rotaryHoleAngle(digit))
        val distance = hypot(point.x - hole.x, point.y - hole.y)
        if (distance <= holeRadius * 1.28f && distance < nearestDistance) {
            nearest = digit
            nearestDistance = distance
        }
    }
    return nearest
}

private fun shouldTapCenter(point: Offset, center: Offset, radius: Float): Boolean =
    hypot(point.x - center.x, point.y - center.y) <= radius * 0.235f

private fun performRotaryTick(view: View, enabled: Boolean) {
    // No FLAG_IGNORE_GLOBAL_SETTING: Android's global haptic preference remains authoritative.
    if (enabled && view.isHapticFeedbackEnabled) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

private suspend fun springRotaryHome(
    wheel: Animatable<Float, AnimationVector1D>,
    view: View,
    haptics: Boolean,
    start: Float
) {
    wheel.snapTo(start)
    var nextTick = ceil(abs(start) / ROTARY_TICK_ANGLE).toInt()
    wheel.animateTo(
        targetValue = 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) {
        val remaining = abs(value)
        while (nextTick > 0 && remaining <= (nextTick - 1) * ROTARY_TICK_ANGLE) {
            performRotaryTick(view, haptics)
            nextTick--
        }
    }
    wheel.snapTo(0f)
}

/**
 * A working old-telephone-style amount entry control. The dial commits only after a selected
 * finger hole reaches the fixed stop; early release returns home without changing the amount.
 */
@Composable
fun RotaryAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    haptics: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val wheel = remember { Animatable(0f) }
    var interactiveRotation by remember { mutableStateOf(0f) }
    var interactionLocked by remember { mutableStateOf(false) }
    var keypadVisible by rememberSaveable { mutableStateOf(false) }
    var undoValue by rememberSaveable { mutableStateOf<String?>(null) }
    val latestValue = rememberUpdatedState(value)
    val latestOnValueChange = rememberUpdatedState(onValueChange)
    val latestHaptics = rememberUpdatedState(haptics)

    fun commitValue(next: String) {
        if (next == latestValue.value) return
        undoValue = latestValue.value
        latestOnValueChange.value(next)
    }

    // Keep the callback holder stable for the pointer-input coroutine without restarting it on
    // every parent value update.
    val commitAction = rememberUpdatedState<(String) -> Unit> { next -> commitValue(next) }
    val undoAction: () -> Unit = {
        val previous = undoValue
        if (previous != null) {
            val current = latestValue.value
            if (previous != current) {
                undoValue = null
                latestOnValueChange.value(previous)
            }
        }
    }
    val clearAction = { commitValue("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MutedGreen
                )
                Text(
                    text = "₹${value.trim().removePrefix("₹").ifBlank { "0" }}",
                    style = MaterialTheme.typography.displaySmall,
                    color = ForestGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                TextButton(onClick = undoAction, enabled = undoValue != null) { Text("Undo") }
                TextButton(onClick = clearAction, enabled = value.isNotEmpty()) { Text("Clear") }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = "Touch a number hole, turn clockwise to the stop, then release",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGreen,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )
        Spacer(Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Input:", style = MaterialTheme.typography.labelLarge, color = MutedGreen)
            TextButton(
                onClick = { keypadVisible = false },
                enabled = keypadVisible,
                modifier = Modifier.semantics { contentDescription = "Use rotary dial input" }
            ) { Text("Dial") }
            TextButton(
                onClick = { keypadVisible = true },
                enabled = !keypadVisible,
                modifier = Modifier.semantics { contentDescription = "Use accessible keypad input" }
            ) { Text("Keypad") }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .aspectRatio(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .semantics {
                        contentDescription =
                            "Rotary amount dial. Select a numbered finger hole and turn clockwise to the fixed stop. Use the digit buttons below for an accessible alternative."
                    }
                    .pointerInput(interactionLocked) {
                        if (interactionLocked) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = minOf(size.width, size.height) / 2f
                            val selectedDigit = rotaryHitDigit(down.position, center, radius)
                            val centerCandidate = selectedDigit == null && shouldTapCenter(down.position, center, radius)
                            if (!centerCandidate && selectedDigit == null) return@awaitEachGesture

                            down.consume()
                            var lastAngle = atan2(down.position.y - center.y, down.position.x - center.x)
                            var rotation = 0f
                            var stillValid = true
                            var committed = false
                            val target: Float = selectedDigit?.let(::rotaryTargetRotation) ?: 0f
                            var lastTick = 0

                            while (stillValid && !committed) {
                                val event = awaitPointerEvent()
                                val change: PointerInputChange =
                                    event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.changedToUp()) {
                                    if (centerCandidate) {
                                        val distanceFromCenter = hypot(
                                            change.position.x - center.x,
                                            change.position.y - center.y
                                        )
                                        if (distanceFromCenter <= radius * 0.29f) {
                                            commitAction.value(appendRotaryDigit(latestValue.value, 0))
                                        }
                                    } else if (selectedDigit != null && rotation >= target - 0.035f) {
                                        // This is the sole commit point for a gesture.
                                        commitAction.value(appendRotaryDigit(latestValue.value, selectedDigit))
                                        committed = true
                                    }
                                    change.consume()
                                    break
                                }
                                if (!change.pressed) break

                                if (centerCandidate) {
                                    val movedAway = hypot(
                                        change.position.x - center.x,
                                        change.position.y - center.y
                                    ) > radius * 0.29f
                                    if (movedAway) stillValid = false
                                    change.consume()
                                    continue
                                }

                                val currentAngle = atan2(
                                    change.position.y - center.y,
                                    change.position.x - center.x
                                )
                                val delta: Float = rotarySignedAngularDelta(lastAngle, currentAngle)
                                val nextRotation: Float = (rotation + delta).coerceIn(0f, target)
                                if (nextRotation != rotation) {
                                    interactiveRotation = nextRotation
                                    val nextTick = floor(nextRotation / ROTARY_TICK_ANGLE).toInt()
                                    if (nextTick > lastTick) {
                                        performRotaryTick(view, latestHaptics.value)
                                        lastTick = nextTick
                                    }
                                    rotation = nextRotation
                                }
                                lastAngle = currentAngle
                                change.consume()
                            }

                            if (centerCandidate) {
                                interactiveRotation = 0f
                            }
                            if (selectedDigit != null && rotation > 0.001f) {
                                interactionLocked = true
                                interactiveRotation = 0f
                                val returnFrom = rotation
                                scope.launch {
                                    springRotaryHome(wheel, view, latestHaptics.value, returnFrom)
                                    interactionLocked = false
                                }
                            }
                        }
                    }
            ) {
                                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = minOf(size.width, size.height) / 2f
                val wheelRadius = radius * 0.89f
                val holeOrbit = radius * 0.655f
                val holeRadius = radius * 0.115f
                val wheelRotation = if (interactionLocked) wheel.value else interactiveRotation

                drawCircle(ForestGreen, wheelRadius, center)
                drawCircle(
                    color = Cream.copy(alpha = 0.12f),
                    radius = wheelRadius * 0.88f,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.012f)
                )
                drawCircle(
                    color = Cream.copy(alpha = 0.09f),
                    radius = wheelRadius * 0.74f,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.008f)
                )

                for (digit in 1..ROTARY_HOLE_COUNT) {
                    val angle = rotaryHoleAngle(digit) + wheelRotation
                    val hole = rotaryPoint(center, holeOrbit, angle)
                    drawCircle(Color.Black.copy(alpha = 0.18f), holeRadius * 1.08f, hole + Offset(0f, radius * 0.012f))
                    drawCircle(Cream, holeRadius, hole)
                }

                val centreRadius = radius * 0.235f
                drawCircle(Lime, centreRadius, center)
                drawCircle(
                    color = ForestGreen.copy(alpha = 0.25f),
                    radius = centreRadius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.012f)
                )

                val stop = rotaryPoint(center, wheelRadius * 1.02f, ROTARY_STOP_ANGLE)
                val stopBase = rotaryPoint(center, wheelRadius * 0.76f, ROTARY_STOP_ANGLE)
                drawLine(ForestGreen, stopBase, stop, strokeWidth = radius * 0.035f)
                drawCircle(Lime, radius * 0.067f, stop)

                drawIntoCanvas { canvas ->
                    val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = ForestGreen.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = radius * 0.105f
                    }
                    val numberOffset = -(numberPaint.ascent() + numberPaint.descent()) / 2f
                    for (digit in 1..ROTARY_HOLE_COUNT) {
                        val angle = rotaryHoleAngle(digit) + wheelRotation
                        val hole = rotaryPoint(center, holeOrbit, angle)
                        canvas.nativeCanvas.drawText(digit.toString(), hole.x, hole.y + numberOffset, numberPaint)
                    }
                    val zeroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = ForestGreen.toArgb()
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = radius * 0.14f
                    }
                    val zeroOffset = -(zeroPaint.ascent() + zeroPaint.descent()) / 2f
                    canvas.nativeCanvas.drawText("0", center.x, center.y + zeroOffset, zeroPaint)
                }
            }
        }

        if (keypadVisible) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Accessible digit keypad",
                style = MaterialTheme.typography.labelLarge,
                color = ForestGreen,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
            RotaryKeypad(
                onDigit = { digit -> commitValue(appendRotaryDigit(latestValue.value, digit)) },
                onDecimal = { commitValue(appendRotaryDecimal(latestValue.value)) },
                onBackspace = { commitValue(removeRotaryLastCharacter(latestValue.value)) }
            )
        }
    }
}

@Composable
private fun RotaryKeypad(
    onDigit: (Int) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
        rows.forEach { row ->
            RotaryKeyRow {
                row.forEach { label ->
                    RotaryKey(label, "Enter digit $label") { onDigit(label.single() - '0') }
                }
            }
        }
        RotaryKeyRow {
            RotaryKey(".", "Add decimal point", onDecimal)
            RotaryKey("0", "Enter digit 0") { onDigit(0) }
            RotaryKey("⌫", "Delete last amount character", onBackspace)
        }
    }
}

@Composable
private fun RotaryKeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
private fun RowScope.RotaryKey(label: String, description: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(44.dp).semantics {
            contentDescription = description
            role = Role.Button
        },
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = ForestGreen)
    }
}
