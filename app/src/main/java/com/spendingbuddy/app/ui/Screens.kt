@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.spendingbuddy.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.spendingbuddy.app.R
import android.view.HapticFeedbackConstants
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendingbuddy.app.*
import com.spendingbuddy.app.p2p.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    state: BuddyState,
    now: ZonedDateTime,
    bottomBar: @Composable () -> Unit,
    onAdd: () -> Unit,
    onFinish: (String) -> Unit,
    onFinishNow: (String) -> Unit,
    onSettings: () -> Unit,
    onFinishYesterday: () -> Unit,
    snackbar: SnackbarHostState? = null,
    onPurchase: ((Expense) -> Unit)? = null,
    auto: AutoActions? = null,
    autoAccessReady: Boolean = true
) {
    val date = now.toLocalDate()
    val spent = spentOn(state.expenses, date)
    val weekSpent = spentInWeek(state.expenses, date)
    val status = dayStatus(date, state.checkIns, now)
    val todayExpenses = state.expenses.filter { it.date == date.toString() }.sortedByDescending { it.updatedAt }
    val streakCount = streak(state.checkIns, now)
    // Remember (across rotation) that this date was seen unfinished, so the stamp only thumps on a real transition.
    var openSeenOn by rememberSaveable { mutableStateOf("") }
    if (status != DayStatus.Done && status != DayStatus.Grace && status != DayStatus.Late) SideEffect { openSeenOn = date.toString() }
    val finished = status == DayStatus.Done || status == DayStatus.Grace || status == DayStatus.Late
    var rocketPlayedOn by rememberSaveable { mutableStateOf("") }
    var launching by remember { mutableStateOf(false) }
    LaunchedEffect(finished, openSeenOn) {
        if (finished && openSeenOn == date.toString() && rocketPlayedOn != date.toString()) {
            rocketPlayedOn = date.toString()
            launching = true
        }
    }
    val tokens = LocalPennyTokens.current
    val view = LocalView.current
    val reduced = LocalReducedMotion.current
    val configuration = LocalConfiguration.current
    val compact = isCompactLayout(configuration.screenHeightDp, LocalDensity.current.fontScale)
    val gap = if (compact) 8.dp else 12.dp
    val gapLarge = if (compact) 8.dp else 16.dp
    val labelStyle = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.6.sp)
    val firstName = state.name.trim().substringBefore(' ')
    val greeting = if (firstName.isBlank() || firstName.equals("You", ignoreCase = true)) null else stringResource(R.string.purchase_today_greeting, firstName)
    val openSettingsLabel = stringResource(R.string.purchase_open_settings)
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        topBar = {
            Row(
                Modifier.statusBarsPadding().padding(horizontal = 18.dp).padding(top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(date.format(dateFormatter), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.purchase_today),
                        style = MaterialTheme.typography.displayMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                    if (greeting != null) Text(greeting, style = MaterialTheme.typography.bodyLarge)
                }
                val settingsPress = remember { MutableInteractionSource() }
                Box(
                    Modifier.size(48.dp).pressScale(settingsPress, pressed = 0.9f).glass(CircleShape, shadow = false)
                        .clickable(interactionSource = settingsPress, indication = LocalIndication.current, role = Role.Button, onClickLabel = openSettingsLabel, onClick = onSettings),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Settings, stringResource(R.string.purchase_settings), Modifier.size(22.dp)) }
            }
        },
        bottomBar = bottomBar,
        // With automatic tracking on, typing a purchase in is the fallback, so its button is smaller.
        floatingActionButtonPosition = if (state.autoDetect) FabPosition.End else FabPosition.Center,
        floatingActionButton = {
            if (state.autoDetect) SecondaryButton(onClick = onAdd) {
                Icon(PennyIcons.Add, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.purchase_add_by_hand))
            } else AccentButton(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                haptics = state.haptics
            ) {
                Icon(PennyIcons.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.purchase_add))
            }
        }
    ) { padding ->
        val todayListState = rememberLazyListState()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = MAX_CONTENT_WIDTH_DP.dp).fillMaxSize().padding(padding).scrollFadingEdges(todayListState, top = 28.dp, bottom = 104.dp),
            state = todayListState,
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val incomingNudge = activeNudge(state)
            if (incomingNudge != null) item { NudgeBanner(incomingNudge, now, onFinish, onAdd) }
            if (auto != null && !state.autoDetect && !state.autoDetectOffered) item {
                AutoPromptCard(onTurnOn = auto.setUp, onNotNow = auto.notNow, modifier = Modifier.enterRise(0))
            }
            if (auto != null && state.autoDetect && !autoAccessReady) item {
                NotebookSurface(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.purchase_connect_alerts), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.purchase_source_missing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = auto.setUp, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.purchase_finish_tracking_setup))
                    }
                }
            }
            if (auto != null) {
                val nowMs = System.currentTimeMillis()
                items(state.pendingDetections.asReversed(), key = { "pending-${it.id}" }) { p ->
                    PendingDetectionCard(p, onAdd = { auto.addPending(p.id) }, onSkip = { auto.skipPending(p.id) }, modifier = Modifier.popIn())
                }
                items(heldExpenses(state, nowMs), key = { "held-${it.id}" }) { e ->
                    HeldPurchaseCard(e, inCircle = state.group != null, minutesLeft = (e.heldUntil - nowMs + 59_999) / 60_000, onUndo = { auto.undo(e.id) }, onKeep = { auto.keep(e.id) }, modifier = Modifier.popIn())
                }
            }
            val yesterday = date.minusDays(1)
            if (eligibleCheckIn(yesterday, now) && state.checkIns.none { it.date == yesterday.toString() }) {
                item {
                    NotebookSurface(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.purchase_yesterday_open), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.purchase_yesterday_grace), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onFinishYesterday, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(stringResource(R.string.purchase_finish_yesterday))
                        }
                    }
                }
            }
            item {
                val weekOver = weekSpent > state.weeklyLimitPaise
                val impulse = impulseStats(state.expenses, date)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    TodayHero(
                        spent = spent,
                        limit = state.dailyLimitPaise,
                        days = weekDays(state, date),
                        streak = streakCount,
                        compact = compact,
                        modifier = Modifier.fillMaxWidth().enterRise(0)
                    )
                    val cleanValue = pluralStringResource(R.plurals.purchase_impulse_free_days, impulse.freeDays, impulse.freeDays)
                    val largeText = LocalDensity.current.fontScale >= 1.3f
                    val cleanCaption = when {
                        largeText && impulse.weekCount > 0 -> stringResource(R.string.purchase_impulse_short, rupees(impulse.weekPaise))
                        largeText -> stringResource(R.string.purchase_no_impulse_short)
                        impulse.weekCount > 0 -> stringResource(R.string.purchase_impulse_week, rupees(impulse.weekPaise))
                        else -> stringResource(R.string.purchase_no_impulse_week)
                    }
                    if (LocalDensity.current.fontScale > COMPACT_FONT_SCALE) {
                        Column(Modifier.fillMaxWidth().padding(top = 12.dp).fadeInContent(100), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile(stringResource(R.string.purchase_this_week), rupees(weekSpent), stringResource(R.string.purchase_of_limit, rupees(state.weeklyLimitPaise)), Modifier.fillMaxWidth(), progress = budgetIntensity(weekSpent, state.weeklyLimitPaise), over = weekOver)
                            StatTile(stringResource(R.string.purchase_impulse_free), cleanValue, cleanCaption, Modifier.fillMaxWidth(), over = impulse.weekCount > 0 && impulse.freeDays == 0)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp).height(IntrinsicSize.Min).enterRise(1), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile(stringResource(R.string.purchase_this_week), rupees(weekSpent), stringResource(R.string.purchase_of_limit, rupees(state.weeklyLimitPaise)), Modifier.weight(1f).fillMaxHeight(), progress = budgetIntensity(weekSpent, state.weeklyLimitPaise), over = weekOver)
                            StatTile(stringResource(R.string.purchase_impulse_free), cleanValue, cleanCaption, Modifier.weight(1f).fillMaxHeight(), over = impulse.weekCount > 0 && impulse.freeDays == 0)
                        }
                    }
                    if (status == DayStatus.Done || status == DayStatus.Grace || status == DayStatus.Late) {
                        Spacer(Modifier.height(gapLarge))
                        FinishStamp(
                            date = date.toString(),
                            finishedAt = state.checkIns.firstOrNull { it.date == date.toString() }?.completedAt,
                            animate = openSeenOn == date.toString(),
                            haptics = state.haptics
                        )
                    } else {
                        SwipeToFinish(
                            onSwiped = { onFinishNow(date.toString()) },
                            onTap = { onFinish(date.toString()) },
                            haptics = state.haptics,
                            modifier = Modifier.padding(top = 14.dp).enterRise(1)
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp).enterRise(2), verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.purchase_list_heading), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
                    Text(
                        pluralStringResource(R.plurals.purchase_item_count, todayExpenses.size, todayExpenses.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (todayExpenses.isEmpty()) item { EmptyState(Modifier.enterRise(3), watching = state.autoDetect) }
            else item {
                Column(
                    // The size animation sits inside the glass, because it clips and would cut off the shadow.
                    Modifier.fillMaxWidth().enterRise(3).glass(RoundedCornerShape(24.dp)).then(if (reduced) Modifier else Modifier.animateContentSize())
                ) {
                    todayExpenses.forEachIndexed { index, e ->
                        key(e.id) {
                            // Rows rise in one after another; a purchase added later rises in on its own.
                            PurchaseRow(e, modifier = Modifier.enterRise(4 + index.coerceAtMost(4)), flat = true, onClick = onPurchase?.let { open -> { open(e) } })
                            if (index < todayExpenses.lastIndex) HorizontalDivider(thickness = 1.dp, color = tokens.border)
                        }
                    }
                }
            }
        }
        }
    }
    RocketCelebration(play = launching, haptics = state.haptics, onFinished = { launching = false }, modifier = Modifier.fillMaxSize())
    }
}

