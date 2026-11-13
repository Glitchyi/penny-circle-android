@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.spendingbuddy.app.ui

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
import com.spendingbuddy.app.rupeeTrackerBitmap
import com.spendingbuddy.app.OnboardingLimits
import com.spendingbuddy.app.onboardingLimitsError
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
    onComplete: (String, Long, Long, Boolean, Boolean) -> Unit
) {
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
            1 -> if (name.trim().isBlank()) error = "Add a name for your notebook."
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
        bottomBar = {
            Column(
                Modifier.fillMaxWidth().background(tokens.bg.copy(alpha = 0.96f)).imePadding().navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (error != null) Text(
                    error.orEmpty(), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
                )
                if (largeText) {
                    if (step > 0) TextButton(onClick = ::goBack, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Back") }
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
                        Text(if (step == ONBOARDING_STEPS - 1) "Finish setup" else if (step == 0) "Get started" else if (step == 3 && needsSource) "Connect payment alerts" else "Continue", fontWeight = FontWeight.Bold)
                    }
                } else Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (step > 0) TextButton(onClick = ::goBack, modifier = Modifier.heightIn(min = 52.dp)) { Text("Back") }
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
                        Text(if (step == ONBOARDING_STEPS - 1) "Finish setup" else if (step == 0) "Get started" else if (step == 3 && needsSource) "Connect payment alerts" else "Continue", fontWeight = FontWeight.Bold)
                    }
                }
            if (step == 3 && needsSource) TextButton(
                onClick = { automaticTracking = false; advance() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Use manual entry for now") }
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
                    Text("PENNY CIRCLE", style = (if (largeText) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge).copy(fontFamily = PennyType.Mono, letterSpacing = 1.2.sp), color = tokens.text, maxLines = if (largeText) 2 else 1)
                    if (!largeText) Text("YOUR SHARED NOTEBOOK", style = MaterialTheme.typography.labelSmall.copy(fontFamily = PennyType.Mono, letterSpacing = 0.8.sp), color = tokens.muted, maxLines = 1)
                }
                Text("${step + 1} / $ONBOARDING_STEPS", style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono), color = tokens.muted)
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
                            name = name.trim().ifBlank { "You" },
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
    val t = LocalPennyTokens.current
    Eyebrow("A FRIENDLY DAILY CHECK-IN")
    Text("A clearer day,\nshared with your circle.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
    Text(
        "Penny Circle is a small shared notebook for purchases, no-spend days and encouragement from people you trust.",
        style = MaterialTheme.typography.bodyLarge,
        color = t.muted
    )
    NotebookSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(CircleShape).background(Brush.linearGradient(listOf(t.accent.copy(alpha = 0.28f), t.accent2.copy(alpha = 0.18f)))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.AutoAwesome, null, tint = t.accent, modifier = Modifier.size(27.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("TODAY", style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.2.sp), color = t.muted)
                Text("One honest day at a time", style = MaterialTheme.typography.titleMedium, color = t.text)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { index, day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(if (index == 4) 30.dp else 24.dp).border(if (index == 4) 3.dp else 2.dp, if (index == 4) t.accent else t.border, CircleShape))
                    Text(day, style = MaterialTheme.typography.labelSmall.copy(fontFamily = PennyType.Mono), color = if (index == 4) t.text else t.muted)
                }
            }
        }
    }
    Text("Your expenses stay on this phone until you choose to share them with a circle.", style = MaterialTheme.typography.bodyMedium, color = t.muted)
    Text("No account to create. No finance dashboard to manage. Just a notebook that helps you stay honest and supported.", style = MaterialTheme.typography.bodyMedium, color = t.muted)
}

