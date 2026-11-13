@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.spendingbuddy.app.ui

import android.content.Context

import com.spendingbuddy.app.R

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
                .getOrDefault(context.getString(R.string.privacy_this_notice_could_not_be_opened))
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
                Text(context.getString(R.string.privacy_privacy_and_licenses), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Close, context.getString(R.string.privacy_close_privacy_and_licenses)) }
            }
            if (selected == null) {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(context.getString(R.string.privacy_your_purchases_and_payment_message_parsing), style = MaterialTheme.typography.bodyLarge)
                    }
                    item {
                        Text(context.getString(R.string.privacy_screenshot_text_recognition_uses_google_s), style = MaterialTheme.typography.bodyMedium, color = LocalPennyTokens.current.muted)
                    }
                    item { Text(context.getString(R.string.privacy_bundled_notices), style = MaterialTheme.typography.titleMedium) }
                    items(documents, key = { it }) { document ->
                        TextButton(onClick = { selected = document }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(context.noticeTitle(document))
                        }
                    }
                }
            } else {
                TextButton(onClick = { selected = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.privacy_all_notices)) }
                Text(context.noticeTitle(selected.orEmpty()), style = MaterialTheme.typography.titleMedium)
                if (text == null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 12.dp))
                    Text(context.getString(R.string.privacy_opening_notice), style = MaterialTheme.typography.bodyMedium)
                } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(chunks) { chunk -> Text(chunk, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

private fun Context.noticeTitle(document: String): String = when (document) {
    "index.txt" -> getString(R.string.privacy_third_party_notices_overview)
    "Android-SDK-terms.txt" -> getString(R.string.privacy_android_sdk_terms)
    "APACHE-2.0.txt" -> getString(R.string.privacy_apache_license)
    "bundled-dependency-notices.txt" -> getString(R.string.privacy_bundled_dependency_notices)
    "dependency-metadata.csv" -> getString(R.string.privacy_dependency_metadata)
    "Geist-OFL-1.1.txt" -> getString(R.string.privacy_geist_license)
    "Google-APIs-terms.txt" -> getString(R.string.privacy_google_apis_terms)
    "Google-ML-Kit-terms.txt" -> getString(R.string.privacy_mlkit_terms)
    "iroh-APACHE-2.0.txt" -> getString(R.string.privacy_iroh_apache)
    "iroh-MIT.txt" -> getString(R.string.privacy_iroh_mit)
    "JNA-APACHE-2.0.txt" -> getString(R.string.privacy_jna_apache)
    "JNA-libffi-LICENSE.txt" -> getString(R.string.privacy_jna_libffi)
    "JNA-LICENSE.txt" -> getString(R.string.privacy_jna_license)
    "ZXing-LICENSE.txt" -> getString(R.string.privacy_zxing_license)
    "ZXing-NOTICE.txt" -> getString(R.string.privacy_zxing_notice)
    else -> document
}
