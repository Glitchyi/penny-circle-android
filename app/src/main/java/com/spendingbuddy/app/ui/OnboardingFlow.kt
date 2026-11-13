@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.spendingbuddy.app.ui

import androidx.compose.ui.platform.LocalContext
import com.spendingbuddy.app.R

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.LayoutDirection
import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.formatLimitInput
import com.spendingbuddy.app.rupees
import com.spendingbuddy.app.rupeeTrackerBitmap
import com.spendingbuddy.app.OnboardingLimits
import com.spendingbuddy.app.onboardingLimitsError
import com.spendingbuddy.app.dailyLimitInputError
import com.spendingbuddy.app.weeklyLimitInputError
import com.spendingbuddy.app.parseOnboardingLimits

private const val ONBOARDING_STEPS = 5

/** Guided first-run setup. Permission callbacks run only after the user taps the matching card. */
@Composable
fun OnboardingFlow(
    state: BuddyState,
    access: AutoDetectAccess,
    onTheme: (String) -> Unit,
    onNotificationAccess: () -> Unit,
    onSms: () -> Unit,
    onNotifications: () -> Unit,
    onComplete: (String, Long, Long, Boolean, Boolean) -> Unit,
    snackbar: SnackbarHostState? = null
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(state.name.take(40)) }
    var themeId by rememberSaveable { mutableStateOf(state.theme) }
    var daily by rememberSaveable { mutableStateOf(formatLimitInput(state.dailyLimitPaise)) }
    var weekly by rememberSaveable { mutableStateOf(formatLimitInput(state.weeklyLimitPaise)) }
    var automaticTracking by rememberSaveable { mutableStateOf(state.autoDetect) }
    var reminders by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val reducedMotion = LocalReducedMotion.current
    val layoutSign = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    val needsSource = automaticTracking && !access.sms && !access.notificationAccess
    val tokens = LocalPennyTokens.current
    val focusManager = LocalFocusManager.current
    val largeText = LocalDensity.current.fontScale >= 1.4f
    val dailyFocus = remember { FocusRequester() }
    val weeklyFocus = remember { FocusRequester() }

    fun limitsOrShowError(): OnboardingLimits? {
        val valid = parseOnboardingLimits(daily, weekly)
        error = if (valid == null) onboardingLimitsError(daily, weekly) else null
        return valid
    }

    fun advance() {
        when (step) {
            1 -> if (name.trim().isBlank()) error = context.getString(R.string.onboarding_add_a_name_for_your_notebook)
                else { error = null; focusManager.clearFocus(); step++ }
            2 -> if (limitsOrShowError() != null) { focusManager.clearFocus(); step++ }
            else -> { error = null; focusManager.clearFocus(); if (step < ONBOARDING_STEPS - 1) step++ }
        }
    }

    fun goBack() {
        if (step > 0) {
            error = null
            focusManager.clearFocus()
            step--
        }
    }

    BackHandler(enabled = step > 0, onBack = ::goBack)

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().background(tokens.bg.copy(alpha = 0.96f)).imePadding().navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (error != null && step != 1 && step != 2) Text(
                    error.orEmpty(), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
                )
                if (largeText) {
                    if (step > 0) TextButton(onClick = ::goBack, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(context.getString(R.string.onboarding_back)) }
                    AccentButton(
                        onClick = {
                            if (step == ONBOARDING_STEPS - 1) {
                                val limits = limitsOrShowError()
                                if (limits != null) onComplete(name.trim().take(40), limits.dailyPaise, limits.weeklyPaise, automaticTracking, reminders)
                            } else if (step == 3 && needsSource) onNotificationAccess() else advance()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        haptics = state.haptics
                    ) {
                        Text(if (step == ONBOARDING_STEPS - 1) context.getString(R.string.onboarding_finish_setup) else if (step == 0) context.getString(R.string.onboarding_get_started) else if (step == 3 && needsSource) context.getString(R.string.onboarding_connect_payment_alerts) else context.getString(R.string.onboarding_continue))
                    }
                } else Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (step > 0) TextButton(onClick = ::goBack, modifier = Modifier.heightIn(min = 52.dp)) { Text(context.getString(R.string.onboarding_back)) }
                    AccentButton(
                        onClick = {
                            if (step == ONBOARDING_STEPS - 1) {
                                val limits = limitsOrShowError()
                                if (limits != null) onComplete(name.trim().take(40), limits.dailyPaise, limits.weeklyPaise, automaticTracking, reminders)
                            } else if (step == 3 && needsSource) onNotificationAccess() else advance()
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        haptics = state.haptics
                    ) {
                        Text(if (step == ONBOARDING_STEPS - 1) context.getString(R.string.onboarding_finish_setup) else if (step == 0) context.getString(R.string.onboarding_get_started) else if (step == 3 && needsSource) context.getString(R.string.onboarding_connect_payment_alerts) else context.getString(R.string.onboarding_continue))
                    }
                }
            if (step == 3 && needsSource) TextButton(
                onClick = { automaticTracking = false; advance() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(context.getString(R.string.onboarding_use_manual_entry_for_now)) }
            }
        }
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp).padding(top = if (largeText) 4.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(if (largeText) 36.dp else 42.dp).clip(RoundedCornerShape(13.dp))
                        .background(tokens.accent),
                    contentAlignment = Alignment.Center
                ) { Text("P", style = MaterialTheme.typography.titleLarge.copy(fontSize = if (largeText) 16.sp else 22.sp), color = tokens.onAccent, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(context.getString(R.string.onboarding_penny_circle), style = (if (largeText) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge).copy(fontFamily = PennyType.Mono, letterSpacing = 1.2.sp), color = tokens.text, maxLines = if (largeText) 2 else 1)
                    if (!largeText) Text(context.getString(R.string.onboarding_your_shared_notebook), style = MaterialTheme.typography.labelSmall.copy(fontFamily = PennyType.Mono, letterSpacing = 0.8.sp), color = tokens.muted, maxLines = 1)
                }
                Text(context.getString(R.string.onboarding_step_counter, step + 1, ONBOARDING_STEPS), style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono), color = tokens.muted)
            }
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(tokens.text.copy(alpha = 0.10f))) {
                Box(Modifier.fillMaxWidth((step + 1f) / ONBOARDING_STEPS).fillMaxHeight().background(tokens.accent, CircleShape))
            }
            AnimatedContent(
                targetState = step,
                modifier = Modifier.fillMaxWidth().weight(1f),
                transitionSpec = {
                    if (reducedMotion) EnterTransition.None togetherWith ExitTransition.None
                    else {
                        val forward = targetState > initialState
                        slideInHorizontally(PennyMotion.spatial()) { (if (forward) it else -it) * layoutSign } togetherWith
                            slideOutHorizontally(PennyMotion.spatial()) { (if (forward) -it else it) * layoutSign }
                    }
                },
                label = "onboarding-step"
            ) { stage ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (stage) {
                        0 -> WelcomeStep()
                        1 -> ProfileStep(
                            name = name,
                            error = error,
                            themeId = themeId,
                            onName = { name = it.take(40); error = null },
                            onTheme = { themeId = it; onTheme(it) },
                            onNameDone = ::advance
                        )
                        2 -> LimitsStep(
                            daily = daily,
                            weekly = weekly,
                            error = error,
                            dailyFocus = dailyFocus,
                            weeklyFocus = weeklyFocus,
                            onDaily = { daily = it.take(14); error = null },
                            onWeekly = { weekly = it.take(14); error = null },
                            onNextField = { weeklyFocus.requestFocus() },
                            onDone = ::advance
                        )
                        3 -> TrackingStep(
                            automatic = automaticTracking,
                            access = access,
                            onAutomatic = { automaticTracking = it },
                            onNotificationAccess = onNotificationAccess,
                            onSms = onSms,
                            onNotifications = onNotifications
                        )
                        else -> ReadyStep(
                            name = name.trim().ifBlank { context.getString(R.string.onboarding_you) },
                            themeId = themeId,
                            daily = parseOnboardingLimits(daily, weekly)?.dailyPaise ?: 0L,
                            weekly = parseOnboardingLimits(daily, weekly)?.weeklyPaise ?: 0L,
                            automatic = automaticTracking,
                            access = access,
                            reminders = reminders,
                            onReminders = { reminders = it },
                            onNotifications = onNotifications
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    Eyebrow(context.getString(R.string.onboarding_a_friendly_daily_check_in))
    Text(context.getString(R.string.onboarding_a_clearer_day_shared_with_your), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
    Text(
        context.getString(R.string.onboarding_penny_circle_is_a_small_shared),
        style = MaterialTheme.typography.bodyLarge,
        color = t.muted
    )
    NotebookSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(CircleShape).background(Brush.linearGradient(listOf(t.accent.copy(alpha = 0.28f), t.accent.copy(alpha = 0.18f)))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.AutoAwesome, null, tint = t.accent, modifier = Modifier.size(27.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(context.getString(R.string.onboarding_today), style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.2.sp), color = t.muted)
                Text(context.getString(R.string.onboarding_one_honest_day_at_a_time), style = MaterialTheme.typography.titleMedium, color = t.text)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            listOf(context.getString(R.string.onboarding_m), context.getString(R.string.onboarding_t), context.getString(R.string.onboarding_w), context.getString(R.string.onboarding_t), context.getString(R.string.onboarding_f), context.getString(R.string.onboarding_s), context.getString(R.string.onboarding_s)).forEachIndexed { index, day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(if (index == 4) 30.dp else 24.dp).border(if (index == 4) 3.dp else 2.dp, if (index == 4) t.accent else t.border, CircleShape))
                    Text(day, style = MaterialTheme.typography.labelSmall.copy(fontFamily = PennyType.Mono), color = if (index == 4) t.text else t.muted)
                }
            }
        }
    }
    Text(context.getString(R.string.onboarding_your_expenses_stay_on_this_phone), style = MaterialTheme.typography.bodyMedium, color = t.muted)
    Text(context.getString(R.string.onboarding_no_account_to_create_no_finance), style = MaterialTheme.typography.bodyMedium, color = t.muted)
}

