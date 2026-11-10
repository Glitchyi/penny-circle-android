package com.spendingbuddy.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun SettingsScreen(
    state: BuddyState,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onLimits: () -> Unit,
    onCircle: (() -> Unit)?,
    onPresets: (() -> Unit)?,
    onAmountInput: (String) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onReminders: (Boolean) -> Unit,
    onAlerts: (Boolean) -> Unit,
    onReachable: (Boolean) -> Unit,
    onExport: () -> Unit,
    onRestore: () -> Unit,
    onBattery: () -> Unit,
    snackbar: SnackbarHostState? = null
) {
    var name by rememberSaveable { mutableStateOf(state.name) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        topBar = { ScreenTitle("Settings", onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text("Profile", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; onName(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name shown to your circle") }
                )
            }
            item { SettingsButton("Spending limits", onLimits) }
            if (onCircle != null) item { SettingsButton("Circle settings", onCircle) }
            if (onPresets != null) item { SettingsButton("Presets", onPresets) }
            item {
                Text(
                    "Amount entry",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = state.amountInput == "keypad",
                        onClick = { onAmountInput("keypad") },
                        label = { Text("Keypad") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = state.amountInput == "dial",
                        onClick = { onAmountInput("dial") },
                        label = { Text("Rotary dial") }
                    )
                }
            }
            item { SettingSwitch("Haptics", state.haptics, onHaptics) }
            item {
                Text(
                    "Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                SettingSwitch("10 PM check-in, 10 AM grace deadline", state.reminders, onReminders)
                SettingSwitch("Circle activity alerts", state.friendAlerts, onAlerts)
            }
            item {
                SettingSwitch("Stay reachable", state.stayReachable, onReachable)
                Text(
                    "Keeps a background connection so friends' updates can arrive sooner. Android may still delay it to save battery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SettingsButton("Battery settings", onBattery)
            }
            item {
                Text(
                    "Your data",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                SettingsButton("Export backup", onExport)
                SettingsButton("Restore backup", onRestore)
            }
            item {
                Text(
                    "Version 0.2.0 · Circle sync is direct between phones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 8.dp)
    ) {
        Text(text, Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null)
    }
}

@Composable
private fun SettingSwitch(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun LimitsScreen(state: BuddyState, onBack: () -> Unit, onSave: (Long, Long) -> Unit) {
    var daily by rememberSaveable { mutableStateOf((state.dailyLimitPaise / 100).toString()) }
    var weekly by rememberSaveable { mutableStateOf((state.weeklyLimitPaise / 100).toString()) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Penny Circle", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    "Step ${step + 1} of 3",
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
                    onClick = if (step == 2) onReminders else onNext,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text(if (step == 2) "Turn on reminders" else "Continue") }
                TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (step == 2) "Not now" else "Skip")
                }
            }
        }
    }
}