/** What the Today screen can do with automatic tracking. */
class AutoActions(
    val setUp: () -> Unit,
    val notNow: () -> Unit,
    val undo: (String) -> Unit,
    val keep: (String) -> Unit,
    val addPending: (String) -> Unit,
    val skipPending: (String) -> Unit
)

@Composable
private fun NudgeBanner(nudge: Nudge, now: ZonedDateTime, onFinish: (String) -> Unit, onAdd: () -> Unit) {
    val sender = nudge.sender.ifBlank { stringResource(R.string.purchase_circle_friend) }
    NotebookSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.purchase_nudge_title, sender), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(stringResource(R.string.purchase_nudge_detail), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onAdd, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.purchase_add)) }
            Button(
                onClick = { onFinish(now.toLocalDate().toString()) },
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.purchase_finish_today)) }
        }
    }
}

@Composable
fun AddSheet(state: BuddyState, initial: Expense? = null, onDismiss: () -> Unit, onSave: suspend (Expense) -> String?, onSavePreset: suspend (name: String, category: String, reason: String) -> String?) {
    val defaultPreset = if (initial == null) state.presets.firstOrNull { it.isDefault } else null
    var step by rememberSaveable { mutableIntStateOf(if (initial == null) 0 else 1) }
    val expenseId by rememberSaveable { mutableStateOf(initial?.id ?: java.util.UUID.randomUUID().toString()) }
    var amount by rememberSaveable { mutableStateOf(initial?.amountPaise?.let(::formatLimitInput) ?: "") }
    var category by rememberSaveable { mutableStateOf(initial?.category ?: defaultPreset?.category ?: state.lastCategory) }
    var reason by rememberSaveable { mutableStateOf(initial?.reason ?: defaultPreset?.reason ?: "") }
    var merchant by rememberSaveable { mutableStateOf(initial?.merchant ?: defaultPreset?.name ?: "") }
    var saveAsPreset by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var date by rememberSaveable { mutableStateOf(initial?.date ?: todayIndia().toString()) }
    var dial by rememberSaveable { mutableStateOf(state.amountInput == "dial") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var presetError by rememberSaveable { mutableStateOf<String?>(null) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val amountErrorText = stringResource(R.string.purchase_error_amount)
    val rawFields = if (submitted) purchaseFieldErrors(amount, category, reason, date) else PurchaseFieldErrors()
    val fields = PurchaseFieldErrors(
        amount = rawFields.amount?.let { stringResource(R.string.purchase_error_amount) },
        category = rawFields.category?.let { stringResource(R.string.purchase_error_category) },
        reason = rawFields.reason?.let { stringResource(R.string.purchase_error_reason) },
        date = rawFields.date?.let { stringResource(R.string.purchase_error_date) }
    )
    fun back() { if (!busy) { if (step == 1) step = 0 else onDismiss() } }
    ModalBottomSheet(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { !busy || it != SheetValue.Hidden })
    ) {
        DialogSystemBars()
        ModalStepBackHandler { back() }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (initial == null) R.string.purchase_add else R.string.purchase_edit), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                StepBars(step, 2)
            }
            RequiredFieldsHint()
            Spacer(Modifier.height(8.dp))
            val reducedMotion = LocalReducedMotion.current
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (reducedMotion) EnterTransition.None togetherWith ExitTransition.None
                    else {
                        val forward = targetState > initialState
                        (slideInHorizontally(PennyMotion.spatial()) { if (forward) it / 3 else -it / 3 } + fadeIn(PennyMotion.effects())) togetherWith
                            (slideOutHorizontally(PennyMotion.spatial()) { if (forward) -it / 3 else it / 3 } + fadeOut(PennyMotion.effects()))
                    }
                },
                label = "addStep"
            ) { shownStep ->
            Column {
            if (shownStep == 0) {
                AmountStep(amount, { amount = it; error = null }, dial, { dial = it }, state.haptics)
                ErrorText(error ?: fields.amount)
                Spacer(Modifier.height(12.dp))
                AccentButton(
                    onClick = { if (amountToPaise(amount) == null || amountToPaise(amount) == 0L) error = amountErrorText else { error = null; step = 1 } },
                    modifier = Modifier.fillMaxWidth(),
                    haptics = state.haptics
                ) { Text(stringResource(R.string.purchase_details_next)) }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.purchase_amount_summary, amountToPaise(amount)?.let(::rupees) ?: amount), modifier = Modifier.weight(1f).semantics { fields.amount?.let { error(it) } }, style = MaterialTheme.typography.titleMedium)
                    TextButton(enabled = !busy, onClick = { step = 0 }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.purchase_change_amount)) }
                }
                ErrorText(fields.amount)
                DetailsStep(state, category, { category = it; presetError = null }, reason, { reason = it; presetError = null }, merchant, { merchant = it; presetError = null }, date, { date = it }, note, { note = it }, saveAsPreset, { saveAsPreset = it; presetError = null }, fields, presetError, !busy)
                ErrorText(error)
                Spacer(Modifier.height(16.dp))
                AccentButton(onClick = {
                    submitted = true
                    error = null
                    val invalid = purchaseFieldErrors(amount, category, reason, date)
                    presetError = if (saveAsPreset && merchant.isNotBlank() && !hasPresetNamed(state.presets, merchant)) validatePreset(state.presets, merchant, category, reason) else null
                    if (invalid.first == null && presetError == null) scope.launch {
                        busy = true
                        try {
                            if (saveAsPreset && merchant.isNotBlank() && !hasPresetNamed(state.presets, merchant)) {
                                presetError = onSavePreset(merchant, category, reason)
                                if (presetError != null) return@launch
                            }
                            // Stable id makes a retry an upsert, including after cancellation/rotation.
                            error = onSave((initial ?: Expense(id = expenseId)).copy(date = date, amountPaise = requireNotNull(amountToPaise(amount)), merchant = merchant, category = category, reason = reason, note = note))
                            if (error == null) onDismiss()
                        } finally { busy = false }
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth(), haptics = state.haptics) { Text(stringResource(R.string.purchase_save)) }
            }
            }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun AmountStep(amount: String, onAmount: (String) -> Unit, dial: Boolean, onDial: (Boolean) -> Unit, haptics: Boolean) {
    AmountDisplay(amount)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        SegmentedToggle(listOf(stringResource(R.string.purchase_keypad), stringResource(R.string.purchase_dial)), if (dial) 1 else 0, { onDial(it == 1) }, Modifier.width(200.dp))
    }
    if (dial) RotaryAmountInput(value = amount, onValueChange = onAmount, haptics = haptics, onUseKeypad = { onDial(false) })
    else Keypad(amount, onAmount, haptics)
}