@Composable
private fun ProfileStep(name: String, error: String?, themeId: String, onName: (String) -> Unit, onTheme: (String) -> Unit, onNameDone: () -> Unit) {
    val context = LocalContext.current
    Eyebrow(context.getString(R.string.onboarding_01_make_it_yours))
    Text(context.getString(R.string.onboarding_what_should_your_circle_call_you), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
    Text(context.getString(R.string.onboarding_choose_a_name_and_a_look), style = MaterialTheme.typography.bodyMedium, color = LocalPennyTokens.current.muted)
    RequiredFieldsHint()
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(context.getString(R.string.onboarding_your_name)) },
        isError = error != null,
        supportingText = { Text(error ?: context.getString(R.string.onboarding_up_to_40_characters), modifier = Modifier.semantics { if (error != null) liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onNameDone() })
    )
    Text(context.getString(R.string.onboarding_pick_a_theme), style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp), color = LocalPennyTokens.current.muted)
    ThemePicker(selectedId = themeId, onSelect = onTheme, modifier = Modifier.fillMaxWidth(), haptics = false)
}

@Composable
private fun LimitsStep(
    daily: String,
    weekly: String,
    error: String?,
    dailyFocus: FocusRequester,
    weeklyFocus: FocusRequester,
    onDaily: (String) -> Unit,
    onWeekly: (String) -> Unit,
    onNextField: () -> Unit,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    val presets = listOf(300L, 500L, 800L, 1_000L)
    val dailyFieldError = error?.takeIf { it == dailyLimitInputError(daily) }
    val weeklyFieldError = error?.takeIf { it == weeklyLimitInputError(weekly) }
    Eyebrow(context.getString(R.string.onboarding_02_set_your_limits))
    Text(context.getString(R.string.onboarding_keep_the_daily_and_weekly_targets), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
    Text(context.getString(R.string.onboarding_start_with_a_suggestion_or_enter), style = MaterialTheme.typography.bodyMedium, color = t.muted)
    RequiredFieldsHint()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        presets.forEach { rupee ->
            val presetWeekly = rupee * 7L
            val selected = com.spendingbuddy.app.amountToPaise(daily) == rupee * 100L && com.spendingbuddy.app.amountToPaise(weekly) == presetWeekly * 100L
            FilterChip(
                selected = selected,
                onClick = { onDaily(rupee.toString()); onWeekly(presetWeekly.toString()) },
                label = { Text(context.getString(R.string.onboarding_limit_preset, rupees(rupee * 100L), rupees(presetWeekly * 100L))) }
            )
        }
    }
    OutlinedTextField(
        value = daily,
        onValueChange = onDaily,
        modifier = Modifier.fillMaxWidth().focusRequester(dailyFocus),
        label = { Text(context.getString(R.string.onboarding_daily_limit)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNextField() }),
        isError = dailyFieldError != null,
        supportingText = dailyFieldError?.let { message -> { ErrorText(context.budgetValidationMessage(message)) } }
    )
    OutlinedTextField(
        value = weekly,
        onValueChange = onWeekly,
        modifier = Modifier.fillMaxWidth().focusRequester(weeklyFocus),
        label = { Text(context.getString(R.string.onboarding_weekly_limit)) },
        supportingText = {
            if (weeklyFieldError != null) ErrorText(context.budgetValidationMessage(weeklyFieldError)) else Text(context.getString(R.string.onboarding_set_the_limit_for_the_full))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        isError = weeklyFieldError != null
    )
}

@Composable
private fun TrackingStep(
    automatic: Boolean,
    access: AutoDetectAccess,
    onAutomatic: (Boolean) -> Unit,
    onNotificationAccess: () -> Unit,
    onSms: () -> Unit,
    onNotifications: () -> Unit
) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    Eyebrow(context.getString(R.string.onboarding_03_choose_how_to_log))
    Text(context.getString(R.string.onboarding_let_the_phone_help_or_keep), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
    Text(context.getString(R.string.onboarding_you_can_change_this_any_time), style = MaterialTheme.typography.bodyMedium, color = t.muted)
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceCard(
            title = context.getString(R.string.onboarding_automatic_tracking),
            subtitle = context.getString(R.string.onboarding_read_supported_payment_alerts_on_this),
            selected = automatic,
            onClick = { onAutomatic(true) }
        )
        ChoiceCard(
            title = context.getString(R.string.onboarding_manual_entry),
            subtitle = context.getString(R.string.onboarding_add_purchases_yourself_from_today_you),
            selected = !automatic,
            onClick = { onAutomatic(false) }
        )
    }
    if (automatic) {
        Text(context.getString(R.string.onboarding_set_up_access), style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp), color = t.muted)
        Text(context.getString(R.string.onboarding_each_button_opens_only_after_you), style = MaterialTheme.typography.bodyMedium, color = t.muted)
        val allowedCount = listOf(access.notificationAccess, access.sms, access.canNotify).count { it }
        NotebookSurface(Modifier.fillMaxWidth()) {
            Text(context.getString(R.string.onboarding_access_status), style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp), color = t.muted)
            Text(context.getString(R.string.onboarding_permission_count, allowedCount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                context.getString(when {
                    access.notificationAccess || access.sms -> if (access.canNotify) R.string.onboarding_source_ready_notices_allowed else R.string.onboarding_source_ready_notices_off
                    else -> if (access.canNotify) R.string.onboarding_source_missing_notices_allowed else R.string.onboarding_source_missing_notices_off
                }),
                style = MaterialTheme.typography.bodyMedium,
                color = t.muted
            )
        }
        AccessSetupCard(
            title = context.getString(R.string.onboarding_payment_and_bank_app_alerts),
            detail = context.getString(R.string.onboarding_needed_to_notice_payments_from_supported),
            enabled = access.notificationAccess,
            action = if (access.notificationAccess) context.getString(R.string.onboarding_review_access) else context.getString(R.string.onboarding_allow_access),
            onClick = onNotificationAccess
        )
        AccessSetupCard(
            title = context.getString(R.string.onboarding_bank_sms),
            detail = context.getString(R.string.onboarding_reads_new_sms_alerts_only_there),
            enabled = access.sms,
            action = if (access.sms) context.getString(R.string.onboarding_review_permission) else context.getString(R.string.onboarding_allow_sms),
            onClick = onSms
        )
        AccessSetupCard(
            title = context.getString(R.string.onboarding_purchase_notifications),
            detail = context.getString(R.string.onboarding_shows_an_undo_or_review_notice),
            enabled = access.canNotify,
            action = if (access.canNotify) context.getString(R.string.onboarding_review_permission) else context.getString(R.string.onboarding_allow_notifications),
            onClick = onNotifications
        )
        NotebookSurface(Modifier.fillMaxWidth()) {
            Text(context.getString(R.string.onboarding_optional_android_live_updates), style = MaterialTheme.typography.titleMedium)
            Text(
                if (access.liveUpdates) context.getString(R.string.onboarding_allowed_on_this_phone_android_and)
                else context.getString(R.string.onboarding_not_enabled_or_not_supported_on),
                style = MaterialTheme.typography.bodyMedium,
                color = t.muted
            )
        }
        BudgetProgressNotificationPreview()
        ReminderProgressNotificationPreview()
        Text(context.getString(R.string.onboarding_if_you_join_a_circle_auto), style = MaterialTheme.typography.bodySmall, color = t.muted)
    } else {
        NotebookSurface(Modifier.fillMaxWidth()) {
            Text(context.getString(R.string.onboarding_manual_entry_is_ready), style = MaterialTheme.typography.titleMedium)
            Text(context.getString(R.string.onboarding_add_a_purchase_or_finish_a), style = MaterialTheme.typography.bodyMedium, color = t.muted)
        }
    }
}

