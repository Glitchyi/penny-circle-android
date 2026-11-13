package com.spendingbuddy.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Pressable button with a hard, unblurred offset shadow. The face sinks onto the shadow while pressed.
 * The shadow is drawn behind the padded box, so it matches the face size exactly.
 */
@Composable
fun HardShadowButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    shadowColor: Color,
    shape: Shape = RoundedCornerShape(12.dp),
    shadowOffset: Dp = 5.dp,
    enabled: Boolean = true,
    haptics: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduced = LocalReducedMotion.current
    val pressOffset by animateDpAsState(
        targetValue = if (pressed) shadowOffset else 0.dp,
        animationSpec = if (reduced) snap() else spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "press"
    )
    val view = LocalView.current
    LaunchedEffect(pressed, haptics) {
        if (pressed && haptics) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    Box(
        modifier
            // drawBehind goes BEFORE padding so `size` includes the padding strip the shadow lives in.
            .drawBehind {
                val sp = shadowOffset.toPx()
                val w = size.width - sp
                val h = size.height - sp
                translate(sp, sp) {
                    when (val outline = shape.createOutline(Size(w, h), layoutDirection, this)) {
                        is Outline.Rectangle -> drawRect(shadowColor, size = Size(w, h))
                        is Outline.Rounded -> drawPath(Path().apply { addRoundRect(outline.roundRect) }, shadowColor)
                        is Outline.Generic -> drawPath(outline.path, shadowColor)
                    }
                }
            }
            .padding(end = shadowOffset, bottom = shadowOffset)
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            interactionSource = interactionSource,
            modifier = Modifier
                // Lambda form defers the offset read to layout, avoiding a re-measure every frame.
                .offset { IntOffset(pressOffset.roundToPx(), pressOffset.roundToPx()) }
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
        ) {
            // The Row wraps its content; the Box supplies the full width so the content stays centred.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}
