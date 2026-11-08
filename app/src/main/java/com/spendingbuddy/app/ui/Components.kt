package com.spendingbuddy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
fun Rule(modifier: Modifier = Modifier) = HorizontalDivider(modifier, color = MaterialTheme.colorScheme.outline.copy(alpha = .7f))

@Composable
fun NotebookSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surface).padding(20.dp), content = content)
}

@Composable
fun Pill(text: String, selected: Boolean = false, tone: Color? = null, onClick: (() -> Unit)? = null) {
    val bg = tone ?: if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .12f) else MaterialTheme.colorScheme.surface
    val content = tone?.let { MaterialTheme.colorScheme.onSurface } ?: if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val m = Modifier.clip(CircleShape).border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
    if (onClick != null) AssistChip(onClick = onClick, label = { Text(text) }, colors = AssistChipDefaults.assistChipColors(containerColor = bg, labelColor = content), border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline))
    else Surface(modifier = m.background(bg), color = Color.Transparent) { Text(text, color = content, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) }
}

@Composable
fun ProgressBar(value: Long, max: Long, over: Boolean = false, modifier: Modifier = Modifier) {
    val fraction = if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f)
    Box(modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary))
    }
}

@Composable
fun PurchaseRow(expense: Expense, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val impulse = expense.reason == IMPULSE
    Row(modifier.fillMaxWidth().then(if (onClick != null) Modifier else Modifier).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(42.dp).clip(RoundedCornerShape(2.dp)).background(if (impulse) MaterialTheme.colorScheme.error else Color.Transparent))
        Spacer(Modifier.width(12.dp))
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .1f), modifier = Modifier.size(34.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(categoryIcon(expense.category), style = MaterialTheme.typography.bodyLarge) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(expense.merchant.ifBlank { expense.category }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(if (impulse) "Impulse" else expense.reason, style = MaterialTheme.typography.bodyMedium, color = if (impulse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
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
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(selected == 0, { onSelect(0) }, icon = { Icon(Icons.Rounded.Today, "Today") }, label = { Text("Today") })
        NavigationBarItem(selected == 1, { onSelect(1) }, icon = { Icon(Icons.Rounded.BarChart, "Activity") }, label = { Text("Activity") })
        NavigationBarItem(selected == 2, { onSelect(2) }, icon = { Icon(Icons.Rounded.People, "Buddy") }, label = { Text("Buddy") })
    }
}

@Composable
fun ErrorText(message: String?) { if (!message.isNullOrBlank()) Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp)) }
