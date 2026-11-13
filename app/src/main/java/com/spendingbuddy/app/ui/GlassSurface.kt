package com.spendingbuddy.app.ui

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard flat panel from the selected theme's surface/border roles.
 * Navigation may explicitly opt into its separately approved soft depth treatment.
 */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp), strength: Float = 1f, shadow: Boolean = false): Modifier = composed {
    val t = LocalPennyTokens.current
    val shadowColor = lerp(t.text, t.accent, 0.4f).copy(alpha = 0.07f)
    this
        .then(if (shadow && !t.dark) Modifier.softShadow(shape, shadowColor, blur = 14.dp, offsetY = 6.dp) else Modifier)
        .clip(shape)
        .background(t.surface.copy(alpha = strength.coerceIn(0f, 1f)), shape)
        .border(PennyBorders.Hairline, t.border, shape)
}

/**
 * Blurred drop shadow drawn only outside [shape]. Modifier.shadow cannot be used under a translucent fill:
 * it shows through as a dark block inside the panel.
 */
private fun Modifier.softShadow(shape: Shape, color: Color, blur: Dp = 22.dp, offsetY: Dp = 10.dp): Modifier = drawWithCache {
    val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
    val paint = Paint().apply {
        this.color = color
        asFrameworkPaint().maskFilter = BlurMaskFilter(blur.toPx(), BlurMaskFilter.Blur.NORMAL)
    }
    val dy = offsetY.toPx()
    onDrawBehind {
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.clipPath(path, ClipOp.Difference)
            canvas.translate(0f, dy)
            canvas.drawPath(path, paint)
            canvas.restore()
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.glass(shape), content = content)
}
