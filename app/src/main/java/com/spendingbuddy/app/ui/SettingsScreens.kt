package com.spendingbuddy.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.BuildConfig
import com.spendingbuddy.app.PAYMENT_UPDATE_MINUTES
import com.spendingbuddy.app.FINISH_COUNTDOWN_LEAD_MINUTES
import com.spendingbuddy.app.DEFAULT_PAYMENT_UPDATE_MINUTES
import com.spendingbuddy.app.DEFAULT_FINISH_COUNTDOWN_LEAD_MINUTES
import com.spendingbuddy.app.finishCountdownStartTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(
    state: BuddyState,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onTheme: (String) -> Unit,
    onLimits: () -> Unit,
    onAutoDetect: () -> Unit,
    onCircle: (() -> Unit)?,
    onPresets: (() -> Unit)?,
    onAmountInput: (String) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onReminders: (Boolean) -> Unit,
    onFinishCountdown: (Boolean) -> Unit,
    onPaymentUpdateMinutes: (Int) -> Unit,
    onFinishCountdownLeadMinutes: (Int) -> Unit,
    onAlerts: (Boolean) -> Unit,
    onReachable: (Boolean) -> Unit,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onBattery: () -> Unit,
    snackbar: SnackbarHostState? = null
) {
    var name by rememberSaveable { mutableStateOf(state.name) }
    var privacyOpen by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        topBar = { ScreenTitle("Settings", onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).scrollFadingEdges(listState, bottom = 40.dp),
            state = listState,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                SettingsGroup("Profile", Modifier.enterRise(0)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; onName(it) },
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                        label = { Text("Name shown to your circle") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                    GroupDivider()
                    SettingsButton("Spending limits", onLimits)
                    GroupDivider()
                    SettingsButton(if (state.autoDetect) "Automatic tracking: on" else "Automatic tracking: off", onAutoDetect)
                    if (onCircle != null) { GroupDivider(); SettingsButton("Circle settings", onCircle) }
                    if (onPresets != null) { GroupDivider(); SettingsButton("Presets", onPresets) }
                }
            }
            item {
                Column(Modifier.enterRise(1)) {
                    GroupLabel("Appearance")
                    ThemePicker(
                        selectedId = state.theme,
                        onSelect = onTheme,
                        haptics = state.haptics
                    )
                }
            }
            item {
                SettingsGroup("Input", Modifier.enterRise(2)) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Amount entry", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        SegmentedToggle(
                            listOf("Keypad", "Dial"),
                            if (state.amountInput == "dial") 1 else 0,
                            { onAmountInput(if (it == 1) "dial" else "keypad") },
                            Modifier.width(168.dp)
                        )
                    }
                    GroupDivider()
                    SettingSwitch("Haptics", state.haptics, onHaptics)
                }
            }
            item {
                SettingsGroup("Reminders") {
                    SettingSwitch("10 PM check-in, 10 AM IST grace deadline", state.reminders, onReminders)
                    GroupDivider()
                    Text("Payment update duration", Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp), style = MaterialTheme.typography.titleSmall)
                    Text("How long the latest budget card stays after a new payment. The 10-minute Undo hold is unchanged.", Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TimerOptions(PAYMENT_UPDATE_MINUTES, state.paymentUpdateMinutes, DEFAULT_PAYMENT_UPDATE_MINUTES, "min", onPaymentUpdateMinutes)
                    GroupDivider()
                    SettingSwitch("Countdown before the 10 PM reminder", state.finishCountdown, onFinishCountdown, enabled = state.reminders)
                    Text("Starts before the fixed 10 PM IST reminder. Late logging remains available through the next-day 10 AM IST grace.", Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 3.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TimerOptions(FINISH_COUNTDOWN_LEAD_MINUTES, state.finishCountdownLeadMinutes, DEFAULT_FINISH_COUNTDOWN_LEAD_MINUTES, "min", onFinishCountdownLeadMinutes, enabled = state.reminders)
                    Text(
                        if (state.reminders) "Selected start: ${finishCountdownStartTime(state.finishCountdownLeadMinutes).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))} IST. WorkManager timing is approximate; Android may not promote it as a Live Update."
                        else "Turn on reminders to use the finish-day countdown.",
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GroupDivider()
                    SettingSwitch("Circle activity alerts", state.friendAlerts, onAlerts)
                }
            }
            item {
                SettingsGroup(
                    "Connection",
                    footer = "Staying reachable keeps a background connection so friends' updates can arrive sooner. Android may still delay it to save battery."
                ) {
                    SettingSwitch("Stay reachable", state.stayReachable, onReachable)
                    GroupDivider()
                    SettingsButton("Battery settings", onBattery)
                }
            }
            item {
                SettingsGroup("Your data", footer = "Version ${BuildConfig.VERSION_NAME} · Circle sync is direct between phones.") {
                    SettingsButton("Export backup", onExport)
                    GroupDivider()
                    SettingsButton("Restore backup", onRestore)
                    GroupDivider()
                    SettingsButton("Privacy and licenses", { privacyOpen = true })
                }
            }
        }
    }
    if (privacyOpen) PrivacyLicensesSheet(onDismiss = { privacyOpen = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerOptions(options: List<Int>, selected: Int, default: Int, unit: String, onSelect: (Int) -> Unit, enabled: Boolean = true) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { value ->
            val isSelected = selected == value
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(value) },
                enabled = enabled,
                label = { Text(buildString { if (isSelected) append("✓ "); append(value); append(' '); append(unit); if (value == default) append(" · default") }) }
            )
        }
    }
}

