@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.spendingbuddy.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bundled, offline-readable notices; opening this sheet sends no network request. */
@Composable
internal fun PrivacyLicensesSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val documents = remember(context) {
        context.assets.list("licenses").orEmpty().filter { it.endsWith(".txt") || it.endsWith(".csv") }
            .sortedBy { if (it == "index.txt") "" else it }
    }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val text by produceState<String?>(null, selected) {
        value = null
        val document = selected
        if (document != null) value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open("licenses/$document").bufferedReader(Charsets.UTF_8).use { it.readText() } }
                .getOrDefault("This notice could not be opened.")
        }
    }
    val chunks = remember(text) { text.orEmpty().lineSequence().chunked(30).map { it.joinToString("\n") }.toList() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LocalPennyTokens.current.surface
    ) {
        DialogSystemBars()
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Privacy and licenses", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Close, "Close privacy and licenses") }
            }
            if (selected == null) {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("Your purchases and payment-message parsing stay on this phone until records are eligible to share with your circle.", style = MaterialTheme.typography.bodyLarge)
                    }
                    item {
                        Text("Screenshot text recognition uses Google's on-device ML Kit. Images and recognition results are processed locally; the SDK may send performance and usage metrics and contact Google for updates. Circle connections may use encrypted transport relays.", style = MaterialTheme.typography.bodyMedium, color = LocalPennyTokens.current.muted)
                    }
                    item { Text("Bundled notices", style = MaterialTheme.typography.titleMedium) }
                    items(documents, key = { it }) { document ->
                        TextButton(onClick = { selected = document }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (document == "index.txt") "Third-party notices overview" else document.removeSuffix(".txt").removeSuffix(".csv").replace('-', ' '))
                        }
                    }
                }
            } else {
                TextButton(onClick = { selected = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text("All notices") }
                Text(selected.orEmpty(), style = MaterialTheme.typography.titleMedium)
                if (text == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 12.dp))
                    Text("Opening notice…", style = MaterialTheme.typography.bodyMedium)
                } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(chunks) { chunk -> Text(chunk, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
