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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
    onSettings: () -> Unit,
    onFinishYesterday: () -> Unit,
    snackbar: SnackbarHostState? = null,
    onPurchase: ((Expense) -> Unit)? = null
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
    val greeting = if (firstName.isBlank() || firstName.equals("You", ignoreCase = true)) "Today" else "Hi, $firstName"
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
                        greeting,
                        style = MaterialTheme.typography.displayMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                val settingsPress = remember { MutableInteractionSource() }
                Box(
                    Modifier.size(48.dp).pressScale(settingsPress, pressed = 0.9f).glass(CircleShape, shadow = false)
                        .clickable(interactionSource = settingsPress, indication = LocalIndication.current, role = Role.Button, onClickLabel = "Open settings", onClick = onSettings),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Outlined.Settings, "Settings", Modifier.size(22.dp)) }
            }
        },
        bottomBar = bottomBar,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            AccentButton(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                haptics = state.haptics
            ) {
                Icon(PennyIcons.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add purchase", fontWeight = FontWeight.Bold)
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
            val yesterday = date.minusDays(1)
            if (eligibleCheckIn(yesterday, now) && state.checkIns.none { it.date == yesterday.toString() }) {
                item {
                    NotebookSurface(Modifier.fillMaxWidth()) {
                        Text("Yesterday is still open", style = MaterialTheme.typography.titleMedium)
                        Text("Finish it before 10 AM to keep your streak.", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onFinishYesterday, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Finish yesterday")
                        }
                    }
                }
            }
            item {
                val over = spent > state.dailyLimitPaise
                val weekOver = weekSpent > state.weeklyLimitPaise
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(
                        Modifier.fillMaxWidth().enterRise(0).glass(RoundedCornerShape(32.dp)).padding(start = 14.dp, end = 14.dp, top = 22.dp, bottom = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, LocalDensity.current.fontScale.coerceAtMost(1.3f))) {
                    val gaugeStroke = 14.dp
                    val diameter = minOf(maxWidth, if (compact) 236.dp else 292.dp)
                    val heroSize = heroFontSizeSp(
                        availableWidthDp = (diameter.value - 76f).coerceAtLeast(0f),
                        digitCount = (spent / 100).toString().length,
                        hasPaise = spent % 100L != 0L,
                        fontScale = LocalDensity.current.fontScale,
                        maxSp = if (compact) 64f else 84f,
                        minSp = 34f
                    ).sp
                    BudgetGauge(
                        progress = budgetIntensity(spent, state.dailyLimitPaise),
                        over = over,
                        diameter = diameter,
                        stroke = gaugeStroke
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(top = gaugeStroke + if (compact) 14.dp else 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "SPENT TODAY",
                                style = labelStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.semantics { heading(); contentDescription = "Spent today" }
                            )
                            Row(Modifier.bumpOnChange(spent).clearAndSetSemantics { contentDescription = "${rupees(spent)} spent today" }, horizontalArrangement = Arrangement.Center) {
                                Text("₹", fontSize = heroSize * 0.42f, color = tokens.muted, fontFamily = PennyType.Sans, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = (heroSize.value * 0.15f).dp))
                                RollingAmount(
                                    rupees = spent / 100,
                                    style = TextStyle(fontFamily = PennyType.Sans, fontWeight = FontWeight.SemiBold, fontSize = heroSize, lineHeight = heroSize, letterSpacing = (-0.04f * heroSize.value).sp),
                                    color = if (over) tokens.danger else tokens.text
                                )
                                if (spent % 100L != 0L) Text(
                                    String.format(java.util.Locale.ROOT, ".%02d", spent % 100),
                                    style = TextStyle(fontFamily = PennyType.Mono, fontWeight = FontWeight.Medium, fontSize = heroSize * 0.3f),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = (heroSize.value * 0.2f).dp)
                                )
                            }
                            Text(
                                "of ${rupees(state.dailyLimitPaise)} daily limit",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.weight(1f))
                            StatusPill(
                                if (over) "${rupees(spent - state.dailyLimitPaise)} over" else "${rupees(state.dailyLimitPaise - spent)} left",
                                danger = over
                            )
                        }
                    }
                    }
                    }
                    val streakValue = if (streakCount == 1) "1 day" else "$streakCount days"
                    val streakCaption = if (streakCount > 0) "Keep it going" else "Starts when you finish"
                    if (LocalDensity.current.fontScale > COMPACT_FONT_SCALE) {
                        Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("This week", rupees(weekSpent), "of ${rupees(state.weeklyLimitPaise)}", Modifier.fillMaxWidth(), progress = budgetIntensity(weekSpent, state.weeklyLimitPaise), over = weekOver)
                            StatTile("Streak", streakValue, streakCaption, Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(top = 20.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatTile("This week", rupees(weekSpent), "of ${rupees(state.weeklyLimitPaise)}", Modifier.weight(1f).fillMaxHeight(), progress = budgetIntensity(weekSpent, state.weeklyLimitPaise), over = weekOver)
                            StatTile("Streak", streakValue, streakCaption, Modifier.weight(1f).fillMaxHeight())
                        }
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
                        FinishRow(
                            onClick = {
                                if (state.haptics) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                onFinish(date.toString())
                            },
                            modifier = Modifier.padding(top = 14.dp).enterRise(1)
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp).enterRise(2), verticalAlignment = Alignment.Bottom) {
                    Text("Purchases", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
                    Text(
                        "${todayExpenses.size} ${if (todayExpenses.size == 1) "item" else "items"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (todayExpenses.isEmpty()) item { EmptyState(Modifier.enterRise(3)) }
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

@Composable
private fun NudgeBanner(nudge: Nudge, now: ZonedDateTime, onFinish: (String) -> Unit, onAdd: () -> Unit) {
    val sender = nudge.sender.ifBlank { "A friend in your circle" }
    NotebookSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$sender nudged you", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
        }
        Text("Finish today before it runs out.", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onAdd, modifier = Modifier.heightIn(min = 48.dp)) { Text("Add purchase") }
            Button(
                onClick = { onFinish(now.toLocalDate().toString()) },
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Finish today") }
        }
    }
}

@Composable
fun AddSheet(state: BuddyState, initial: Expense? = null, onDismiss: () -> Unit, onSave: (Expense) -> Unit, onSavePreset: (name: String, category: String, reason: String) -> String?) {
    val defaultPreset = if (initial == null) state.presets.firstOrNull { it.isDefault } else null
    var step by rememberSaveable { mutableIntStateOf(if (initial == null) 0 else 1) }
    var amount by rememberSaveable { mutableStateOf(initial?.amountPaise?.let { (it / 100.0).toString().trimEnd('0').trimEnd('.') } ?: "") }
    var category by rememberSaveable { mutableStateOf(initial?.category ?: defaultPreset?.category ?: state.lastCategory) }
    var reason by rememberSaveable { mutableStateOf(initial?.reason ?: defaultPreset?.reason ?: "") }
    var merchant by rememberSaveable { mutableStateOf(initial?.merchant ?: defaultPreset?.name ?: "") }
    var saveAsPreset by rememberSaveable { mutableStateOf(false) }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var date by rememberSaveable { mutableStateOf(initial?.date ?: todayIndia().toString()) }
    var dial by rememberSaveable { mutableStateOf(state.amountInput == "dial") }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (initial == null) "Add purchase" else "Edit purchase", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                StepBars(step, 2)
            }
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
                ErrorText(error)
                Spacer(Modifier.height(12.dp))
                AccentButton(
                    onClick = { if (amountToPaise(amount) == null || amountToPaise(amount) == 0L) error = "Enter an amount above ₹0" else { error = null; step = 1 } },
                    modifier = Modifier.fillMaxWidth(),
                    haptics = state.haptics
                ) { Text("Next", fontWeight = FontWeight.Bold) }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Amount ₹${amount.ifBlank { "0" }}", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { step = 0 }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Change") }
                }
                DetailsStep(state, category, { category = it }, reason, { reason = it }, merchant, { merchant = it }, date, { date = it }, note, { note = it }, saveAsPreset, { saveAsPreset = it })
                ErrorText(error)
                Spacer(Modifier.height(16.dp))
                AccentButton(onClick = {
                    val p = amountToPaise(amount)
                    if (p == null || p <= 0L) error = "Enter an amount above ₹0"
                    else if (reason.isBlank()) error = "Choose a reason"
                    else if (saveAsPreset && merchant.isNotBlank() && !hasPresetNamed(state.presets, merchant) && validatePreset(state.presets, merchant, category, reason) != null) error = validatePreset(state.presets, merchant, category, reason)
                    else {
                        if (saveAsPreset && merchant.isNotBlank() && !hasPresetNamed(state.presets, merchant)) onSavePreset(merchant, category, reason)
                        onSave(Expense(id = initial?.id ?: java.util.UUID.randomUUID().toString(), date = date, amountPaise = p, merchant = merchant, category = category, reason = reason, note = note))
                    }
                }, modifier = Modifier.fillMaxWidth(), haptics = state.haptics) { Text("Save purchase", fontWeight = FontWeight.Bold) }
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
        SegmentedToggle(listOf("Keypad", "Dial"), if (dial) 1 else 0, { onDial(it == 1) }, Modifier.width(200.dp))
    }
    if (dial) RotaryAmountInput(value = amount, onValueChange = onAmount, haptics = true, onUseKeypad = { onDial(false) })
    else Keypad(amount, onAmount, haptics)
}