@Composable
internal fun GroupLabel(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp).semantics { heading(); contentDescription = title }
    )
}

@Composable
internal fun GroupDivider() {
    HorizontalDivider(Modifier.padding(start = 16.dp), thickness = 1.dp, color = LocalPennyTokens.current.text.copy(alpha = 0.08f))
}

/** A labelled group of rows in one frosted card, with an optional explanation underneath. */
@Composable
internal fun SettingsGroup(title: String, modifier: Modifier = Modifier, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier) {
        GroupLabel(title)
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)), content = content)
        if (footer != null) Text(
            footer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp)
        )
    }
}

@Composable
internal fun SettingsButton(text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SettingSwitch(text: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    // The whole row is the switch, so the touch target is the full width.
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, Modifier.weight(1f).padding(end = 12.dp), style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun LimitsScreen(state: BuddyState, onBack: () -> Unit, onSave: (Long, Long) -> Unit) {
    var daily by rememberSaveable { mutableStateOf((state.dailyLimitPaise / 100).toString()) }
    var weekly by rememberSaveable { mutableStateOf((state.weeklyLimitPaise / 100).toString()) }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { ScreenTitle("Spending limits", onBack) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Set the limits you want to see each day and week.", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = daily,
                onValueChange = { daily = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Daily limit (₹)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("300", "500", "800", "1000").forEach { Pill("₹$it", daily == it, onClick = { daily = it }) }
            }
            OutlinedTextField(
                value = weekly,
                onValueChange = { weekly = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Weekly limit (₹)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            TextButton(
                onClick = { weekly = ((daily.toLongOrNull() ?: 0) * 7).toString() },
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Use 7 × daily") }
            Button(
                onClick = { onSave((daily.toLongOrNull() ?: 0) * 100, (weekly.toLongOrNull() ?: 0) * 100) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text("Save limits") }
        }
    }
}

@Composable
fun OnboardingScreen(
    step: Int,
    theme: String,
    onTheme: (String) -> Unit,
    name: String,
    daily: String,
    weekly: String,
    onName: (String) -> Unit,
    onDaily: (String) -> Unit,
    onWeekly: (String) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onReminders: () -> Unit
) {
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text("Penny Circle", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Step ${step + 1} of 4",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                when (step) {
                    0 -> {
                        Text(
                            "What should your circle call you?",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 38.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = onName,
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                            label = { Text("Your name") },
                            singleLine = true
                        )
                    }

                    1 -> {
                        Text(
                            "Pick a look",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 38.dp)
                        )
                        Text(
                            "You can change it any time in Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        ThemePicker(selectedId = theme, onSelect = onTheme, modifier = Modifier.padding(top = 20.dp))
                    }

                    2 -> {
                        Text(
                            "Pick a daily limit",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 38.dp)
                        )
                        Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("300", "500", "800", "1000").forEach {
                                Pill("₹$it", daily == it, onClick = { onDaily(it); onWeekly((it.toLong() * 7).toString()) })
                            }
                        }
                        OutlinedTextField(
                            value = daily,
                            onValueChange = onDaily,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Daily limit (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = weekly,
                            onValueChange = onWeekly,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            label = { Text("Weekly limit (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    else -> {
                        Text(
                            "Stay on track",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 38.dp)
                        )
                        Text(
                            "A 10 PM reminder helps you finish the day. If you miss it, finish before 10 AM to keep your streak. Friends in your circle can send a nudge when you need one.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = if (step == 3) onReminders else onNext,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text(if (step == 3) "Turn on reminders" else "Continue") }
                TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (step == 3) "Not now" else "Skip")
                }
            }
        }
    }
}