@Composable
private fun DetailsStep(state: BuddyState, category: String, onCategory: (String) -> Unit, reason: String, onReason: (String) -> Unit, merchant: String, onMerchant: (String) -> Unit, date: String, onDate: (String) -> Unit, note: String, onNote: (String) -> Unit, saveAsPreset: Boolean, onSaveAsPreset: (Boolean) -> Unit, errors: PurchaseFieldErrors, presetError: String?, enabled: Boolean) {
    val presetMessage = presetValidationMessage(presetError)
    if (state.presets.isNotEmpty()) {
        Text(stringResource(R.string.purchase_presets), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.presets.forEach { p ->
                Pill(if (p.isDefault) stringResource(R.string.purchase_default_preset, p.name) else p.name, selected = merchant.trim().equals(p.name, ignoreCase = true), onClick = if (enabled) ({ onMerchant(p.name); onCategory(p.category); onReason(p.reason) }) else null)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    Text(stringResource(R.string.purchase_category), modifier = Modifier.semantics { errors.category?.let { error(it) } }, style = MaterialTheme.typography.titleMedium)
    LazyRowChips(state.categories, category, onCategory, enabled)
    ErrorText(errors.category)
    Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.purchase_reason), modifier = Modifier.semantics { errors.reason?.let { error(it) } }, style = MaterialTheme.typography.titleMedium)
    LazyRowChips(state.reasons, reason, onReason, enabled)
    ErrorText(errors.reason)
    if (reason == IMPULSE) Text(stringResource(R.string.purchase_impulse_notice), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
    OutlinedTextField(merchant, onMerchant, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.purchase_merchant_optional)) }, singleLine = true, enabled = enabled,
        isError = presetMessage != null, supportingText = presetMessage?.let { message -> { Text(message) } })
    if (merchant.isNotBlank() && !hasPresetNamed(state.presets, merchant) && state.presets.size < MAX_PRESETS) {
        Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).toggleable(saveAsPreset, enabled = enabled, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onSaveAsPreset), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.purchase_save_preset, merchant.trim()), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(saveAsPreset, null, enabled = enabled)
        }
    }
    if (state.name.isNotBlank()) Spacer(Modifier.height(6.dp))
    Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.purchase_when), style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Pill(stringResource(R.string.purchase_today), date == todayIndia().toString(),onClick = if (enabled) ({ onDate(todayIndia().toString()) }) else null); Pill(stringResource(R.string.purchase_yesterday), date == todayIndia().minusDays(1).toString(), onClick = if (enabled) ({ onDate(todayIndia().minusDays(1).toString()) }) else null) }
    OutlinedTextField(date, onDate, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.purchase_date_label)) }, singleLine = true, enabled = enabled,
        isError = errors.date != null, supportingText = errors.date?.let { message -> { Text(message) } })
    OutlinedTextField(note, onNote, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text(stringResource(R.string.purchase_note_optional)) }, singleLine = true, enabled = enabled)
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun LazyRowChips(values: List<String>, selected: String, onSelected: (String) -> Unit, enabled: Boolean = true) { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { values.forEach { Pill(it, it == selected, onClick = if (enabled) ({ onSelected(it) }) else null) } } }

