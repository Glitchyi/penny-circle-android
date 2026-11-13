@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.spendingbuddy.app.ui

import androidx.compose.ui.platform.LocalContext
import com.spendingbuddy.app.R

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.error as semanticsError
import androidx.compose.ui.unit.dp
import com.spendingbuddy.app.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Personal presets: quick-fills for the purchase form. They stay on this phone and are never shared with the circle.
 * [onSave] gets (id or null for new, name, category, reason) and returns an error message or null.
 */
@Composable
fun PresetsScreen(
    state: BuddyState,
    onBack: () -> Unit,
    onSave: suspend (id: String?, name: String, category: String, reason: String) -> String?,
    onSetDefault: suspend (String?) -> String?,
    onDelete: suspend (String) -> String?,
    onUndoDelete: suspend (Preset) -> String?,
    snackbar: SnackbarHostState? = null
) {
    val context = LocalContext.current
    // editing: null = closed, "" = new, otherwise preset id
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var operationError by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteError by rememberSaveable { mutableStateOf<String?>(null) }
    var defaultErrorId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingRestore by rememberSaveable(stateSaver = PendingPresetRestoreSaver) { mutableStateOf<Preset?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbars = snackbar ?: remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = { ScreenTitle(context.getString(R.string.presets_presets), { if (!busy) onBack() }) },
        floatingActionButton = {
            if (state.presets.size < MAX_PRESETS) ExtendedFloatingActionButton(onClick = { if (!busy) editing = "" }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text(context.getString(R.string.presets_new_preset)) })
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).scrollFadingEdges(listState), state = listState, contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(context.getString(R.string.presets_only_on_this_phone_presets_fill), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item {
                if (defaultErrorId == null) ErrorText(operationError)
                val restore = pendingRestore
                if (restore != null && operationError != null) TextButton(enabled = !busy, onClick = { scope.launch {
                    busy = true
                    try {
                        operationError = onUndoDelete(restore)
                        if (operationError == null) pendingRestore = null
                    } finally { busy = false }
                } }) { Text(context.getString(R.string.presets_retry_restore)) }
            }
            if (state.presets.size >= MAX_PRESETS) item {
                Text(context.resources.getQuantityString(R.plurals.presets_capacity_saved, MAX_PRESETS, MAX_PRESETS), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.presets.isEmpty()) item {
                NotebookSurface(Modifier.fillMaxWidth()) {
                    Text(context.getString(R.string.presets_no_presets_yet), style = MaterialTheme.typography.titleMedium)
                    Text(context.getString(R.string.presets_add_one_for_a_place_you), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(state.presets, key = { it.id }) { p ->
                PresetRow(p, enabled = !busy, errorMessage = operationError.takeIf { defaultErrorId == p.id }, onEdit = { editing = p.id }, onDelete = { deleteError = null; confirmDelete = p.id }, onDefault = {
                    scope.launch {
                        busy = true
                        try {
                            operationError = onSetDefault(if (p.isDefault) null else p.id)
                            defaultErrorId = p.id.takeIf { operationError != null }
                        } finally { busy = false }
                    }
                })
            }
        }
    }
    confirmDelete?.let { id ->
        val p = state.presets.firstOrNull { it.id == id }
        if (p == null) { confirmDelete = null } else AlertDialog(
            onDismissRequest = { if (!busy) confirmDelete = null },
            title = { Text(context.getString(R.string.presets_delete_title, p.name)) },
            text = { Column { Text(context.getString(R.string.presets_purchases_you_already_logged_are_not)); ErrorText(deleteError) } },
            confirmButton = { TextButton(enabled = !busy, onClick = { scope.launch {
                busy = true
                try {
                    deleteError = onDelete(id)
                    if (deleteError == null) {
                        confirmDelete = null
                        operationError = null
                        defaultErrorId = null
                        scope.launch {
                            snackbars.currentSnackbarData?.dismiss()
                            if (snackbars.showSnackbar(context.getString(R.string.presets_deleted, p.name), context.getString(R.string.presets_undo), duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                                pendingRestore = p
                                busy = true
                                try {
                                    operationError = onUndoDelete(p)
                                    if (operationError == null) pendingRestore = null
                                    else listState.scrollToItem(0)
                                } finally { busy = false }
                            }
                        }
                    }
                } finally { busy = false }
            } }) { Text(context.getString(R.string.presets_delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { confirmDelete = null }) { Text(context.getString(R.string.presets_keep)) } }
        )
    }
    editing?.let { id ->
        val existing = state.presets.firstOrNull { it.id == id }
        if (id.isNotEmpty() && existing == null) { editing = null }
        else PresetEditSheet(state, existing, onDismiss = { editing = null }, onSave = { n, c, r -> onSave(existing?.id, n, c, r).also { if (it == null) editing = null } })
    }
}

@Composable
private fun PresetRow(p: Preset, onEdit: () -> Unit, onDelete: () -> Unit, onDefault: () -> Unit, enabled: Boolean, errorMessage: String?) {
    val context = LocalContext.current
    val impulse = p.reason == IMPULSE
    val description = context.getString(if (p.isDefault) R.string.presets_row_default_description else R.string.presets_row_description,
        p.name, p.category, if (impulse) context.getString(R.string.presets_impulse) else p.reason)
    val defaultActionDescription = context.getString(if (p.isDefault) R.string.presets_remove_default_description else R.string.presets_make_default_description, p.name)
    NotebookSurface(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
        contentDescription = description
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryChip(p.category)
                    Text(p.reason, style = MaterialTheme.typography.bodyMedium, color = if (impulse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    if (p.isDefault) Text(context.getString(R.string.presets_default), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onEdit, enabled = enabled, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Icon(Icons.Rounded.Edit, context.getString(R.string.presets_edit_description, p.name)) }
        }
        TextButton(onClick = onDefault, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = defaultActionDescription }) {
            Text(if (p.isDefault) context.getString(R.string.presets_remove_as_default) else context.getString(R.string.presets_make_default))
        }
        ErrorText(errorMessage)
        Rule()
        TextButton(onClick = onDelete, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text(context.getString(R.string.presets_delete_description, p.name), color = MaterialTheme.colorScheme.error)
        }
    }
}

private val PendingPresetRestoreSaver = Saver<Preset?, String>(
    save = { Json.encodeToString(it) },
    restore = { Json.decodeFromString<Preset?>(it) }
)

@Composable
private fun PresetEditSheet(state: BuddyState, existing: Preset?, onDismiss: () -> Unit, onSave: suspend (String, String, String) -> String?) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: state.lastCategory) }
    var reason by rememberSaveable { mutableStateOf(existing?.reason ?: state.reasons.firstOrNull() ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val rawNameError = error?.takeIf { it == "Give the preset a name" || it.startsWith("Keep the name") || it.startsWith("You already have a preset called") }
    val rawCategoryError = error?.takeIf { it == "Choose a category" }
    val rawReasonError = error?.takeIf { it == "Choose a reason" }
    val nameError = presetValidationMessage(rawNameError)
    val categoryError = presetValidationMessage(rawCategoryError)
    val reasonError = presetValidationMessage(rawReasonError)
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(confirmValueChange = { !busy || it != SheetValue.Hidden })) {
        DialogSystemBars()
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Text(if (existing == null) context.getString(R.string.presets_new_preset) else context.getString(R.string.presets_edit_preset), style = MaterialTheme.typography.headlineSmall)
            RequiredFieldsHint()
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(name, { name = it; error = null }, Modifier.fillMaxWidth(), label = { Text(context.getString(R.string.presets_merchant_or_what_for)) }, singleLine = true, enabled = !busy,
                isError = nameError != null, supportingText = nameError?.let { message -> { ErrorText(message) } })
            Spacer(Modifier.height(8.dp)); Text(context.getString(R.string.presets_category), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()).semantics { categoryError?.let { semanticsError(it) } }, horizontalArrangement = Arrangement.spacedBy(8.dp)) { state.categories.forEach { Pill(it, it == category, onClick = if (!busy) ({ category = it; error = null }) else null) } }
            ErrorText(categoryError)
            Spacer(Modifier.height(8.dp)); Text(context.getString(R.string.presets_why_did_you_buy_it), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()).semantics { reasonError?.let { semanticsError(it) } }, horizontalArrangement = Arrangement.spacedBy(8.dp)) { state.reasons.forEach { Pill(it, it == reason, onClick = if (!busy) ({ reason = it; error = null }) else null) } }
            ErrorText(reasonError)
            if (reason == IMPULSE) Text(context.getString(R.string.presets_impulse), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
            ErrorText(presetValidationMessage(error?.takeIf { it != rawNameError && it != rawCategoryError && it != rawReasonError }))
            Spacer(Modifier.height(8.dp))
            Button(enabled = !busy, onClick = {
                error = validatePreset(state.presets, name, category, reason, existing?.id)
                if (error == null) scope.launch {
                    busy = true
                    try { error = onSave(name, category, reason) } finally { busy = false }
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(context.getString(R.string.presets_save_preset)) }
            Spacer(Modifier.height(18.dp))
        }
    }
}
