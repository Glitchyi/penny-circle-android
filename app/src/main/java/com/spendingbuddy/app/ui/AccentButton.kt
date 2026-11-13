package com.spendingbuddy.app.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp

/** Quiet frosted button for the second choice on a screen. Same height and press feel as [AccentButton]. */
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val t = LocalPennyTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(interactionSource)
            .glass(shape)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides if (enabled) t.text else ButtonDefaults.textButtonColors().disabledContentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Row(
                Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
            }
        }
    }
}

/** One-accent primary action with native focus/disabled states and an optional haptic tick. */
@Composable
fun AccentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    haptics: Boolean = true,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val t = LocalPennyTokens.current
    val reduced = LocalReducedMotion.current
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = ButtonDefaults.buttonColors(containerColor = t.accent, contentColor = t.onAccent)
    val fill = if (enabled) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    val pressScale = animateFloatAsState(
        if (pressed && enabled) 0.96f else 1f,
        if (reduced) snap() else PennyMotion.spatial(),
        label = "press"
    )
    LaunchedEffect(pressed, enabled, haptics) {
        if (pressed && enabled && haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
    Box(
        modifier
            .shadow(
                elevation = if (enabled) 18.dp else 0.dp,
                shape = shape,
                ambientColor = t.accent.copy(alpha = 0.35f),
                spotColor = t.accent.copy(alpha = 0.55f)
            )
            .graphicsLayer {
                scaleX = pressScale.value
                scaleY = pressScale.value
            }
            .clip(shape)
            .background(fill, shape)
            .then(if (focused && enabled) Modifier.border(OutlinedTextFieldDefaults.FocusedBorderThickness, t.onAccent, shape) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .defaultMinSize(minHeight = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Row(
                Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
            }
        }
    }
}