@Composable
fun FinishSheet(state: BuddyState, date: String, onConfirm: suspend () -> String?, onAdd: () -> Unit, onDismiss: () -> Unit) {
    var error by rememberSaveable(date) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val localDate = runCatching { LocalDate.parse(date) }.getOrDefault(todayIndia()); val purchases = state.expenses.filter { it.date == date }; val total = netSpending(purchases); val impulses = purchases.count { it.reason == IMPULSE }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() },
        sheetState = rememberModalBottomSheetState(confirmValueChange = { !busy || it != SheetValue.Hidden })) { DialogSystemBars(); Column(Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.purchase_finish_title, localDate.format(dateFormatter)), style = MaterialTheme.typography.headlineSmall)
        Text(pluralStringResource(R.plurals.purchase_finish_summary, purchases.size, purchases.size, rupees(total), pluralStringResource(R.plurals.purchase_impulse_count, impulses, impulses)), style = MaterialTheme.typography.bodyLarge)
        Text(if (purchases.isEmpty()) stringResource(R.string.purchase_no_spend_question) else if (total <= state.dailyLimitPaise) stringResource(R.string.purchase_under_limit, rupees(state.dailyLimitPaise - total)) else stringResource(R.string.purchase_over_limit, rupees(total - state.dailyLimitPaise)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 14.dp))
        ErrorText(error)
        AccentButton(enabled = !busy, onClick = { scope.launch {
            busy = true
            try { error = onConfirm(); if (error == null) onDismiss() } finally { busy = false }
        } }, modifier = Modifier.fillMaxWidth(), haptics = state.haptics) { Text(stringResource(if (purchases.isEmpty()) R.string.purchase_no_spend_action else if (localDate == todayIndia()) R.string.purchase_finish_today else R.string.purchase_finish_day)) }
        Spacer(Modifier.height(10.dp))
        SecondaryButton(onClick = { if (!busy) onAdd() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.purchase_add_missed)) }
        Spacer(Modifier.height(12.dp))
    } }
}