@Composable
private fun ReadyStep(
    name: String,
    themeId: String,
    daily: Long,
    weekly: Long,
    automatic: Boolean,
    access: AutoDetectAccess,
    reminders: Boolean,
    onReminders: (Boolean) -> Unit,
    onNotifications: () -> Unit
) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    Eyebrow(context.getString(R.string.onboarding_04_ready))
    Text(context.getString(R.string.onboarding_ready_title, name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
    Text(context.getString(R.string.onboarding_here_s_what_you_chose_you), style = MaterialTheme.typography.bodyMedium, color = t.muted)
    NotebookSurface(Modifier.fillMaxWidth()) {
        SummaryLine(context.getString(R.string.onboarding_theme), context.themeLabel(AppTheme.fromId(themeId)))
        SummaryLine(context.getString(R.string.onboarding_daily_limit_ed373), com.spendingbuddy.app.rupees(daily))
        SummaryLine(context.getString(R.string.onboarding_weekly_limit_41fab), com.spendingbuddy.app.rupees(weekly))
        SummaryLine(context.getString(R.string.onboarding_logging), if (automatic) context.getString(R.string.onboarding_automatic_tracking) else context.getString(R.string.onboarding_manual_entry))
        if (automatic) {
            SummaryLine(context.getString(R.string.onboarding_access), context.getString(R.string.onboarding_source_count, listOf(access.notificationAccess, access.sms).count { it }))
            if (!access.notificationAccess && !access.sms) {
                Text(context.getString(R.string.onboarding_automatic_tracking_is_selected_but_no), style = MaterialTheme.typography.bodyMedium, color = t.muted, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (reminders && !access.canNotify) {
            Text(context.getString(R.string.onboarding_allow_notifications_to_receive_reminders), style = MaterialTheme.typography.bodyMedium, color = t.muted, modifier = Modifier.padding(top = 8.dp))
            TextButton(onClick = onNotifications, modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.onboarding_allow_notifications)) }
        }
    }
    NotebookSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(context.getString(R.string.onboarding_10_pm_finish_day_reminder), style = MaterialTheme.typography.titleMedium)
                Text(context.getString(R.string.onboarding_a_gentle_reminder_the_next_day), style = MaterialTheme.typography.bodyMedium, color = t.muted)
            }
            Switch(checked = reminders, onCheckedChange = onReminders, modifier = Modifier.semantics { contentDescription = context.getString(R.string.onboarding_enable_10_pm_finish_day_reminder) })
        }
    }
    Text(context.getString(R.string.onboarding_nothing_is_shared_with_a_circle), style = MaterialTheme.typography.bodyMedium, color = t.muted)
}

