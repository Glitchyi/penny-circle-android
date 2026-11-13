@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.spendingbuddy.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.spendingbuddy.app.*

/**
 * Personal presets: quick-fills for the purchase form. They stay on this phone and are never shared with the circle.
 * [onSave] gets (id or null for new, name, category, reason) and returns an error message or null.
 */
@Composable
fun PresetsScreen(
    state: BuddyState,
    onBack: () -> Unit,
    onSave: (id: String?, name: String, category: String, reason: String) -> String?,
    onSetDefault: (String?) -> Unit,
    onDelete: (String) -> Unit,
    snackbar: SnackbarHostState? = null
) {
    // editing: null = closed, "" = new, otherwise preset id
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        topBar = { ScreenTitle("Presets", onBack) },
        floatingActionButton = {
            if (state.presets.size < MAX_PRESETS) ExtendedFloatingActionButton(onClick = { editing = "" }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("New preset") })
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).scrollFadingEdges(listState), state = listState, contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Only on this phone. Presets fill in the category and reason when you add a purchase.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (state.presets.isEmpty()) item {
                NotebookSurface(Modifier.fillMaxWidth()) {
                    Text("No presets yet", style = MaterialTheme.typography.titleMedium)
                    Text("Add one for a place you often spend at, or turn on Save as preset when you log a purchase.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(state.presets, key = { it.id }) { p ->
                PresetRow(p, onEdit = { editing = p.id }, onDelete = { confirmDelete = p.id }, onDefault = { onSetDefault(if (p.isDefault) null else p.id) })
            }
        }
    }
    confirmDelete?.let { id ->
        val p = state.presets.firstOrNull { it.id == id }
        if (p == null) { confirmDelete = null } else AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete \"${p.name}\"?") },
            text = { Text("Purchases you already logged are not changed. You can undo right after.") },
            confirmButton = { TextButton(onClick = { confirmDelete = null; onDelete(id) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Keep") } }
        )
    }
    editing?.let { id ->
        val existing = state.presets.firstOrNull { it.id == id }
        if (id.isNotEmpty() && existing == null) { editing = null }
        else PresetEditSheet(state, existing, onDismiss = { editing = null }, onSave = { n, c, r -> onSave(existing?.id, n, c, r).also { if (it == null) editing = null } })
    }
}

@Composable
private fun PresetRow(p: Preset, onEdit: () -> Unit, onDelete: () -> Unit, onDefault: () -> Unit) {
    val impulse = p.reason == IMPULSE
    NotebookSurface(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = "${p.name}, ${p.category}, ${if (impulse) "Impulse" else p.reason}${if (p.isDefault) ", default" else ""}"
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryChip(p.category)
                    Text(p.reason, style = MaterialTheme.typography.bodyMedium, color = if (impulse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (p.isDefault) Text("Default", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onEdit, Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Icon(Icons.Rounded.Edit, "Edit ${p.name}") }
            IconButton(onClick = onDelete, Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Icon(Icons.Rounded.Delete, "Delete ${p.name}") }
        }
        TextButton(onClick = onDefault, Modifier.heightIn(min = 48.dp).clearAndSetSemantics { contentDescription = if (p.isDefault) "Stop using ${p.name} as the default" else "Use ${p.name} as the default" }) {
            Text(if (p.isDefault) "Remove as default" else "Make default")
        }
    }
}

@Composable
private fun PresetEditSheet(state: BuddyState, existing: Preset?, onDismiss: () -> Unit, onSave: (String, String, String) -> String?) {
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: state.lastCategory) }
    var reason by rememberSaveable { mutableStateOf(existing?.reason ?: state.reasons.firstOrNull() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        DialogSystemBars()
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().imePadding()) {
            Text(if (existing == null) "New preset" else "Edit preset", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(name, { name = it.take(MAX_PRESET_NAME); error = null }, Modifier.fillMaxWidth(), label = { Text("Merchant or what for") }, singleLine = true)
            Spacer(Modifier.height(8.dp)); Text("Category", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { state.categories.forEach { Pill(it, it == category, onClick = { category = it }) } }
            Spacer(Modifier.height(8.dp)); Text("Why did you buy it?", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { state.reasons.forEach { Pill(it, it == reason, onClick = { reason = it }) } }
            if (reason == IMPULSE) Text("Impulse", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
            ErrorText(error)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { error = onSave(name, category, reason) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save preset") }
            Spacer(Modifier.height(18.dp))
        }
    }
}
