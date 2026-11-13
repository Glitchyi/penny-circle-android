package com.spendingbuddy.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.spendingbuddy.app.R
import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.ImportCandidate
import com.spendingbuddy.app.ImportCandidateErrors
import com.spendingbuddy.app.amountToPaise
import com.spendingbuddy.app.rupees
import com.spendingbuddy.app.importCandidateMessages

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ImportCandidateEditor(
    candidate: ImportCandidate,
    state: BuddyState,
    fieldErrors: ImportCandidateErrors,
    enabled: Boolean,
    onUpdate: (ImportCandidate) -> Unit
) {
    val errors = LocalContext.current.importCandidateMessages(candidate, fieldErrors)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val label = candidate.merchant.ifBlank { stringResource(R.string.purchase_fallback_name) }
        val selectLabel = stringResource(R.string.import_select_purchase, label)
        Row(
            Modifier.fillMaxWidth().toggleable(candidate.selected, enabled = enabled, role = Role.Checkbox,
                onValueChange = { onUpdate(candidate.copy(selected = it)) })
                .semantics(mergeDescendants = true) { contentDescription = selectLabel },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(candidate.selected, onCheckedChange = null, enabled = enabled)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.import_summary, amountToPaise(candidate.amountText)?.let(::rupees) ?: candidate.amountText, formatPurchaseDate(candidate.date)),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (candidate.warning.isNotBlank()) Text(localizedImportWarning(candidate.warning), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (candidate.possibleDuplicate) Text(stringResource(R.string.import_possible_duplicate),
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(candidate.amountText, { onUpdate(candidate.copy(amountText = it)) }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.import_amount_label)) }, singleLine = true, enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = errors.amount != null,
            supportingText = errors.amount?.let { message -> { Text(message) } })
        OutlinedTextField(candidate.date, { onUpdate(candidate.copy(date = it)) }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.purchase_date_label)) }, singleLine = true, enabled = enabled,
            isError = errors.date != null, supportingText = errors.date?.let { message -> { Text(message) } })
        OutlinedTextField(candidate.merchant, { onUpdate(candidate.copy(merchant = it)) }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.import_merchant_label)) }, singleLine = true, enabled = enabled,
            isError = errors.merchant != null, supportingText = errors.merchant?.let { message -> { Text(message) } })
        Column(Modifier.semantics { errors.category?.let { error(it) } }) {
            Text(stringResource(R.string.purchase_category), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (state.categories + candidate.category).filter { it.isNotBlank() }.distinct().forEach { category ->
                    Pill(category, candidate.category == category, onClick = if (enabled) ({ onUpdate(candidate.copy(category = category)) }) else null)
                }
            }
            ErrorText(errors.category)
        }
        Column(Modifier.semantics { errors.reason?.let { error(it) } }) {
            Text(stringResource(R.string.purchase_reason), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (state.reasons + candidate.reason).filter { it.isNotBlank() }.distinct().forEach { reason ->
                    Pill(reason, candidate.reason == reason, onClick = if (enabled) ({ onUpdate(candidate.copy(reason = reason)) }) else null)
                }
            }
            ErrorText(errors.reason)
        }
        Rule()
    }
}

@Composable
private fun localizedImportWarning(warning: String): String {
    val messages = listOf(
        "Review extracted details" to R.string.import_warning_review,
        "Date not detected; enter it before saving" to R.string.import_warning_date,
        "Merchant not detected; add it manually" to R.string.import_warning_merchant,
        com.spendingbuddy.app.INCOMING_WARNING to R.string.import_warning_incoming,
        "Relative date 'Today'; review before saving" to R.string.import_warning_today,
        "Relative date 'Yesterday'; review before saving" to R.string.import_warning_yesterday,
        "Date year is uncertain; review before saving" to R.string.import_warning_year
    )
    var result = warning
    messages.forEach { (original, resource) -> result = result.replace(original, stringResource(resource)) }
    return result
}

@Composable
internal fun localizedImportStatusError(message: String): String = when (message) {
    "Import was interrupted. Retry or remove this screenshot." -> stringResource(R.string.import_interrupted)
    "Reading timed out. Retry or remove this screenshot." -> stringResource(R.string.import_timeout)
    "Temporary screenshot copy is unavailable" -> stringResource(R.string.import_copy_unavailable)
    "The image could not be decoded" -> stringResource(R.string.import_decode_error)
    "Screenshot processing failed" -> stringResource(R.string.import_processing_error)
    else -> message
}