@Composable
private fun DetailsStep(state: BuddyState, category: String, onCategory: (String) -> Unit, reason: String, onReason: (String) -> Unit, merchant: String, onMerchant: (String) -> Unit, date: String, onDate: (String) -> Unit, note: String, onNote: (String) -> Unit, saveAsPreset: Boolean, onSaveAsPreset: (Boolean) -> Unit) {
    if (state.presets.isNotEmpty()) {
        Text("Presets", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.presets.forEach { p ->
                Pill(if (p.isDefault) "${p.name} (default)" else p.name, selected = merchant.trim().equals(p.name, ignoreCase = true), onClick = { onMerchant(p.name); onCategory(p.category); onReason(p.reason) })
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    Text("Category", style = MaterialTheme.typography.titleMedium)
    LazyRowChips(state.categories, category, onCategory)
    Spacer(Modifier.height(8.dp)); Text("Why did you buy it?", style = MaterialTheme.typography.titleMedium)
    LazyRowChips(state.reasons, reason, onReason)
    if (reason == IMPULSE) Text("Marked as impulse. Worth a pause next time.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
    OutlinedTextField(merchant, onMerchant, Modifier.fillMaxWidth(), label = { Text("Merchant or what for (optional)") }, singleLine = true)
    if (merchant.isNotBlank() && !hasPresetNamed(state.presets, merchant) && state.presets.size < MAX_PRESETS) {
        Row(Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).toggleable(saveAsPreset, role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onSaveAsPreset), verticalAlignment = Alignment.CenterVertically) {
            Text("Save \"${merchant.trim()}\" as a preset (only on this phone)", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Switch(saveAsPreset, null)
        }
    }
    if (state.name.isNotBlank()) Spacer(Modifier.height(6.dp))
    Spacer(Modifier.height(8.dp)); Text("When", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Pill("Today", date == todayIndia().toString(),onClick = { onDate(todayIndia().toString()) }); Pill("Yesterday", date == todayIndia().minusDays(1).toString(), onClick = { onDate(todayIndia().minusDays(1).toString()) }) }
    OutlinedTextField(note, onNote, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Note (optional)") }, singleLine = true)
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun LazyRowChips(values: List<String>, selected: String, onSelected: (String) -> Unit) { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { values.forEach { Pill(it, it == selected, onClick = { onSelected(it) }) } } }

@Composable
fun FinishSheet(state: BuddyState, date: String, onConfirm: () -> Unit, onAdd: () -> Unit, onDismiss: () -> Unit) {
    val localDate = runCatching { LocalDate.parse(date) }.getOrDefault(todayIndia()); val purchases = state.expenses.filter { it.date == date }; val total = netSpending(purchases); val impulses = purchases.count { it.reason == IMPULSE }
    ModalBottomSheet(onDismissRequest = onDismiss) { Column(Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding()) {
        Text("Finish ${localDate.format(dateFormatter)}", style = MaterialTheme.typography.headlineSmall)
        Text("${purchases.size} purchases · ${rupees(total)} · $impulses impulse", style = MaterialTheme.typography.bodyLarge)
        Text(if (purchases.isEmpty()) "No purchases today?" else if (total <= state.dailyLimitPaise) "You stayed ${rupees(state.dailyLimitPaise - total)} under your daily limit." else "You went ${rupees(total - state.dailyLimitPaise)} over today's limit.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 14.dp))
        AccentButton(onClick = onConfirm, modifier = Modifier.fillMaxWidth(), haptics = state.haptics) { Text(if (purchases.isEmpty()) "Mark as no-spend day" else "Yes, that's everything", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        SecondaryButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add something I missed", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(12.dp))
    } }
}

@Composable
fun ActivityScreen(state: BuddyState, now: ZonedDateTime, bottomBar: @Composable () -> Unit, onAdd: () -> Unit, snackbar: SnackbarHostState? = null, onPurchase: ((Expense) -> Unit)? = null) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { ScreenTitle("Activity") }, bottomBar = bottomBar,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        floatingActionButton = {
            AccentButton(onClick = onAdd, haptics = state.haptics) {
                Icon(PennyIcons.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add purchase", fontWeight = FontWeight.Bold)
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
    onRemove: (String) -> Unit,
    onUpdate: (ImportCandidate) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    snackbar: SnackbarHostState? = null
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        topBar = { ScreenTitle("Import review", onBack) },
        bottomBar = {
            if (step == 1) Surface(shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDiscard) { Text("Discard draft") }
                    Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text("Save ${state.importCandidates.count { it.selected }} purchases") }
                }
            }
        }
    ) { pad ->
        val reviewListState = rememberLazyListState()
        LazyColumn(Modifier.fillMaxSize().padding(pad).scrollFadingEdges(reviewListState, bottom = 40.dp), state = reviewListState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Step ${step + 1} of 2", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (step == 0) "Screenshots" else "Review purchases", style = MaterialTheme.typography.headlineSmall)
            }
            if (step == 0) {
                items(state.importImages, key = { it.id }) { image ->
                    NotebookSurface(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                when (image.status) {
                                    "processing" -> "Reading…"
                                    "ready", "done" -> "Ready"
                                    "failed" -> "Couldn't read: ${image.error}"
                                    else -> image.status
                                }, Modifier.weight(1f)
                            )
                            if (image.status == "failed") TextButton(onClick = { onRetry(image.id) }) { Text("Retry") }
                            TextButton(onClick = { onRemove(image.id) }) { Text("Remove") }
                        }
                    }
                }
                item { OutlinedButton(onClick = onAddMore, modifier = Modifier.fillMaxWidth()) { Text("Add more screenshots") } }
                item {
                    Button(
                        onClick = { step = 1 },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.importImages.none { it.status == "processing" } && state.importCandidates.isNotEmpty()
                    ) { Text("Continue") }
                }
            } else {
                items(state.importCandidates, key = { it.id }) { candidate -> ImportCandidateRow(candidate, onUpdate) }
            }
        }
    }
}

@Composable private fun ImportCandidateRow(c: ImportCandidate, onUpdate: (ImportCandidate) -> Unit) { Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(c.selected, { onUpdate(c.copy(selected = it)) }); Column(Modifier.weight(1f)) { Text(c.merchant.ifBlank { "Purchase" }, style = MaterialTheme.typography.titleMedium); Text("${c.amountText} · ${c.date}", style = MaterialTheme.typography.bodyMedium) }; if (c.possibleDuplicate) Text("Possible duplicate", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }; Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Pill(c.category, selected = true); Pill(c.reason, selected = true) }; if (c.possibleDuplicate) Text("Looks like a purchase you already logged", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Rule() } }
