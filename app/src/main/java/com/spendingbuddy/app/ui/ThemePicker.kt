package com.spendingbuddy.app.ui

import androidx.compose.ui.platform.LocalContext
import com.spendingbuddy.app.R

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import com.spendingbuddy.app.rupees

internal fun Context.themeLabel(theme: AppTheme): String = getString(when (theme) {
    AppTheme.LightHorizon -> R.string.theme_picker_light_horizon
    AppTheme.VioletNight -> R.string.theme_picker_violet_night
    AppTheme.MintNight -> R.string.theme_picker_mint_night
    AppTheme.SignalBlue -> R.string.theme_picker_signal_blue
})

internal fun Context.themeMood(theme: AppTheme): String = getString(when (theme) {
    AppTheme.LightHorizon -> R.string.theme_picker_light_mood
    AppTheme.VioletNight -> R.string.theme_picker_violet_mood
    AppTheme.MintNight -> R.string.theme_picker_mint_mood
    AppTheme.SignalBlue -> R.string.theme_picker_blue_mood
})

/** Two-column grid of theme cards. Each card previews its own theme, not the current one. */
@Composable
fun ThemePicker(selectedId: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier, haptics: Boolean = true) {
    val selectedTheme = AppTheme.fromId(selectedId)
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppTheme.values().toList().chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { theme ->
                    ThemeCard(
                        theme = theme,
                        selected = theme == selectedTheme,
                        onClick = { onSelect(theme.id) },
                        haptics = haptics,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit,
    haptics: Boolean,
    modifier: Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val shape = RoundedCornerShape(14.dp)
    val reduceMotion = LocalReducedMotion.current
    val scale = remember { Animatable(1f) }
    var previousSelected by remember { mutableStateOf(selected) }
    // One-off pop when the card becomes selected; nothing animates on first composition.
    LaunchedEffect(selected, reduceMotion) {
        val becameSelected = selected && !previousSelected
        previousSelected = selected
        if (becameSelected && !reduceMotion) {
            scale.snapTo(0.96f)
            scale.animateTo(1f, PennyMotion.spatial())
        } else scale.snapTo(1f)
    }
    val borderColor by pennyAnimatedColor(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = if (reduceMotion) snap() else PennyMotion.effects(),
        label = "themeCardBorder"
    )
    val borderWidth by animateDpAsState(
        if (selected) 2.dp else 1.dp,
        animationSpec = if (reduceMotion) snap() else PennyMotion.spatial(),
        label = "themeCardBorderWidth"
    )

    Column(
        modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(borderWidth, borderColor, shape)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = {
                    if (!selected && haptics) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onClick()
                }
            )
            .semantics { contentDescription = context.getString(R.string.theme_picker_description, context.themeLabel(theme), context.themeMood(theme)) }
            .padding(10.dp)
    ) {
        ThemePreview(theme)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                context.themeLabel(theme),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (selected) {
                Box(
                    Modifier
                        .padding(start = 6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        null,
                        Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        Text(
            context.themeMood(theme),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Miniature of [theme] drawn from its own tokens. */
@Composable
private fun ThemePreview(theme: AppTheme) {
    val t = theme.tokens()
    val shape = RoundedCornerShape(10.dp)
    val horizonShape = RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp)
    val rowShape = RoundedCornerShape(3.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(shape)
            .background(t.bg)
            .border(1.dp, t.border, shape)
    ) {
        Column(Modifier.padding(start = 10.dp, top = 10.dp)) {
            Text(
                rupees(25_000L),
                fontFamily = PennyType.Mono,
                style = MaterialTheme.typography.headlineSmall,
                color = t.text
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.45f)
                    .height(3.dp)
                    .clip(rowShape)
                    .background(t.accent)
            )
        }
        Column(
            Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(2) {
                Box(
                    Modifier
                        .size(28.dp, 6.dp)
                        .clip(rowShape)
                        .background(t.surface2)
                        .border(1.dp, t.border, rowShape)
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(16.dp)
                .clip(horizonShape)
                .background(t.accent.copy(alpha = 0.12f))
                .border(1.dp, t.accent, horizonShape)
        )
    }
}
