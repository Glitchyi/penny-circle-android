package com.spendingbuddy.app.ui

import androidx.compose.ui.platform.LocalContext
import com.spendingbuddy.app.R

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Small tinted capsule for the one fact that matters most right now (money left, or money over). */
@Composable
internal fun StatusPill(text: String, danger: Boolean, modifier: Modifier = Modifier) {
    val t = LocalPennyTokens.current
    val base = if (danger) t.danger else t.accent
    Text(
        text,
        modifier
            .background(base.copy(alpha = if (t.dark) 0.22f else 0.12f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = if (t.dark) lerp(base, t.text, 0.45f) else base,
        maxLines = 1
    )
}

/** Compact figure tile: a small label, one strong value, a quiet caption and an optional progress line. */
@Composable
internal fun StatTile(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    over: Boolean = false
) {
    val t = LocalPennyTokens.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (t.dark) t.surface2 else t.accent.copy(alpha = 0.06f))
            .border(1.dp, t.border.copy(alpha = 0.55f), shape)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.2.sp),
            color = t.muted,
            maxLines = 1
        )
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = if (over) t.danger else t.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(caption, style = MaterialTheme.typography.bodyMedium, color = t.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (progress != null) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(t.text.copy(alpha = 0.10f))) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            if (over) Brush.horizontalGradient(listOf(t.danger, t.danger))
                            else Brush.horizontalGradient(listOf(t.accent, t.accent)),
                            CircleShape
                        )
                )
            }
        }
    }
}

/**
 * Slide the handle to the right to finish the day in one move. A tap (or a screen reader's activate) opens the
 * review sheet instead, so nothing depends on being able to swipe. The handle springs back if let go early.
 */
