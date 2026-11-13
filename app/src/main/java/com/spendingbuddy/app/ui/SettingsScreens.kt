package com.spendingbuddy.app.ui

import androidx.compose.ui.platform.LocalContext
import com.spendingbuddy.app.R

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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.spendingbuddy.app.amountToPaise
import com.spendingbuddy.app.dailyLimitInputError
import com.spendingbuddy.app.formatLimitInput
import com.spendingbuddy.app.parseOnboardingLimits
import com.spendingbuddy.app.rupees
import com.spendingbuddy.app.weeklyLimitFromDailyInput
import com.spendingbuddy.app.weeklyLimitInputError
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(state.name) }
    var privacyOpen by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        topBar = { ScreenTitle(context.getString(R.string.settings_settings), onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).scrollFadingEdges(listState, bottom = 40.dp),
            state = listState,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            item {
                SettingsGroup(context.getString(R.string.settings_profile), Modifier.enterRise(0)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; onName(it) },
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                        label = { Text(context.getString(R.string.settings_name_shown_to_your_circle)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
            item {
                SettingsGroup(context.getString(R.string.settings_spending_and_logging)) {
                    SettingsButton(context.getString(R.string.settings_spending_limits), onLimits, context.getString(R.string.settings_limits_summary, rupees(state.dailyLimitPaise), rupees(state.weeklyLimitPaise)))
                    GroupDivider()
                    SettingsButton(if (state.autoDetect) context.getString(R.string.settings_automatic_tracking_on) else context.getString(R.string.settings_automatic_tracking_off), onAutoDetect, context.getString(R.string.settings_choose_payment_sources_and_shop_rules))
                    if (onPresets != null) { GroupDivider(); SettingsButton(context.getString(R.string.settings_presets), onPresets, context.getString(R.string.settings_personal_shortcuts_for_logging_purchases)) }
                }
            }
            if (onCircle != null) item {
                SettingsGroup(context.getString(R.string.settings_circle)) {
                    SettingsButton(context.getString(R.string.settings_circle_settings), onCircle, context.getString(R.string.settings_sharing_invitations_and_membership))
                }
            }
            item {
                Column(Modifier.enterRise(1)) {
                    GroupLabel(context.getString(R.string.settings_appearance))
                    ThemePicker(
                        selectedId = state.theme,
                        onSelect = onTheme,
                        haptics = state.haptics
                    )
                }
            }
            item {
                SettingsGroup(context.getString(R.string.settings_input), Modifier.enterRise(2)) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(context.getString(R.string.settings_amount_entry), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        SegmentedToggle(
                            listOf(context.getString(R.string.settings_keypad), context.getString(R.string.settings_dial)),
                            if (state.amountInput == "dial") 1 else 0,
                            { onAmountInput(if (it == 1) "dial" else "keypad") },
                            Modifier.width(168.dp)
                        )
                    }
                    GroupDivider()
                    SettingSwitch(context.getString(R.string.settings_haptics), state.haptics, onHaptics)
                }
            }
            item {
                SettingsGroup(context.getString(R.string.settings_reminders)) {
                    SettingSwitch(context.getString(R.string.settings_10_pm_check_in_10_am), state.reminders, onReminders)
                    GroupDivider()
                    Text(context.getString(R.string.settings_payment_update_duration), Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp), style = MaterialTheme.typography.titleSmall)
                    Text(context.getString(R.string.settings_how_long_the_latest_budget_card), Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TimerOptions(PAYMENT_UPDATE_MINUTES, state.paymentUpdateMinutes, DEFAULT_PAYMENT_UPDATE_MINUTES, onPaymentUpdateMinutes)
                    GroupDivider()
                    SettingSwitch(context.getString(R.string.settings_countdown_before_the_10_pm_reminder), state.finishCountdown, onFinishCountdown, enabled = state.reminders)
                    Text(context.getString(R.string.settings_starts_before_the_fixed_10_pm), Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 3.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TimerOptions(FINISH_COUNTDOWN_LEAD_MINUTES, state.finishCountdownLeadMinutes, DEFAULT_FINISH_COUNTDOWN_LEAD_MINUTES, onFinishCountdownLeadMinutes, enabled = state.reminders)
                    Text(
                        if (state.reminders) context.getString(R.string.settings_countdown_start, finishCountdownStartTime(state.finishCountdownLeadMinutes).format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)))
                        else context.getString(R.string.settings_turn_on_reminders_to_use_the),
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    GroupDivider()
                    SettingSwitch(context.getString(R.string.settings_circle_activity_alerts), state.friendAlerts, onAlerts)
                }
            }
            item {
                SettingsGroup(
                    context.getString(R.string.settings_connection),
                    footer = context.getString(R.string.settings_staying_reachable_keeps_a_background_connection)
                ) {
                    SettingSwitch(context.getString(R.string.settings_stay_reachable), state.stayReachable, onReachable)
                    GroupDivider()
                    SettingsButton(context.getString(R.string.settings_battery_settings), onBattery)
                }
            }
            item {
                SettingsGroup(context.getString(R.string.settings_your_data), footer = context.getString(R.string.settings_version, BuildConfig.VERSION_NAME)) {
                    SettingsButton(context.getString(R.string.settings_export_backup), onExport)
                    GroupDivider()
                    SettingsButton(context.getString(R.string.settings_restore_backup), onRestore)
                    GroupDivider()
                    SettingsButton(context.getString(R.string.settings_privacy_and_licenses), { privacyOpen = true })
                }
            }
        }
    }
    if (privacyOpen) PrivacyLicensesSheet(onDismiss = { privacyOpen = false })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerOptions(options: List<Int>, selected: Int, default: Int, onSelect: (Int) -> Unit, enabled: Boolean = true) {
    val context = LocalContext.current
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { value ->
            val isSelected = selected == value
            val label = context.getString(if (value == default) R.string.settings_timer_default else R.string.settings_timer_minutes, value)
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(value) },
                enabled = enabled,
                label = { Text(if (isSelected) context.getString(R.string.settings_timer_selected, label) else label) }
            )
        }
    }
}

