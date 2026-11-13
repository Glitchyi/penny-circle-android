package com.spendingbuddy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/** Frosted-glass surface: soft shadow, translucent fill, top sheen and a light edge. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp), strength: Float = 1f): Modifier = composed {
    val t = LocalPennyTokens.current
    val fill = if (t.dark) Color.White.copy(alpha = 0.07f * strength) else Color.White.copy(alpha = 0.58f * strength)
    val edge = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (t.dark) 0.30f else 0.95f),
            Color.White.copy(alpha = if (t.dark) 0.05f else 0.25f)
        )
    )
    val sheenColor = Color.White.copy(alpha = if (t.dark) 0.10f else 0.35f)
    // No elevation shadow here on purpose: Android's shadow bleeds through a translucent fill and
    // shows up as a dark rectangle inside the panel.
    this
        .clip(shape)
        .background(fill, shape)
        .drawBehind {
            // Light catching the top of the glass.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(sheenColor, Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.45f
                )
            )
        }
        .border(1.dp, edge, shape)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.glass(shape), content = content)
}
