package com.spendingbuddy.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle

/** Odometer-style amount in Indian grouping. Digits slide in place; TalkBack reads the whole amount once. */
@Composable
fun RollingAmount(
    rupees: Long,
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color = LocalContentColor.current,
    brush: Brush? = null
) {
    val formatted = remember(rupees) { formatIndian(rupees) }
    val reduced = LocalReducedMotion.current
    // Previous amount, kept outside Compose state; updated after each composition.
    val prev = remember { LongArray(1).also { it[0] = rupees } }
    // One direction for the whole number, decided from the values (so 199 -> 200 slides up everywhere).
    val up = isCountingUp(prev[0], rupees)
    SideEffect { prev[0] = rupees }
    // Reversed so that each digit is keyed by its position from the right.
    val chars = remember(formatted) { formatted.reversed() }
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "₹$formatted" },
        verticalAlignment = Alignment.Top
    ) {
        chars.indices.reversed().forEach { i ->
            key(i) {
                val ch = chars[i]
                if (ch.isDigit()) {
                    RollingDigit(digit = ch, up = up, style = style, color = color, reduced = reduced, brush = brush)
                } else {
                    Text(
                        text = ch.toString(),
                        style = if (brush != null) style.copy(brush = brush) else style,
                        color = color,
                        modifier = Modifier.clearAndSetSemantics {}
                    )
                }
            }
        }
    }
}

@Composable
private fun RollingDigit(digit: Char, up: Boolean, style: TextStyle, color: Color, reduced: Boolean, brush: Brush?) {
    // Increasing amount slides digits up; decreasing amount slides them down.
    AnimatedContent(
        targetState = digit,
        modifier = Modifier.clearAndSetSemantics {},
        transitionSpec = {
            if (reduced) {
                EnterTransition.None togetherWith ExitTransition.None
            } else if (up) {
                (slideInVertically(spring(dampingRatio = 0.75f, stiffness = 300f)) { it } + fadeIn(tween(200))) togetherWith
                    (slideOutVertically(tween(260)) { -it } + fadeOut(tween(200)))
            } else {
                (slideInVertically(spring(dampingRatio = 0.75f, stiffness = 300f)) { -it } + fadeIn(tween(200))) togetherWith
                    (slideOutVertically(tween(260)) { it } + fadeOut(tween(200)))
            }
        },
        label = "digit"
    ) { d ->
        Text(text = d.toString(), style = if (brush != null) style.copy(brush = brush) else style, color = color)
    }
}

/** Formats with Indian grouping (last 3 digits, then groups of 2), independent of the device locale. */
internal fun formatIndian(rupees: Long): String {
    // Working on the string avoids overflow for Long.MIN_VALUE.
    val raw = rupees.toString()
    val negative = raw.startsWith("-")
    val digits = if (negative) raw.substring(1) else raw
    val grouped = if (digits.length <= 3) {
        digits
    } else {
        val head = digits.dropLast(3)
        val groups = head.reversed().chunked(2).map { it.reversed() }.reversed()
        groups.joinToString(",") + "," + digits.takeLast(3)
    }
    return if (negative) "-$grouped" else grouped
}

internal fun isCountingUp(previous: Long, current: Long): Boolean = current >= previous