@Composable
fun ActivityScreen(state: BuddyState, now: ZonedDateTime, bottomBar: @Composable () -> Unit, onAdd: () -> Unit, snackbar: SnackbarHostState? = null, onPurchase: ((Expense) -> Unit)? = null) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { ScreenTitle(stringResource(R.string.shell_activity)) }, bottomBar = bottomBar,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        floatingActionButton = {
            AccentButton(onClick = onAdd, haptics = state.haptics) {
                Icon(PennyIcons.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.purchase_add))
            }
        }
    ) { pad -> ActivityPager(state, now, Modifier.padding(pad), onPurchase) }
}

@Composable
fun ImportScreen(
    state: BuddyState,
    onBack: () -> Unit,
    onAddMore: () -> Unit,
    onRetry: (String) -> Unit,
    onRemove: suspend (String) -> String?,
    onUpdate: suspend (ImportCandidate) -> String?,
    onSave: suspend () -> String?,
    onDiscard: suspend () -> String?,
    onAddByHand: () -> Unit,
    snackbar: SnackbarHostState? = null
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var drafts by rememberSaveable(stateSaver = ImportCandidateDraftSaver) { mutableStateOf(state.importCandidates) }
    var showValidation by rememberSaveable { mutableStateOf(false) }
    var operationError by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.importCandidates) {
        // Import may append/remove image candidates while the owner reviews another screenshot.
        drafts = mergeImportReviewDrafts(drafts, state.importCandidates)
    }
    val noSelectionMessage = stringResource(R.string.import_no_selection)
    fun back() { if (!busy) { if (step == 1) step = 0 else onBack() } }
    BackHandler { back() }
    val reducedMotion = LocalReducedMotion.current
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        topBar = { ScreenTitle(stringResource(R.string.import_review_title), ::back) },
        bottomBar = {
            if (step == 1) Surface(shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding().imePadding()) {
                    ErrorText(operationError)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(enabled = !busy, onClick = { scope.launch {
                        busy = true
                        try { operationError = onDiscard() } finally { busy = false }
                    } }) { Text(stringResource(R.string.import_discard)) }
                    Button(enabled = !busy, onClick = {
                        showValidation = true
                        val validationProblem = importReviewError(drafts)
                        operationError = if (drafts.none { it.selected }) noSelectionMessage else null
                        if (validationProblem == null) scope.launch {
                            busy = true
                            try {
                                for (candidate in drafts) {
                                    val problem = onUpdate(candidate)
                                    if (problem != null) { operationError = problem; return@launch }
                                }
                                operationError = onSave()
                            } finally { busy = false }
                        }
                    }, modifier = Modifier.weight(1f)) { Text(pluralStringResource(R.plurals.import_save_purchases, drafts.count { it.selected }, drafts.count { it.selected })) }
                    }
                }
            }
        }
    ) { pad ->
        val reviewListState = rememberLazyListState()
        LazyColumn(Modifier.fillMaxSize().padding(pad).scrollFadingEdges(reviewListState, bottom = 40.dp), state = reviewListState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(R.string.import_step, step + 1), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(if (step == 0) R.string.import_screenshots else R.string.import_review_purchases), style = MaterialTheme.typography.headlineSmall)
            }
            if (step == 0) {
                itemsIndexed(state.importImages, key = { _, image -> image.id }) { index, image ->
                    val processing = image.status == "processing"
                    NotebookSurface(Modifier.fillMaxWidth().fadeInContent((index * 45).coerceAtMost(180))) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        ) {
                            Text(
                                when (image.status) {
                                    "processing" -> stringResource(R.string.import_reading)
                                    "ready", "done" -> stringResource(R.string.import_ready)
                                    "failed" -> stringResource(R.string.import_failed, localizedImportStatusError(image.error))
                                    else -> stringResource(R.string.import_waiting)
                                }, Modifier.weight(1f)
                            )
                            if (processing && !reducedMotion) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else if (processing) Icon(Icons.Rounded.AutoAwesome, contentDescription = stringResource(R.string.import_reading_description), modifier = Modifier.size(18.dp))
                            if (image.status == "failed") TextButton(onClick = { onRetry(image.id) }) { Text(stringResource(R.string.import_retry)) }
                            TextButton(enabled = !busy, onClick = { scope.launch {
                                busy = true
                                try { operationError = onRemove(image.id) } finally { busy = false }
                            } }) { Text(stringResource(R.string.import_remove)) }
                        }
                    }
                }
                item { OutlinedButton(onClick = onAddMore, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.import_add_more)) } }
                if (state.importImages.isNotEmpty() && state.importImages.all { it.status == "ready" || it.status == "done" } && drafts.isEmpty()) {
                    item {
                        Text(stringResource(R.string.import_no_results), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.import_no_results_body), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onAddByHand, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.purchase_add_by_hand)) }
                    }
                }
                item { ErrorText(operationError) }
                item {
                    Button(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy && state.importImages.none { it.status == "processing" } && drafts.isNotEmpty()
                    ) { Text(stringResource(R.string.import_continue)) }
                }
            } else {
                items(drafts, key = { it.id }) { candidate ->
                    ImportCandidateEditor(candidate, state,
                        if (showValidation && candidate.selected) importCandidateErrors(candidate) else ImportCandidateErrors(), !busy) { edited ->
                        drafts = drafts.map { if (it.id == edited.id) edited else it }
                        operationError = null
                    }
                }
            }
        }
    }
}

private val ImportCandidateDraftSaver = Saver<List<ImportCandidate>, String>(
    save = { Json.encodeToString(it) },
    restore = { Json.decodeFromString<List<ImportCandidate>>(it) }
)
