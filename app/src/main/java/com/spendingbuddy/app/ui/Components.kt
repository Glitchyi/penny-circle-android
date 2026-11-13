package com.spendingbuddy.app.ui

import androidx.compose.ui.platform.LocalContext
import com.spendingbuddy.app.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.spendingbuddy.app.*

@Composable
fun ScreenTitle(title: String, onBack: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    val context = LocalContext.current
    Row(Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = 18.dp).padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            val press = remember { MutableInteractionSource() }
            Box(
                Modifier.size(48.dp).pressScale(press, pressed = 0.9f).glass(CircleShape, shadow = false)
                    .clickable(interactionSource = press, indication = LocalIndication.current, role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, context.getString(R.string.component_back), Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
        }
        // Top-level screens get the large title; pushed screens a smaller one beside the back button.
        Text(
            title,
            style = if (onBack == null) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        action?.invoke()
    }
}

@Composable
fun Rule(modifier: Modifier = Modifier) = HorizontalDivider(modifier, thickness = PennyBorders.Hairline, color = MaterialTheme.colorScheme.outlineVariant)

/** Standard card: the same frosted panel as the Today screen, with 16dp padding. */
@Composable
fun NotebookSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.glass(RoundedCornerShape(24.dp)).padding(16.dp), content = content)
}

@Composable
fun Pill(text: String, selected: Boolean = false, tone: Color? = null, onClick: (() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    // A tone is used as the background only for non-selectable pills (e.g. the streak badge).
    val bg = if (tone != null && onClick == null) tone
        else if (selected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent
    val content = scheme.onSurface
    val borderStroke = if (selected) BorderStroke(PennyBorders.SelectedChip, scheme.primary) else BorderStroke(PennyBorders.Hairline, scheme.outlineVariant)
    // Selectable pills are capsules; the tick means the choice never relies on colour alone.
    if (onClick != null) FilterChip(
        selected = selected, onClick = onClick, label = { Text(text) }, shape = CircleShape,
        leadingIcon = if (selected) { { Icon(Icons.Rounded.Check, null, Modifier.size(18.dp)) } } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent, labelColor = content,
            selectedContainerColor = bg, selectedLabelColor = content, selectedLeadingIconColor = scheme.primary
        ),
        border = borderStroke
    ) else Surface(shape = CircleShape, color = bg, border = borderStroke) {
        Text(text, color = content, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
    }
}

/** Category label in its accent colours. The name is always shown as text. */
@Composable
fun CategoryChip(category: String, modifier: Modifier = Modifier) {
    val cc = CategoryColors.forCategory(category, LocalPennyTokens.current.dark)
    Surface(modifier, shape = PennyShapes.Small, color = cc.container, border = BorderStroke(1.dp, cc.accent.copy(alpha = .35f))) {
        Text(category, color = cc.content, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun ProgressBar(value: Long, max: Long, over: Boolean = false, modifier: Modifier = Modifier) {
    val fraction = if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f)
    val t = LocalPennyTokens.current
    Box(modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(t.text.copy(alpha = 0.10f))) {
        Box(
            Modifier.fillMaxWidth(fraction).fillMaxHeight().background(
                if (over) t.danger else t.accent,
                CircleShape
            )
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun PurchaseRow(expense: Expense, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, flat: Boolean = false) {
    val context = LocalContext.current
    val impulse = expense.reason == IMPULSE
    val cc = CategoryColors.forCategory(if (impulse) IMPULSE else expense.category, LocalPennyTokens.current.dark)
    val largeText = LocalDensity.current.fontScale >= 1.4f
    val base = modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).semantics(mergeDescendants = true) {}.then(if (flat) Modifier.padding(horizontal = 16.dp) else Modifier)
    BoxWithConstraints((if (onClick != null) base.clickable(onClick = onClick) else base).padding(vertical = 10.dp)) {
        val stacked = largeText || maxWidth < 260.dp
        Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(42.dp).clip(RoundedCornerShape(2.dp)).background(if (impulse) MaterialTheme.colorScheme.error else Color.Transparent))
        Spacer(Modifier.width(12.dp))
        Surface(shape = PennyShapes.Medium, color = cc.container, modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(categoryIconVector(expense.category), contentDescription = null, tint = cc.content, modifier = Modifier.size(20.dp)) }
        }
        Spacer(Modifier.width(10.dp))
        if (stacked) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(expense.merchant.ifBlank { expense.category }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(rupees(expense.amountPaise), style = MaterialTheme.typography.titleMedium, color = if (impulse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (expense.merchant.isNotBlank()) CategoryChip(expense.category)
                    if (expense.needsCategory) Text(context.getString(R.string.component_needs_category), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    else if (expense.source == com.spendingbuddy.app.SOURCE_AUTO) Text(context.getString(R.string.component_auto), style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (impulse) context.getString(R.string.component_impulse) else expense.reason, style = MaterialTheme.typography.bodyMedium, fontWeight = if (impulse) FontWeight.SemiBold else null, color = if (impulse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(Modifier.weight(1f)) {
                Text(expense.merchant.ifBlank { expense.category }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (expense.merchant.isNotBlank()) CategoryChip(expense.category)
                    if (expense.needsCategory) Text(context.getString(R.string.component_needs_category), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    else if (expense.source == com.spendingbuddy.app.SOURCE_AUTO) Text(context.getString(R.string.component_auto), style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text(if (impulse) context.getString(R.string.component_impulse) else expense.reason, style = MaterialTheme.typography.bodyMedium, fontWeight = if (impulse) FontWeight.SemiBold else null, color = if (impulse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(rupees(expense.amountPaise), style = MaterialTheme.typography.titleMedium, color = if (impulse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier, watching: Boolean = false) {
    val context = LocalContext.current
    // Quiet on purpose: Add purchase and Finish today (which offers the no-spend day) are the actions.
    val t = LocalPennyTokens.current
    val tint = if (t.dark) lerp(t.accent, t.text, 0.45f) else t.accent
    Row(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(t.accent.copy(alpha = if (t.dark) 0.20f else 0.10f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ReceiptLong, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (watching) context.getString(R.string.component_watching_for_payments) else context.getString(R.string.component_nothing_logged_yet), style = MaterialTheme.typography.titleMedium)
            Text(
                if (watching) context.getString(R.string.component_purchases_show_up_here_when_a)
                else context.getString(R.string.component_no_spending_today_finish_the_day),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorText(message: String?) {
    if (!message.isNullOrBlank()) Text(
        message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 8.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            error(message)
        }
    )
}

@Composable
internal fun RequiredFieldsHint() {
    val context = LocalContext.current
    Text(
        context.getString(R.string.component_required_fields_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
