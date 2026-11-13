@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.spendingbuddy.app.ui

import androidx.compose.ui.platform.LocalContext
import com.spendingbuddy.app.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.MerchantRule
import kotlinx.coroutines.launch

/** What the system currently allows. Read again each time the screen comes back to the front. */
class AutoDetectAccess(val notificationAccess: Boolean, val sms: Boolean, val canNotify: Boolean, val liveUpdates: Boolean)

/**
 * Settings for automatic tracking: the main switch, the three system permissions it can use, and the user's own
 * shop rules. Every sentence here describes what the code does; nothing is promised about phones it was not tested on.
 */
@Composable
fun AutoDetectScreen(
    state: BuddyState,
    access: AutoDetectAccess,
    onBack: () -> Unit,
    onEnabled: suspend (Boolean) -> String?,
    onNotificationAccess: () -> Unit,
    onSms: () -> Unit,
    onNotifications: () -> Unit,
    onLiveUpdates: () -> Unit,
    onSaveRule: suspend (merchant: String, category: String, reason: String) -> String?,
    onDeleteRule: suspend (key: String) -> String?,
    snackbar: SnackbarHostState? = null
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var editingKey by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val editing = state.merchantRules.firstOrNull { it.key == editingKey }
    var operationError by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val changeEnabled: (Boolean) -> Unit = { enabled ->
        if (!busy) scope.launch {
            busy = true
            try { operationError = onEnabled(enabled) } finally { busy = false }
        }
    }
    val hasSource = access.notificationAccess || access.sms
    val ready = state.autoDetect && hasSource && access.canNotify
    val setupSummary = when {
        !state.autoDetect -> context.getString(R.string.tracking_turn_on_automatic_tracking_then_connect)
        !hasSource -> context.getString(R.string.tracking_tracking_is_on_but_no_payment)
        !access.canNotify -> context.getString(R.string.tracking_a_payment_source_is_connected_allow)
        else -> context.getString(R.string.tracking_a_payment_source_is_connected_and)
    }
    val setupAction: (() -> Unit)? = when {
        !state.autoDetect -> ({ changeEnabled(true) })
        !hasSource && !access.notificationAccess -> onNotificationAccess
        !hasSource -> onSms
        !access.canNotify -> onNotifications
        else -> null
    }
    val setupActionLabel = when {
        !state.autoDetect -> context.getString(R.string.tracking_turn_on_automatic_tracking)
        !hasSource -> context.getString(R.string.tracking_connect_payment_alerts)
        !access.canNotify -> context.getString(R.string.tracking_allow_notifications)
        else -> context.getString(R.string.tracking_ready)
    }
    val readyOutline = if (ready) {
        Modifier.border(1.dp, LocalPennyTokens.current.accent.copy(alpha = 0.30f), RoundedCornerShape(24.dp))
    } else Modifier
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        topBar = { ScreenTitle(context.getString(R.string.tracking_automatic_tracking), { if (!busy) onBack() }) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).scrollFadingEdges(listState, bottom = 40.dp),
            state = listState,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                SettingsGroup(
                    context.getString(R.string.tracking_setup),
                    Modifier.enterRise(0).then(readyOutline),
                    footer = context.getString(R.string.tracking_connect_one_source_first_notifications_let)
                ) {
                    Text(
                        setupSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                    ErrorText(operationError)
                    if (setupAction != null) {
                        AccentButton(
                            onClick = setupAction,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            haptics = state.haptics
                        ) { Text(setupActionLabel) }
                    } else {
                        Text(
                            setupActionLabel,
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = PennyType.Mono),
                            color = LocalPennyTokens.current.accent,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
            item {
                SettingsGroup(
                    context.getString(R.string.tracking_tracking), Modifier.enterRise(0),
                    footer = context.getString(R.string.tracking_the_original_message_text_is_never)
                ) {
                    SettingSwitch(context.getString(R.string.tracking_log_purchases_from_payment_alerts), state.autoDetect, changeEnabled, enabled = !busy)
                    ErrorText(operationError)
                }
            }
            item {
                SettingsGroup(
                    context.getString(R.string.tracking_where_to_look), Modifier.enterRise(1),
                    footer = context.getString(R.string.tracking_connect_at_least_one_source_android)
                ) {
                    AccessRow(context.getString(R.string.tracking_payment_and_bank_app_alerts), if (access.notificationAccess) context.getString(R.string.tracking_allowed) else context.getString(R.string.tracking_needs_notification_access), onNotificationAccess, allowed = access.notificationAccess)
                    GroupDivider()
                    AccessRow(context.getString(R.string.tracking_bank_sms), if (access.sms) context.getString(R.string.tracking_allowed) else context.getString(R.string.tracking_needs_sms_permission), onSms, allowed = access.sms)
                    GroupDivider()
                    AccessRow(context.getString(R.string.tracking_show_what_was_added), if (access.canNotify) context.getString(R.string.tracking_allowed) else context.getString(R.string.tracking_needs_notification_permission), onNotifications, allowed = access.canNotify)
                    GroupDivider()
                    AccessRow(
                        context.getString(R.string.tracking_live_update_on_lock_screen_and),
                        if (access.liveUpdates) context.getString(R.string.tracking_optional_allowed) else context.getString(R.string.tracking_optional_not_allowed_or_not_supported),
                        onLiveUpdates,
                        allowed = access.liveUpdates
                    )
                }
            }
            item {
                SettingsGroup(
                    context.getString(R.string.tracking_your_shops), Modifier.enterRise(2),
                    footer = context.getString(R.string.tracking_a_shop_with_a_rule_always)
                ) {
                    state.merchantRules.sortedBy { it.merchant.lowercase() }.forEach { rule ->
                        AccessRow(rule.merchant, context.getString(R.string.tracking_rule_summary, rule.category, rule.reason), { if (!busy) editingKey = rule.key })
                        GroupDivider()
                    }
                    SettingsButton(context.getString(R.string.tracking_add_a_shop), { if (!busy) adding = true })
                }
            }
            item {
                SettingsGroup(context.getString(R.string.tracking_how_it_decides)) {
                    Text(
                        context.getString(R.string.tracking_decision_explanation),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
    if (adding || editing != null) {
        RuleSheet(
            state = state,
            existing = editing,
            onDismiss = { adding = false; editingKey = null },
            onSave = onSaveRule,
            onDelete = if (editing != null) ({ onDeleteRule(editing.key) }) else null
        )
    }
}

/** A settings row with its current state underneath and a chevron: tapping it opens the place to change it. */
@Composable
private fun AccessRow(title: String, status: String, onClick: () -> Unit, allowed: Boolean = false) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth()
            .then(if (allowed) Modifier.border(1.dp, LocalPennyTokens.current.accent.copy(alpha = 0.32f), RoundedCornerShape(16.dp)) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (allowed) Icon(Icons.Rounded.CheckCircle, context.getString(R.string.tracking_allowed), tint = LocalPennyTokens.current.accent)
        else Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RuleSheet(
    state: BuddyState,
    existing: MerchantRule?,
    onDismiss: () -> Unit,
    onSave: suspend (String, String, String) -> String?,
    onDelete: (suspend () -> String?)?
) {
    val context = LocalContext.current
    var merchant by rememberSaveable { mutableStateOf(existing?.merchant.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: state.categories.firstOrNull().orEmpty()) }
    var reason by rememberSaveable { mutableStateOf(existing?.reason ?: state.reasons.firstOrNull().orEmpty()) }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !busy || it != androidx.compose.material3.SheetValue.Hidden })) {
        DialogSystemBars()
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Text(if (existing == null) context.getString(R.string.tracking_add_a_shop) else context.getString(R.string.tracking_edit_shop), style = MaterialTheme.typography.headlineSmall)
            RequiredFieldsHint()
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it; nameError = null; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(context.getString(R.string.tracking_shop_name_as_it_appears_in)) },
                isError = nameError != null,
                supportingText = nameError?.let { message -> { ErrorText(message) } },
                singleLine = true,
                enabled = existing == null && !busy,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(context.getString(R.string.tracking_category), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { Pill(it, it == category, onClick = if (!busy) ({ category = it; error = null }) else null) }
            }
            Spacer(Modifier.height(8.dp))
            Text(context.getString(R.string.tracking_reason), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.reasons.forEach { Pill(it, it == reason, onClick = if (!busy) ({ reason = it; error = null }) else null) }
            }
            ErrorText(error)
            Spacer(Modifier.height(16.dp))
            AccentButton(
                onClick = {
                    nameError = context.getString(R.string.tracking_enter_the_shop_name).takeIf { merchant.none { it.isLetterOrDigit() } }
                    error = when {
                        category.isBlank() -> context.getString(R.string.validation_category_required)
                        reason.isBlank() -> context.getString(R.string.validation_reason_required)
                        else -> null
                    }
                    if (nameError == null && error == null) scope.launch {
                        busy = true
                        try { error = onSave(merchant.trim(), category, reason); if (error == null) onDismiss() } finally { busy = false }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                haptics = state.haptics
            ) { Text(context.getString(R.string.tracking_save)) }
            if (onDelete != null) TextButton(enabled = !busy, onClick = { scope.launch {
                busy = true
                try { error = onDelete(); if (error == null) onDismiss() } finally { busy = false }
            } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(context.getString(R.string.tracking_remove_this_rule), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
