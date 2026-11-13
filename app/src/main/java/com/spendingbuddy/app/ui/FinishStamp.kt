package com.spendingbuddy.app.ui

import androidx.compose.ui.platform.LocalContext
import com.spendingbuddy.app.R

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.spendingbuddy.app.INDIA_ZONE
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private const val STAMP_REST_ROTATION = -4f

/**
 * "Done" stamp. When [animate] is true (the day was seen open and has just been finished) it thumps in once with a
 * scale + rotate + fade spring and a single haptic at impact (if [haptics]). The "played for date" key is saved, so
 * recomposition, rotation and returning to the screen do not replay it. With the system animator scale at 0 (reduced
 * motion) the final state shows immediately. The word "Done" is always visible text.
 */
@Composable
fun FinishStamp(date: String, finishedAt: Long?, animate: Boolean, haptics: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var playedFor by rememberSaveable { mutableStateOf("") }
    val reduced = LocalReducedMotion.current
    val shouldRun = animate && playedFor != date
    val play = shouldRun && !reduced
    val scale = remember { Animatable(if (play) 1.9f else 1f) }
    val rotation = remember { Animatable(if (play) -18f else STAMP_REST_ROTATION) }
    val alpha = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(date, animate, reduced) {
        if (reduced) {
            scale.snapTo(1f)
            rotation.snapTo(STAMP_REST_ROTATION)
            alpha.snapTo(1f)
            playedFor = date
            return@LaunchedEffect
        }
        if (!shouldRun) return@LaunchedEffect
        playedFor = date
        if (!play) { if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); return@LaunchedEffect }
        coroutineScope {
            launch { alpha.animateTo(1f, tween(140)) }
            launch { rotation.animateTo(STAMP_REST_ROTATION, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
            launch {
                // Impact: the first time the stamp reaches its resting size.
                snapshotFlow { scale.value }.first { it <= 1f }
                if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            scale.animateTo(1f, spring(0.45f, 380f))
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.fillMaxWidth()) {
        Text(
            context.getString(R.string.finish_stamp_done),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = MaterialTheme.typography.displaySmall.fontWeight,
            modifier = Modifier
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value; rotationZ = rotation.value; this.alpha = alpha.value }
                .semantics(mergeDescendants = true) { contentDescription = context.getString(R.string.finish_stamp_day_finished) }
                .background(MaterialTheme.colorScheme.tertiaryContainer, PennyShapes.Small)
                .border(PennyBorders.Hairline, MaterialTheme.colorScheme.tertiary, PennyShapes.Small)
                .padding(horizontal = 18.dp, vertical = 5.dp)
        )
        Text(
            if (finishedAt != null) context.getString(R.string.finish_stamp_finished_at, java.time.Instant.ofEpochMilli(finishedAt).atZone(INDIA_ZONE).format(DateTimeFormatter.ofPattern("h:mm a"))) else context.getString(R.string.finish_stamp_finished_today),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
