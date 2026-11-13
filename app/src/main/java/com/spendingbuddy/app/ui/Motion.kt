package com.spendingbuddy.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Physical press feedback: the surface sinks while it is held and springs back with a small bounce.
 * A quick tap still dips fully before it bounces, so every touch is felt. Put this before the clip and
 * background, and give the same [interaction] to the clickable.
 */
fun Modifier.pressScale(interaction: InteractionSource, pressed: Float = 0.965f): Modifier = composed {
    val reduced = LocalReducedMotion.current
    if (reduced) return@composed this
    val scale = remember { Animatable(1f) }
    LaunchedEffect(interaction) {
        interaction.interactions.collectLatest { event ->
            when (event) {
                is PressInteraction.Press -> scale.animateTo(pressed, spring(dampingRatio = 1f, stiffness = 1400f))
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    if (scale.value > (1f + pressed) / 2f) scale.animateTo(pressed, spring(dampingRatio = 1f, stiffness = 2600f))
                    scale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
                }
            }
        }
    }
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** A short scale kick whenever [key] changes (not on first show), for a number that has just been updated. */
fun Modifier.bumpOnChange(key: Any?, amount: Float = 0.07f): Modifier = composed {
    val reduced = LocalReducedMotion.current
    if (reduced) return@composed this
    val scale = remember { Animatable(1f) }
    val last = remember { arrayOf(key) }
    LaunchedEffect(key) {
        if (key != last[0]) {
            last[0] = key
            scale.snapTo(1f + amount)
            scale.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f))
        }
    }
    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * A new element pops up from just below its place with a small bounce (typed digits, keys, badges).
 * [delayMillis] staggers a group so that it opens like a wave.
 */
fun Modifier.popIn(delayMillis: Int = 0): Modifier = composed {
    val reduced = LocalReducedMotion.current
    if (reduced) return@composed this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        progress.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 620f))
    }
    graphicsLayer {
        val p = progress.value
        alpha = (p * 2f).coerceIn(0f, 1f)
        val s = 0.5f + 0.5f * p
        scaleX = s
        scaleY = s
        translationY = (1f - p) * 14.dp.toPx()
    }
}

/**
 * One-time, short settling motion. Keep content legible throughout pane navigation.
 * It plays once per screen visit and is remembered across scrolling and rotation.
 */
fun Modifier.enterRise(order: Int = 0): Modifier = composed {
    val reduced = LocalReducedMotion.current
    if (reduced) return@composed this
    var entered by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (entered || reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(30L * order.coerceIn(0, 2))
            progress.animateTo(1f, tween(durationMillis = 240))
        }
        entered = true
    }
    graphicsLayer {
        val p = progress.value
        translationY = (1f - p) * 12.dp.toPx()
    }
}

/** A quiet one-time content fade for changing details that do not need to move or scale. */
fun Modifier.fadeInContent(delayMillis: Int = 0): Modifier = composed {
    if (LocalReducedMotion.current) return@composed this
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        alpha.animateTo(1f, tween(durationMillis = 180))
    }
    graphicsLayer { this.alpha = alpha.value }
}
