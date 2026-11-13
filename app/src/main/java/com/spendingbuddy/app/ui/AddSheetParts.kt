package com.spendingbuddy.app.ui

import androidx.compose.ui.platform.LocalContext
import com.spendingbuddy.app.R

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendingbuddy.app.appendRotaryDecimal
import com.spendingbuddy.app.appendRotaryDigit
import com.spendingbuddy.app.removeRotaryLastCharacter

/** Capsule switch with a thumb that slides between the options. */
@Composable
internal fun SegmentedToggle(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalPennyTokens.current
    val reduced = LocalReducedMotion.current
    BoxWithConstraints(modifier.height(48.dp).clip(CircleShape).background(t.surface2).selectableGroup()) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected, if (reduced) snap() else PennyMotion.spatial(), label = "segment")
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(segment)
                .fillMaxHeight()
                .padding(4.dp)
                .background(t.surface, CircleShape)
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selected
                val color by pennyAnimatedColor(if (isSelected) t.text else t.muted, PennyMotion.effects(), label = "segmentText")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(index) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, color = color)
                }
            }
        }
    }
}

/** Two short bars that show which step of the sheet is open. */
@Composable
internal fun StepBars(step: Int, count: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    Row(
        modifier.clearAndSetSemantics { contentDescription = context.getString(R.string.amount_step, step + 1, count) },
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(count) { index ->
            val color by pennyAnimatedColor(if (index <= step) t.accent else t.text.copy(alpha = 0.14f), PennyMotion.effects(), label = "stepBar")
            Box(Modifier.size(width = 22.dp, height = 4.dp).drawBehind { drawRoundRect(color, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)) })
        }
    }
}

/** The amount being typed, centred. Each new character pops in, and the type shrinks as the number grows. */
@Composable
internal fun AmountDisplay(amount: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    val reduced = LocalReducedMotion.current
    val textMeasurer = rememberTextMeasurer()
    val textStyle = LocalTextStyle.current
    val currencyGapPx = with(LocalDensity.current) { 4.dp.roundToPx() }
    val shown = amount.ifBlank { "0" }
    val target = when {
        shown.length <= 5 -> 76f
        shown.length <= 7 -> 60f
        else -> 46f
    }
    BoxWithConstraints(
        modifier.fillMaxWidth().heightIn(min = 100.dp)
            .clearAndSetSemantics { contentDescription = if (amount.isBlank()) context.getString(R.string.amount_zero) else context.getString(R.string.amount_value, amount) },
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = constraints.maxWidth
        val fittedTarget = remember(shown, target, availableWidth, textStyle, textMeasurer, currencyGapPx) {
            // Match the separate Text nodes exactly, including per-glyph pixel rounding, the
            // currency marker and its gap. TextMeasurer uses the current device font scaling.
            fun widthAt(size: Float): Int {
                val currencyWidth = textMeasurer.measure(
                    "₹",
                    style = textStyle.copy(fontSize = (size * 0.5f).sp, fontWeight = FontWeight.Medium),
                    softWrap = false,
                    maxLines = 1
                ).size.width
                val digitStyle = textStyle.copy(
                    fontSize = size.sp,
                    lineHeight = size.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = PennyType.Mono,
                    letterSpacing = (-0.03f * size).sp
                )
                return currencyWidth + currencyGapPx + shown.sumOf { ch ->
                    textMeasurer.measure(ch.toString(), style = digitStyle, softWrap = false, maxLines = 1).size.width
                }
            }
            if (widthAt(target) <= availableWidth) target else {
                // These are fitting bounds, not a new typography scale or a smaller input cap.
                var fits = 0f
                var overflows = target
                repeat(16) {
                    val candidate = (fits + overflows) / 2f
                    if (widthAt(candidate) <= availableWidth) fits = candidate else overflows = candidate
                }
                fits
            }
        }
        val animatedSize by animateFloatAsState(fittedTarget, if (reduced) snap() else PennyMotion.spatial(), label = "amountSize")
        // Shrinking must fit immediately; expansion may keep its existing spring without overshoot.
        val size = animatedSize.coerceAtMost(fittedTarget)
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("₹", fontSize = (size * 0.5f).sp, fontWeight = FontWeight.Medium, color = t.muted, modifier = Modifier.padding(end = 4.dp))
            shown.forEachIndexed { index, ch ->
                key(index, ch) {
                    Text(
                        ch.toString(),
                        fontSize = size.sp,
                        lineHeight = size.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PennyType.Mono,
                        letterSpacing = (-0.03f * size).sp,
                        color = if (amount.isBlank()) t.muted else t.text,
                        modifier = Modifier.popIn()
                    )
                }
            }
        }
    }
}

/** Number pad. Stateless: the amount lives in the caller, so switching modes or going back keeps the input. */
@Composable
internal fun Keypad(amount: String, onAmount: (String) -> Unit, haptics: Boolean = true) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "⌫")).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { columnIndex, key ->
                    val label = when (key) { "⌫" -> context.getString(R.string.amount_delete_last_character); "." -> context.getString(R.string.amount_decimal_point); else -> context.getString(R.string.amount_digit, key) }
                    // The pad opens as a diagonal wave from the top-left key.
                    KeypadKey(key, label, haptics, Modifier.weight(1f).popIn(delayMillis = 40 + (rowIndex + columnIndex) * 38)) {
                        onAmount(
                            when (key) {
                                "⌫" -> removeRotaryLastCharacter(amount)
                                "." -> appendRotaryDecimal(amount)
                                else -> appendRotaryDigit(amount, key.single() - '0')
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(key: String, label: String, haptics: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val t = LocalPennyTokens.current
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val isDigit = key.single().isDigit()
    val fill by pennyAnimatedColor(
        when {
            pressed -> t.accent.copy(alpha = if (t.dark) 0.36f else 0.20f)
            isDigit -> t.surface2
            else -> Color.Transparent
        },
        PennyMotion.effects(),
        label = "keyFill"
    )
    Box(
        modifier
            .height(62.dp)
            .pressScale(interaction, pressed = 0.9f)
            .clip(RoundedCornerShape(20.dp))
            .drawBehind { drawRect(fill) }
            .then(if (focused) Modifier.border(OutlinedTextFieldDefaults.FocusedBorderThickness, t.text, RoundedCornerShape(20.dp)) else Modifier)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button) {
                if (haptics) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (key == "⌫") Icon(Icons.AutoMirrored.Rounded.Backspace, null, tint = t.text, modifier = Modifier.size(26.dp))
        else Text(key, style = MaterialTheme.typography.displayMedium, fontFamily = PennyType.Mono, color = t.text)
    }
}