@Composable
internal fun GroupLabel(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono),
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
internal fun SettingsButton(text: String, onClick: () -> Unit, summary: String? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LimitsScreen(
    state: BuddyState,
    onBack: () -> Unit,
    dailyDraft: String? = null,
    weeklyDraft: String? = null,
    onDraftChange: (String, String) -> Unit = { _, _ -> },
    onSave: suspend (Long, Long) -> String?
) {
    val context = LocalContext.current
    var daily by rememberSaveable { mutableStateOf(dailyDraft ?: formatLimitInput(state.dailyLimitPaise)) }
    var weekly by rememberSaveable { mutableStateOf(weeklyDraft ?: formatLimitInput(state.weeklyLimitPaise)) }
    var dailyError by rememberSaveable { mutableStateOf<String?>(null) }
    var weeklyError by rememberSaveable { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val calculatedWeekly = weeklyLimitFromDailyInput(daily)
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { ScreenTitle(context.getString(R.string.settings_spending_limits), onBack) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(context.getString(R.string.settings_set_the_limits_you_want_to), style = MaterialTheme.typography.bodyLarge)
            RequiredFieldsHint()
            OutlinedTextField(
                value = daily,
                onValueChange = { daily = it; onDraftChange(it, weekly); if (dailyError != null) dailyError = dailyLimitInputError(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(context.getString(R.string.settings_daily_limit)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = dailyError != null,
                supportingText = dailyError?.let { message -> { ErrorText(context.budgetValidationMessage(message)) } }
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(300L, 500L, 800L, 1_000L).forEach { preset ->
                    Pill(rupees(preset * 100L), amountToPaise(daily) == preset * 100L, onClick = {
                        daily = preset.toString()
                        onDraftChange(daily, weekly)
                        dailyError = null
                    })
                }
            }
            OutlinedTextField(
                value = weekly,
                onValueChange = { weekly = it; onDraftChange(daily, it); if (weeklyError != null) weeklyError = weeklyLimitInputError(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(context.getString(R.string.settings_weekly_limit)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = weeklyError != null,
                supportingText = weeklyError?.let { message -> { ErrorText(context.budgetValidationMessage(message)) } }
            )
            TextButton(
                onClick = {
                    if (calculatedWeekly != null) {
                        weekly = calculatedWeekly
                        onDraftChange(daily, weekly)
                        weeklyError = null
                    }
                },
                enabled = calculatedWeekly != null,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(context.getString(R.string.settings_use_7_daily)) }
            ErrorText(saveError)
            Button(
                onClick = {
                    dailyError = dailyLimitInputError(daily)
                    weeklyError = weeklyLimitInputError(weekly)
                    parseOnboardingLimits(daily, weekly)?.let { valid ->
                        if (!saving) scope.launch {
                            saving = true
                            saveError = null
                            try { saveError = onSave(valid.dailyPaise, valid.weeklyPaise) }
                            finally { saving = false }
                        }
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text(if (saving) context.getString(R.string.settings_saving) else context.getString(R.string.settings_save_limits)) }
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
    val context = LocalContext.current
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(context.getString(R.string.settings_penny_circle), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    context.getString(R.string.settings_legacy_onboarding_step, step + 1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                when (step) {
                    0 -> {
                        Text(
                            context.getString(R.string.settings_what_should_your_circle_call_you),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 38.dp)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = onName,
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                            label = { Text(context.getString(R.string.settings_your_name)) },
                            singleLine = true
                        )
                    }

                    1 -> {
                        Text(
                            context.getString(R.string.settings_pick_a_look),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 38.dp)
                        )
                        Text(
                            context.getString(R.string.settings_you_can_change_it_any_time),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        ThemePicker(selectedId = theme, onSelect = onTheme, modifier = Modifier.padding(top = 20.dp))
                    }

                    2 -> {
                        Text(
                            context.getString(R.string.settings_pick_a_daily_limit),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 38.dp)
                        )
                        Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("300", "500", "800", "1000").forEach {
                                Pill(rupees(it.toLong() * 100L), daily == it, onClick = { onDaily(it); onWeekly((it.toLong() * 7).toString()) })
                            }
                        }
                        OutlinedTextField(
                            value = daily,
                            onValueChange = onDaily,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(context.getString(R.string.settings_daily_limit)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = weekly,
                            onValueChange = onWeekly,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            label = { Text(context.getString(R.string.settings_weekly_limit)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    else -> {
                        Text(
                            context.getString(R.string.settings_stay_on_track),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 38.dp)
                        )
                        Text(
                            context.getString(R.string.settings_a_10_pm_reminder_helps_you),
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
                ) { Text(if (step == 3) context.getString(R.string.settings_turn_on_reminders) else context.getString(R.string.settings_continue)) }
                TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (step == 3) context.getString(R.string.settings_not_now) else context.getString(R.string.settings_skip))
                }
            }
        }
    }
}