@Composable
internal fun SwipeToFinish(onSwiped: () -> Unit, onTap: () -> Unit, haptics: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    val reduced = LocalReducedMotion.current
    val view = LocalView.current
    val reserved = with(LocalDensity.current) { (48.dp + 16.dp).toPx() }
    val scope = rememberCoroutineScope()
    val direction = LocalLayoutDirection.current
    val directionSign = if (direction == LayoutDirection.Ltr) 1f else -1f
    val motionMode = reduced to directionSign
    val latestMotionMode = rememberUpdatedState(motionMode)
    var offset by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    var completionTriggered by remember { mutableStateOf(false) }
    var travel by remember { mutableFloatStateOf(0f) }
    var highestHapticStep by remember { mutableStateOf(0) }
    val handle = 48.dp
    val inset = 6.dp
    val interaction = remember { MutableInteractionSource() }
    LaunchedEffect(motionMode) {
        settleJob?.cancel()
        settleJob = null
        // A setting/direction change cancels the gesture, never the already committed finish.
        offset = if (completionTriggered) travel else 0f
        highestHapticStep = 0
    }
    fun settleBack() {
        settleJob?.cancel()
        val target = if (completionTriggered) travel else 0f
        if (latestMotionMode.value != motionMode || latestMotionMode.value.first) {
            offset = target
            settleJob = null
            return
        }
        settleJob = scope.launch {
            val animation = Animatable(offset)
            animation.animateTo(target, PennyMotion.spatial()) { offset = value }
            offset = target
        }
    }
    fun completeSwipe() {
        if (latestMotionMode.value != motionMode) { settleBack(); return }
        if (travel > 0f && offset >= travel * 0.72f && !completionTriggered) {
            completionTriggered = true
            settleJob?.cancel()
            settleJob = scope.launch {
                val animation = Animatable(offset)
                if (reduced) animation.snapTo(travel)
                else animation.animateTo(travel, PennyMotion.spatial()) { offset = value }
                offset = animation.value
            }
            // A distinct confirmation pulse occurs only after an uncancelled release clears the threshold.
            if (haptics) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            onSwiped()
        } else settleBack()
    }
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .glass(CircleShape)
            .onSizeChanged { travel = (it.width - reserved).coerceAtLeast(0f) }
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClickLabel = context.getString(R.string.today_parts_review_and_finish_today), onClick = onTap)
            .semantics {
                contentDescription = context.getString(if (direction == LayoutDirection.Ltr) R.string.today_parts_swipe_right_description else R.string.today_parts_swipe_left_description)
            }
            .pointerInput(haptics, reduced, directionSign) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        settleJob?.cancel()
                        settleJob = null
                        val progress = if (travel > 0f) offset / travel else 0f
                        highestHapticStep = when {
                            progress >= 0.90f -> 4
                            progress >= 0.72f -> 3
                            progress >= 0.46f -> 2
                            progress >= 0.22f -> 1
                            else -> 0
                        }
                    },
                    onDragEnd = { completeSwipe() },
                    onDragCancel = { if (!completionTriggered) settleBack() },
                    onHorizontalDrag = { _, delta ->
                    val logicalDelta = delta * directionSign
                    val resistedDelta = if (logicalDelta > 0f && travel > 0f && offset >= travel * 0.82f) logicalDelta * 0.62f else logicalDelta
                    offset = (offset + resistedDelta).coerceIn(0f, travel)
                    val progress = if (travel > 0f) offset / travel else 0f
                    val step = when {
                        progress >= 0.90f -> 4
                        progress >= 0.72f -> 3
                        progress >= 0.46f -> 2
                        progress >= 0.22f -> 1
                        else -> 0
                    }
                    // Fire at most once per newly reached detent in this drag, even if the finger wavers backward.
                    if (step > highestHapticStep) {
                        highestHapticStep = step
                        if (haptics) view.performHapticFeedback(
                            when (step) {
                                4 -> HapticFeedbackConstants.LONG_PRESS
                                3 -> HapticFeedbackConstants.KEYBOARD_TAP
                                else -> HapticFeedbackConstants.CLOCK_TICK
                            }
                        )
                    }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // The part already swiped fills with the accent, and the label fades as the handle covers it.
        Box(
            Modifier.matchParentSize().drawBehind {
                val w = offset + (handle + inset * 2).toPx()
                if (offset > 0.5f) drawRoundRect(
                    Brush.horizontalGradient(listOf(t.accent.copy(alpha = 0.30f), t.accent.copy(alpha = 0.22f))),
                    topLeft = androidx.compose.ui.geometry.Offset(if (directionSign < 0f) size.width - w else 0f, 0f),
                    size = Size(w, size.height), cornerRadius = CornerRadius(size.height / 2f)
                )
            }
        )
        Text(
            context.getString(R.string.today_parts_swipe_to_finish_today),
            style = MaterialTheme.typography.titleMedium,
            color = t.text,
            maxLines = if (LocalDensity.current.fontScale > 1.15f) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(start = handle + inset * 2 + 8.dp, end = 20.dp, top = 4.dp, bottom = 4.dp)
                .graphicsLayer { alpha = if (travel <= 0f) 1f else (1f - offset / travel * 1.4f).coerceIn(0f, 1f) }
        )
        Box(
            Modifier
                .padding(start = inset)
                .align(Alignment.CenterStart)
                .offset { IntOffset(offset.roundToInt(), 0) }
                .size(handle)
                .background(Brush.linearGradient(listOf(t.accent, t.accent)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                PennyIcons.ArrowRight,
                null,
                tint = t.onAccent,
                modifier = Modifier.size(24.dp).graphicsLayer { scaleX = directionSign }
            )
        }
    }
}

/** Secondary action that closes out the day. Reads as a list cell so it does not compete with Add purchase. */
@Composable
internal fun FinishRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    val shape = RoundedCornerShape(22.dp)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .glass(shape)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 68.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .background(
                    Brush.linearGradient(listOf(t.accent.copy(alpha = 0.30f), t.accent.copy(alpha = 0.22f))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, null, tint = if (t.dark) t.text else t.accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(context.getString(R.string.today_parts_finish_today), style = MaterialTheme.typography.titleMedium, color = t.text)
            Text(
                context.getString(R.string.today_parts_close_out_the_day_and_keep),
                style = MaterialTheme.typography.bodyMedium,
                color = t.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(PennyIcons.ArrowRight, null, tint = t.muted)
    }
}