@Composable
private fun ChoiceCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (selected) t.accent.copy(alpha = if (t.dark) 0.13f else 0.08f) else t.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) t.accent else t.border, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = if (selected) t.text else t.muted)
        }
        Spacer(Modifier.width(10.dp))
        if (selected) Icon(Icons.Rounded.Check, context.getString(R.string.onboarding_selected), tint = t.accent, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun AccessSetupCard(title: String, detail: String, enabled: Boolean, action: String, onClick: () -> Unit) {
    val t = LocalPennyTokens.current
    val shape = RoundedCornerShape(24.dp)
    NotebookSurface(
        Modifier.fillMaxWidth().border(
            width = if (enabled) 2.dp else 1.dp,
            color = if (enabled) t.accent else t.border,
            shape = shape
        )
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stackAction = LocalDensity.current.fontScale >= 1.4f || maxWidth < 280.dp
            if (stackAction) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = t.muted)
                    AccessStatus(enabled, Modifier.padding(top = 2.dp))
                    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(action) }
                }
            } else Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = t.muted)
                    AccessStatus(enabled, Modifier.padding(top = 4.dp))
                }
                TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
            }
        }
    }
}

@Composable
private fun AccessStatus(enabled: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        if (enabled) Icon(Icons.Rounded.Check, null, tint = t.accent, modifier = Modifier.size(18.dp))
        Text(if (enabled) context.getString(R.string.onboarding_allowed) else context.getString(R.string.onboarding_not_set_up), style = MaterialTheme.typography.labelLarge, color = if (enabled) t.accent else t.muted)
    }
}