@Composable
private fun ProfileStep(name: String, error: String?, themeId: String, onName: (String) -> Unit, onTheme: (String) -> Unit, onNameDone: () -> Unit) {
    Eyebrow("01 / MAKE IT YOURS")
    Text("What should your circle call you?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
    Text("Choose a name and a look. You can change both later.", style = MaterialTheme.typography.bodyMedium, color = LocalPennyTokens.current.muted)
    OutlinedTextField(
        value = name,
        onValueChange = onName,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Your name") },
        isError = error != null,
        supportingText = { Text("Up to 40 characters") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onNameDone() })
    )
    Text("PICK A THEME", style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp), color = LocalPennyTokens.current.muted)
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
    val t = LocalPennyTokens.current
    val presets = listOf(300L, 500L, 800L, 1_000L)
    Eyebrow("02 / SET YOUR LIMITS")
    Text("Keep the daily and weekly targets yours.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
    Text("Start with a suggestion or enter any amount. Weekly limits run Monday through Sunday.", style = MaterialTheme.typography.bodyMedium, color = t.muted)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        presets.forEach { rupee ->
            val presetWeekly = rupee * 7L
            val selected = com.spendingbuddy.app.amountToPaise(daily) == rupee * 100L && com.spendingbuddy.app.amountToPaise(weekly) == presetWeekly * 100L
            FilterChip(
                selected = selected,
                onClick = { onDaily(rupee.toString()); onWeekly(presetWeekly.toString()) },
                label = { Text("₹${rupee} / ₹${presetWeekly}") }
            )
        }
    }
    OutlinedTextField(
        value = daily,
        onValueChange = onDaily,
        modifier = Modifier.fillMaxWidth().focusRequester(dailyFocus),
        label = { Text("Daily limit (₹)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNextField() }),
        isError = error != null
    )
    OutlinedTextField(
        value = weekly,
        onValueChange = onWeekly,
        modifier = Modifier.fillMaxWidth().focusRequester(weeklyFocus),
        label = { Text("Weekly limit (₹)") },
        supportingText = { Text("Set the limit for the full Monday–Sunday week.") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        isError = error != null
    )
    if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite })
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
    val t = LocalPennyTokens.current
    Eyebrow("03 / CHOOSE HOW TO LOG")
    Text("Let the phone help, or keep it manual.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
    Text("You can change this any time in Settings.", style = MaterialTheme.typography.bodyMedium, color = t.muted)
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChoiceCard(
            title = "Automatic tracking",
            subtitle = "Read supported payment alerts on this phone and add clear shop payments with a 10-minute undo window.",
            selected = automatic,
            onClick = { onAutomatic(true) }
        )
        ChoiceCard(
            title = "Manual entry",
            subtitle = "Add purchases yourself from Today. You can set up automatic tracking later.",
            selected = !automatic,
            onClick = { onAutomatic(false) }
        )
    }
    if (automatic) {
        Text("SET UP ACCESS", style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp), color = t.muted)
        Text("Each button opens only after you tap it. New messages are parsed on-device; their text is discarded, never stored or sent.", style = MaterialTheme.typography.bodyMedium, color = t.muted)
        val allowedCount = listOf(access.notificationAccess, access.sms, access.canNotify).count { it }
        NotebookSurface(Modifier.fillMaxWidth()) {
            Text("ACCESS STATUS", style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp), color = t.muted)
            Text("$allowedCount of 3 permissions allowed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    access.notificationAccess || access.sms -> "At least one payment source is ready."
                    else -> "Allow a payment source to enable automatic detection."
                } + if (access.canNotify) " Purchase notices are allowed." else " Purchase notices are off.",
                style = MaterialTheme.typography.bodyMedium,
                color = t.muted
            )
        }
        AccessSetupCard(
            title = "Payment and bank app alerts",
            detail = "Needed to notice payments from supported apps.",
            enabled = access.notificationAccess,
            action = if (access.notificationAccess) "Review access" else "Allow access",
            onClick = onNotificationAccess
        )
        AccessSetupCard(
            title = "Bank SMS",
            detail = "Reads new SMS alerts only; there is no scan of old messages.",
            enabled = access.sms,
            action = if (access.sms) "Review permission" else "Allow SMS",
            onClick = onSms
        )
        AccessSetupCard(
            title = "Purchase notifications",
            detail = "Shows an undo or review notice when a payment is detected.",
            enabled = access.canNotify,
            action = if (access.canNotify) "Review permission" else "Allow notifications",
            onClick = onNotifications
        )
        NotebookSurface(Modifier.fillMaxWidth()) {
            Text("Optional Android Live Updates", style = MaterialTheme.typography.titleMedium)
            Text(
                if (access.liveUpdates) "Allowed on this phone. Android and device support can still affect whether an update is promoted."
                else "Not enabled or not supported on this phone. Payment tracking and ordinary notifications can still work.",
                style = MaterialTheme.typography.bodyMedium,
                color = t.muted
            )
        }
        BudgetProgressNotificationPreview()
        ReminderProgressNotificationPreview()
        Text("If you join a circle, auto-added purchases stay on this phone during the undo window, then can sync. Android may delay background work.", style = MaterialTheme.typography.bodySmall, color = t.muted)
    } else {
        NotebookSurface(Modifier.fillMaxWidth()) {
            Text("Manual entry is ready", style = MaterialTheme.typography.titleMedium)
            Text("Add a purchase or finish a no-spend day from Today. Automatic tracking remains off until you turn it on.", style = MaterialTheme.typography.bodyMedium, color = t.muted)
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
    val t = LocalPennyTokens.current
    Eyebrow("04 / READY")
    Text("Your notebook is ready, $name.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
    Text("Here’s what you chose. You can edit these in Settings later.", style = MaterialTheme.typography.bodyMedium, color = t.muted)
    NotebookSurface(Modifier.fillMaxWidth()) {
        SummaryLine("THEME", AppTheme.fromId(themeId).label)
        SummaryLine("DAILY LIMIT", com.spendingbuddy.app.rupees(daily))
        SummaryLine("WEEKLY LIMIT", com.spendingbuddy.app.rupees(weekly))
        SummaryLine("LOGGING", if (automatic) "Automatic tracking" else "Manual entry")
        if (automatic) {
            SummaryLine("ACCESS", "${listOf(access.notificationAccess, access.sms).count { it }} of 2 sources allowed")
            if (!access.notificationAccess && !access.sms) {
                Text("Automatic tracking is selected, but no message source is allowed yet. You can finish setup and allow one later; manual entry remains available.", style = MaterialTheme.typography.bodyMedium, color = t.muted, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (reminders && !access.canNotify) {
            Text("Allow notifications to receive reminders.", style = MaterialTheme.typography.bodyMedium, color = t.muted, modifier = Modifier.padding(top = 8.dp))
            TextButton(onClick = onNotifications, modifier = Modifier.heightIn(min = 48.dp)) { Text("Allow notifications") }
        }
    }
    NotebookSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("10 PM finish-day reminder", style = MaterialTheme.typography.titleMedium)
                Text("A gentle reminder; the next-day grace window ends at 10 AM IST. Android delivery timing is approximate.", style = MaterialTheme.typography.bodyMedium, color = t.muted)
            }
            Switch(checked = reminders, onCheckedChange = onReminders, modifier = Modifier.semantics { contentDescription = "Enable 10 PM finish-day reminder" })
        }
    }
    Text("Nothing is shared with a circle until you join one.", style = MaterialTheme.typography.bodyMedium, color = t.muted)
}

@Composable
private fun ChoiceCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
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
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = t.muted)
        }
        Spacer(Modifier.width(10.dp))
        if (selected) Icon(Icons.Rounded.Check, "Selected", tint = t.accent, modifier = Modifier.size(22.dp))
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
    val t = LocalPennyTokens.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        if (enabled) Icon(Icons.Rounded.Check, null, tint = t.accent, modifier = Modifier.size(18.dp))
        Text(if (enabled) "Allowed" else "Not set up", style = MaterialTheme.typography.labelLarge, color = if (enabled) t.accent else t.muted)
    }
}

