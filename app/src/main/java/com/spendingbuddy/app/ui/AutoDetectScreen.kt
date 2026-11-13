@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.spendingbuddy.app.ui

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
    onEnabled: (Boolean) -> Unit,
    onNotificationAccess: () -> Unit,
    onSms: () -> Unit,
    onNotifications: () -> Unit,
    onLiveUpdates: () -> Unit,
    onSaveRule: (merchant: String, category: String, reason: String) -> Unit,
    onDeleteRule: (key: String) -> Unit,
    snackbar: SnackbarHostState? = null
) {
    val listState = rememberLazyListState()
    var editing by remember { mutableStateOf<MerchantRule?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val hasSource = access.notificationAccess || access.sms
    val ready = state.autoDetect && hasSource && access.canNotify
    val setupSummary = when {
        !state.autoDetect -> "Turn on automatic tracking, then connect one source: payment-app alerts or bank SMS."
        !hasSource -> "Tracking is on, but no payment source is connected yet. Connect payment-app alerts or bank SMS. Manual entry remains available in Today."
        !access.canNotify -> "A payment source is connected. Allow notifications so you can see new Auto entries and use Undo or Keep."
        else -> "A payment source is connected and notifications are allowed. Automatic tracking is ready."
    }
    val setupAction: (() -> Unit)? = when {
        !state.autoDetect -> ({ onEnabled(true) })
        !hasSource && !access.notificationAccess -> onNotificationAccess
        !hasSource -> onSms
        !access.canNotify -> onNotifications
        else -> null
    }
    val setupActionLabel = when {
        !state.autoDetect -> "Turn on automatic tracking"
        !hasSource -> "Connect payment alerts"
        !access.canNotify -> "Allow notifications"
        else -> "Ready"
    }
    val readyOutline = if (ready) {
        Modifier.border(1.dp, LocalPennyTokens.current.accent.copy(alpha = 0.30f), RoundedCornerShape(24.dp))
    } else Modifier
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        topBar = { ScreenTitle("Automatic tracking", onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).scrollFadingEdges(listState, bottom = 40.dp),
            state = listState,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                SettingsGroup(
                    "Setup",
                    Modifier.enterRise(0).then(readyOutline),
                    footer = "Connect one source first. Notifications let you see and undo an Auto entry. A Live Update on the lock screen or status bar is optional."
                ) {
                    Text(
                        setupSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                    if (setupAction != null) {
                        AccentButton(
                            onClick = setupAction,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            haptics = state.haptics
                        ) { Text(setupActionLabel, fontWeight = FontWeight.Bold) }
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
                    "Tracking", Modifier.enterRise(0),
                    footer = "The original message text is never stored or sent. Only the amount, shop name, time and a local payment reference used to catch duplicates are kept."
                ) {
                    SettingSwitch("Log purchases from payment alerts", state.autoDetect, onEnabled)
                }
            }
            item {
                SettingsGroup(
                    "Where to look", Modifier.enterRise(1),
                    footer = "Connect at least one source. Android may restrict access to payment-app alerts or bank SMS on some phones. You can still add purchases by hand from Today."
                ) {
                    AccessRow("Payment and bank app alerts", if (access.notificationAccess) "Allowed" else "Needs notification access", onNotificationAccess, allowed = access.notificationAccess)
                    GroupDivider()
                    AccessRow("Bank SMS", if (access.sms) "Allowed" else "Needs SMS permission", onSms, allowed = access.sms)
                    GroupDivider()
                    AccessRow("Show what was added", if (access.canNotify) "Allowed" else "Needs notification permission", onNotifications, allowed = access.canNotify)
                    GroupDivider()
                    AccessRow(
                        "Live Update on lock screen and status bar",
                        if (access.liveUpdates) "Optional · Allowed" else "Optional · Not allowed or not supported",
                        onLiveUpdates,
                        allowed = access.liveUpdates
                    )
                }
            }
            item {
                SettingsGroup(
                    "Your shops", Modifier.enterRise(2),
                    footer = "A shop with a rule always gets that category. Without a rule the app uses what you chose last time for that shop, then its built-in list, and otherwise saves it as Other and asks you."
                ) {
                    state.merchantRules.sortedBy { it.merchant.lowercase() }.forEach { rule ->
                        AccessRow(rule.merchant, "${rule.category} · ${rule.reason}", { editing = rule })
                        GroupDivider()
                    }
                    SettingsButton("Add a shop", { adding = true })
                }
            }
            item {
                SettingsGroup("How it decides") {
                    Text(
                        "Added on its own: card payments, and payments to a shop you have a rule for, have logged before, or that is on the built-in list.\n\n" +
                            "Asked first: payments to a new name or to a person, cash withdrawals, alerts with no shop name, and anything of ₹20,000 or more.\n\n" +
                            "Never added: money received, refunds, failed payments, one-time passwords, bills that are due and offers.\n\n" +
                            "A purchase added on its own can be undone for 10 minutes. Your circle sees it only after that, or sooner if you tap Keep.",
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
            onDismiss = { adding = false; editing = null },
            onSave = { merchant, category, reason -> onSaveRule(merchant, category, reason); adding = false; editing = null },
            onDelete = editing?.let { rule -> { onDeleteRule(rule.key); editing = null } }
        )
    }
}

/** A settings row with its current state underneath and a chevron: tapping it opens the place to change it. */
@Composable
private fun AccessRow(title: String, status: String, onClick: () -> Unit, allowed: Boolean = false) {
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
        if (allowed) Icon(Icons.Rounded.CheckCircle, "Allowed", tint = LocalPennyTokens.current.accent)
        else Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RuleSheet(
    state: BuddyState,
    existing: MerchantRule?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onDelete: (() -> Unit)?
) {
    var merchant by rememberSaveable { mutableStateOf(existing?.merchant.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: state.categories.first()) }
    var reason by rememberSaveable { mutableStateOf(existing?.reason ?: state.reasons.first()) }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        DialogSystemBars()
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Text(if (existing == null) "Add a shop" else "Edit shop", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Shop name as it appears in the alert") },
                singleLine = true,
                enabled = existing == null,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Category", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { Pill(it, it == category, onClick = { category = it }) }
            }
            Spacer(Modifier.height(8.dp))
            Text("Reason", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.reasons.forEach { Pill(it, it == reason, onClick = { reason = it }) }
            }
            ErrorText(error)
            Spacer(Modifier.height(16.dp))
            AccentButton(
                onClick = { if (merchant.none { it.isLetterOrDigit() }) error = "Enter the shop name" else onSave(merchant.trim(), category, reason) },
                modifier = Modifier.fillMaxWidth(),
                haptics = state.haptics
            ) { Text("Save", fontWeight = FontWeight.Bold) }
            if (onDelete != null) TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Remove this rule", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