/** Static UI-only sample: it never creates a notification, purchase, or budget change. */
@Composable
private fun BudgetProgressNotificationPreview() {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(t.surface)
            .border(1.dp, t.border, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(context.getString(R.string.onboarding_preview_demo), style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp), color = t.accent)
        Text(context.getString(R.string.onboarding_new_payment_2_minute_update_by), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(context.getString(R.string.onboarding_today_420_of_800_used), style = MaterialTheme.typography.bodyMedium, color = t.text)
        RupeeProgressPreview(0.525f)
        Text(context.getString(R.string.onboarding_380_left_today_undo_stays_available), style = MaterialTheme.typography.bodyMedium, color = t.muted)
        Text(context.getString(R.string.onboarding_example_only_no_notification_is_sent), style = MaterialTheme.typography.bodySmall, color = t.muted)
    }
}

@Composable
private fun ReminderProgressNotificationPreview() {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    NotebookSurface(Modifier.fillMaxWidth()) {
        Text(context.getString(R.string.onboarding_preview_demo), style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp), color = t.accent)
        Text(context.getString(R.string.onboarding_your_finish_day_reminder), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(context.getString(R.string.onboarding_30_00_until_the_10_pm), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        RupeeProgressPreview(0f)
        Text(context.getString(R.string.onboarding_optional_in_settings_reminders_its_default), style = MaterialTheme.typography.bodyMedium, color = t.muted)
        Text(context.getString(R.string.onboarding_example_only_android_controls_promotion_and), style = MaterialTheme.typography.bodySmall, color = t.muted)
    }
}

/** Same original coin artwork as Android's progress tracker; the adjacent text conveys its value. */
@Composable
private fun RupeeProgressPreview(progress: Float) {
    val t = LocalPennyTokens.current
    val coin = remember(t.accent, t.onAccent) { rupeeTrackerBitmap(t.accent.toArgb(), t.onAccent.toArgb()).asImageBitmap() }
    BoxWithConstraints(Modifier.fillMaxWidth().height(32.dp)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).align(Alignment.Center).height(7.dp)
            .clip(CircleShape).background(t.border)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(t.accent, CircleShape))
        }
        Image(coin, contentDescription = null,
            modifier = Modifier.offset(x = (maxWidth - 32.dp) * progress).size(32.dp))
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    val largeText = LocalDensity.current.fontScale >= 1.4f
    if (largeText) {
        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 0.8.sp), color = LocalPennyTokens.current.muted)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    } else BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        if (maxWidth < 310.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 0.8.sp), color = LocalPennyTokens.current.muted)
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 0.8.sp), color = LocalPennyTokens.current.muted)
            Spacer(Modifier.width(12.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp), color = LocalPennyTokens.current.accent)
}
