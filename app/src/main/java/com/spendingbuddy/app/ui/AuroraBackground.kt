package com.spendingbuddy.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Soft, slowly drifting colour blobs over the theme background. Phase is read only inside the draw lambda. */
fun Modifier.auroraBackground(animate: Boolean = true): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val t = LocalPennyTokens.current
    val phase = if (animate && !reduced) {
        rememberInfiniteTransition(label = "aurora").animateFloat(
            0f,
            1f,
            infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Reverse),
            label = "p"
        )
    } else {
        null
    }
    drawBehind {
        val w = size.width
        val h = size.height
        val p = phase?.value ?: 0f

        // 1) Base colour.
        drawRect(t.bg)

        // 2) Three soft blobs.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(t.accent.copy(alpha = if (t.dark) 0.42f else 0.34f), Color.Transparent),
                center = Offset(w * (0.12f + 0.10f * p), h * (0.06f + 0.04f * p)),
                radius = w * 1.0f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(t.accent2.copy(alpha = if (t.dark) 0.28f else 0.26f), Color.Transparent),
                center = Offset(w * (0.98f - 0.10f * p), h * (0.30f + 0.06f * p)),
                radius = w * 0.85f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(t.accent3.copy(alpha = if (t.dark) 0.30f else 0.30f), Color.Transparent),
                center = Offset(w * (0.05f + 0.08f * p), h * (0.88f - 0.05f * p)),
                radius = w * 1.0f
            )
        )

        // 3) Gentle vignette so the bottom bar area stays calm.
        drawRect(
            brush = Brush.verticalGradient(
                0.55f to Color.Transparent,
                1f to t.bg.copy(alpha = 0.55f),
                startY = 0f,
                endY = h
            )
        )
    }
}