/** Static UI-only sample: it never creates a notification, purchase, or budget change. */
@Composable
private fun BudgetProgressNotificationPreview() {
    val t = LocalPennyTokens.current
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(t.surface)
            .border(1.dp, t.border, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text("PREVIEW / DEMO", style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp), color = t.accent)
        Text("New payment · 2-minute update by default", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Today · ₹420 of ₹800 used", style = MaterialTheme.typography.bodyMedium, color = t.text)
        RupeeProgressPreview(0.525f)
        Text("₹380 left today. Undo stays available in Today for 10 minutes; the purchase stays private during that window.", style = MaterialTheme.typography.bodyMedium, color = t.muted)
        Text("Example only. No notification is sent from this preview.", style = MaterialTheme.typography.bodySmall, color = t.muted)
    }
}

@Composable
private fun ReminderProgressNotificationPreview() {
    val t = LocalPennyTokens.current
    NotebookSurface(Modifier.fillMaxWidth()) {
        Text("PREVIEW / DEMO", style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp), color = t.accent)
        Text("Your finish-day reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("30:00 until the 10 PM reminder", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        RupeeProgressPreview(0f)
        Text("Optional in Settings → Reminders. Its default 30-minute lead can be changed to 15 or 60 minutes. It stops when you finish; late logging is still available until 10 AM the next day.", style = MaterialTheme.typography.bodyMedium, color = t.muted)
        Text("Example only. Android controls promotion and background timing.", style = MaterialTheme.typography.bodySmall, color = t.muted)
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

private fun formatLimitInput(paise: Long): String {
    val value = paise.coerceAtLeast(0L)
    val whole = value / 100L
    val fraction = (value % 100L).toString().padStart(2, '0').trimEnd('0')
    return if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
}
