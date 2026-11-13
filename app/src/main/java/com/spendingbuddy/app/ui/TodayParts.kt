package com.spendingbuddy.app.ui

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
        color = if (t.dark) lerp(base, Color.White, 0.45f) else base,
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
            .background(if (t.dark) Color.White.copy(alpha = 0.06f) else t.accent.copy(alpha = 0.06f))
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
                            else Brush.horizontalGradient(listOf(t.accent, t.accent2)),
                            CircleShape
                        )
                )
            }
        }
    }
}

/** Secondary action that closes out the day. Reads as a list cell so it does not compete with Add purchase. */
@Composable
internal fun FinishRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                    Brush.linearGradient(listOf(t.accent.copy(alpha = 0.30f), t.accent2.copy(alpha = 0.22f))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, null, tint = if (t.dark) Color.White else t.accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Finish today", style = MaterialTheme.typography.titleMedium, color = t.text)
            Text(
                "Close out the day and keep your streak",
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
