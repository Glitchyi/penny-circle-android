@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.spendingbuddy.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.spendingbuddy.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendingbuddy.app.Expense
import com.spendingbuddy.app.INDIA_ZONE
import com.spendingbuddy.app.IMPULSE
import com.spendingbuddy.app.SOURCE_AUTO
import com.spendingbuddy.app.rupees
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/** A spacious, scrollable detail and action surface for one logged purchase. */
@Composable
fun PurchaseDetailsSheet(
    expense: Expense,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleImpulse: suspend (Boolean) -> String?,
    onDelete: suspend () -> String?
) {
    var error by rememberSaveable(expense.id) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val tokens = LocalPennyTokens.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
        confirmValueChange = { !busy || it != androidx.compose.material3.SheetValue.Hidden })
    val isImpulse = expense.kind == "expense" && expense.reason == IMPULSE
    val isAuto = expense.source == SOURCE_AUTO
    val now = System.currentTimeMillis()
    val holdStatus = when {
        !isAuto -> null
        expense.heldUntil > now -> {
            val minutesLeft = ((expense.heldUntil - now + 59_999L) / 60_000L).coerceAtLeast(1L)
            pluralStringResource(R.plurals.purchase_hold_remaining, minutesLeft.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), minutesLeft)
        }
        expense.heldUntil > 0L -> stringResource(R.string.purchase_hold_ended)
        else -> stringResource(R.string.purchase_hold_complete)
    }

    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        sheetState = sheetState,
        containerColor = tokens.surface,
        contentColor = tokens.text
    ) {
        DialogSystemBars()
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.purchase_details_heading),
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = PennyType.Mono,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = tokens.muted
                )
                IconButton(
                    onClick = onDismiss,
                    enabled = !busy,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.purchase_close_details), tint = tokens.muted)
                }
            }

            Spacer(Modifier.height(6.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = tokens.surface2,
                border = BorderStroke(1.dp, tokens.border)
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                    Text(
                        rupees(expense.amountPaise),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = PennyType.Mono,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isImpulse) MaterialTheme.colorScheme.error else tokens.text
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        expense.merchant.ifBlank { expense.category.ifBlank { stringResource(R.string.purchase_fallback_name) } },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.text
                    )
                    Spacer(Modifier.height(12.dp))
                    CategoryChip(expense.category)
                    Spacer(Modifier.height(7.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        if (isImpulse) {
                            Icon(
                                Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            if (expense.needsCategory) stringResource(R.string.purchase_needs_category) else if (isImpulse) stringResource(R.string.purchase_impulse) else expense.reason,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isImpulse || expense.needsCategory) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isImpulse) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (expense.note.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.purchase_note_heading),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp),
                            color = tokens.muted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(expense.note, style = MaterialTheme.typography.bodyLarge, color = tokens.text)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = tokens.surface,
                border = BorderStroke(1.dp, tokens.border)
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    DetailFact(label = stringResource(R.string.purchase_date_heading), value = formatPurchaseDate(expense.date))
                    DetailFact(label = stringResource(R.string.purchase_source_heading), value = if (isAuto) stringResource(R.string.purchase_source_auto) else stringResource(R.string.purchase_source_manual))
                    if (holdStatus != null) DetailFact(label = stringResource(R.string.purchase_status_heading), value = holdStatus)
                }
            }

            Spacer(Modifier.height(22.dp))
            ErrorText(error)
            Text(
                stringResource(R.string.purchase_actions_heading),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.4.sp),
                color = tokens.muted
            )
            Spacer(Modifier.height(10.dp))
            PurchaseActionRow(
                title = stringResource(R.string.purchase_edit),
                detail = stringResource(R.string.purchase_edit_detail),
                icon = { Icon(Icons.Rounded.Edit, null, tint = tokens.accent, modifier = Modifier.size(22.dp)) },
                enabled = !busy,
                onClick = onEdit
            )
            if (expense.kind == "expense") {
                Spacer(Modifier.height(8.dp))
                PurchaseActionRow(
                    title = if (isImpulse) stringResource(R.string.purchase_unmark_impulse) else stringResource(R.string.purchase_mark_impulse),
                    detail = if (isImpulse) stringResource(R.string.purchase_remove_impulse_detail) else stringResource(R.string.purchase_add_impulse_detail),
                    titleColor = if (isImpulse) MaterialTheme.colorScheme.error else tokens.text,
                    icon = {
                        Icon(
                            Icons.Rounded.LocalFireDepartment,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    enabled = !busy,
                    onClick = { scope.launch {
                        busy = true
                        try { error = onToggleImpulse(!isImpulse); if (error == null) onDismiss() } finally { busy = false }
                    } }
                )
            }

            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = tokens.surface,
                border = BorderStroke(1.dp, tokens.danger.copy(alpha = if (tokens.dark) 0.46f else 0.34f))
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    PurchaseActionRow(
                        title = stringResource(R.string.purchase_delete),
                        detail = stringResource(R.string.purchase_delete_detail),
                        titleColor = tokens.danger,
                        icon = { Icon(Icons.Rounded.Delete, null, tint = tokens.danger, modifier = Modifier.size(22.dp)) },
                        enabled = !busy,
                        onClick = { scope.launch {
                            busy = true
                            try { error = onDelete(); if (error == null) onDismiss() } finally { busy = false }
                        } },
                        inset = 0.dp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.purchase_delete_consequence),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.muted,
                        modifier = Modifier.padding(start = 48.dp, end = 4.dp, bottom = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DetailFact(label: String, value: String) {
    val tokens = LocalPennyTokens.current
    if (LocalDensity.current.fontScale >= 1.4f) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = PennyType.Mono, letterSpacing = 0.8.sp), color = tokens.muted)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = tokens.text)
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.width(76.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PennyType.Mono, letterSpacing = 0.8.sp),
            color = tokens.muted
        )
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = tokens.text)
    }
}

@Composable
private fun PurchaseActionRow(
    title: String,
    detail: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true,
    titleColor: Color = LocalPennyTokens.current.text,
    inset: androidx.compose.ui.unit.Dp = 12.dp
) {
    val tokens = LocalPennyTokens.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(tokens.surface2)
            .border(1.dp, tokens.border, RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = inset, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = titleColor)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = tokens.muted)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.NorthEast, contentDescription = null, tint = tokens.muted, modifier = Modifier.size(18.dp))
    }
}

internal fun formatPurchaseDate(date: String): String = runCatching {
    LocalDate.parse(date).atStartOfDay(INDIA_ZONE).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))
}.getOrDefault(date)
