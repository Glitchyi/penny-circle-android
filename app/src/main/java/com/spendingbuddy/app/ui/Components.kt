package com.spendingbuddy.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendingbuddy.app.*

@Composable
fun ScreenTitle(title: String, onBack: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun Rule(modifier: Modifier = Modifier) = HorizontalDivider(modifier, thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

/** Flat card: surface fill, 1dp hairline border, 12dp radius, no shadow. */
@Composable
fun NotebookSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(PennyShapes.Large).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PennyShapes.Large).padding(16.dp),
        content = content
    )
}

@Composable
fun Pill(text: String, selected: Boolean = false, tone: Color? = null, onClick: (() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    val bg = tone ?: if (selected) scheme.primaryContainer else scheme.surface
    val content = if (tone != null) scheme.onSurface else if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant
    val borderColor = if (selected) scheme.primary else scheme.outlineVariant
    if (onClick != null) AssistChip(
        onClick = onClick, label = { Text(text) }, shape = PennyShapes.Medium,
        colors = AssistChipDefaults.assistChipColors(containerColor = bg, labelColor = content),
        border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = borderColor)
    ) else Surface(shape = PennyShapes.Medium, color = bg, border = BorderStroke(1.dp, borderColor)) {
        Text(text, color = content, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
    }
}

/** Category label in its accent colours. The name is always shown as text. */
@Composable
fun CategoryChip(category: String, modifier: Modifier = Modifier) {
    val cc = CategoryColors.forCategory(category)
    Surface(modifier, shape = PennyShapes.Small, color = cc.container, border = BorderStroke(1.dp, cc.accent.copy(alpha = .35f))) {
        Text(category, color = cc.content, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun ProgressBar(value: Long, max: Long, over: Boolean = false, modifier: Modifier = Modifier) {
    val fraction = if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary))
    }
}

@Composable
fun PurchaseRow(expense: Expense, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val impulse = expense.reason == IMPULSE
    val cc = CategoryColors.forCategory(if (impulse) "Impulse" else expense.category)
    val base = modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
    Row((if (onClick != null) base.clickable(onClick = onClick) else base).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(42.dp).clip(RoundedCornerShape(2.dp)).background(if (impulse) MaterialTheme.colorScheme.error else Color.Transparent))
        Spacer(Modifier.width(12.dp))
        Surface(shape = PennyShapes.Medium, color = cc.container, modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(categoryIcon(expense.category), color = cc.content, style = MaterialTheme.typography.bodyLarge) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(expense.merchant.ifBlank { expense.category }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CategoryChip(expense.category)
                Text(if (impulse) "Impulse" else expense.reason, style = MaterialTheme.typography.bodyMedium, fontWeight = if (impulse) FontWeight.SemiBold else null, color = if (impulse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(rupees(expense.amountPaise), style = MaterialTheme.typography.titleMedium, color = if (impulse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

private fun categoryIcon(category: String) = when (category) {
    "Food" -> "🍴"
    "Transport" -> "🚌"
    "Shopping" -> "🛍"
    "Bills" -> "▤"
    else -> "•"
}

@Composable
fun EmptyState(onAdd: () -> Unit, onNoSpend: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp)) {
        Text("Nothing logged yet today.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onAdd) { Text("Add purchase") }
            OutlinedButton(onClick = onNoSpend) { Text("No purchases today") }
        }
    }
}

@Composable
fun AppNavBar(selected: Int, onSelect: (Int) -> Unit) {
    Column {
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        NavigationBarItem(selected == 0, { onSelect(0) }, icon = { Icon(Icons.Rounded.Today, null) }, label = { Text("Today") })
        NavigationBarItem(selected == 1, { onSelect(1) }, icon = { Icon(Icons.Rounded.BarChart, null) }, label = { Text("Activity") })
        NavigationBarItem(selected == 2, { onSelect(2) }, icon = { Icon(Icons.Rounded.People, null) }, label = { Text("Buddy") })
    }
    }
}

@Composable
fun ErrorText(message: String?) { if (!message.isNullOrBlank()) Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp)) }
