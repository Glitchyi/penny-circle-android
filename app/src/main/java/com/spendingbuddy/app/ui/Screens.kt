@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.spendingbuddy.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
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
    Box(Modifier.fillMaxSize().auroraBackground()) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        topBar = {
            Column(Modifier.statusBarsPadding().padding(horizontal = 18.dp).padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(top = 8.dp)) {
                        Text(
                            date.format(dateFormatter).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("YOUR SHARED DIARY", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Settings") }
                }
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
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(32.dp)).padding(horizontal = 20.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        buildAnnotatedString { withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("01  ") }; append("SPENT TODAY") },
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { heading(); contentDescription = "Spent today" }
                    )
                    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, LocalDensity.current.fontScale.coerceAtMost(1.3f))) {
                    val heroSize = heroFontSizeSp(
                        availableWidthDp = (maxWidth.value - 40f).coerceAtLeast(0f),
                        digitCount = (spent / 100).toString().length,
                        hasPaise = spent % 100L != 0L,
                        fontScale = LocalDensity.current.fontScale,
                        maxSp = if (compact) 80f else 96f
                    ).sp
                    Row(Modifier.clearAndSetSemantics { contentDescription = "${rupees(spent)} spent today" }, horizontalArrangement = Arrangement.Center) {
                        Text("₹", fontSize = heroSize * 0.42f, color = tokens.accent2, fontFamily = PennyType.Sans, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = (heroSize.value * 0.15f).dp))
                        RollingAmount(
                            rupees = spent / 100,
                            style = TextStyle(fontFamily = PennyType.Sans, fontWeight = FontWeight.SemiBold, fontSize = heroSize, lineHeight = heroSize, letterSpacing = (-5).sp),
                            color = MaterialTheme.colorScheme.onBackground,
                            brush = Brush.verticalGradient(listOf(tokens.text, tokens.accent))
                        )
                        if (spent % 100L != 0L) Text(
                            String.format(java.util.Locale.ROOT, ".%02d", spent % 100),
                            style = TextStyle(fontFamily = PennyType.Mono, fontWeight = FontWeight.Medium, fontSize = 28.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 18.dp)
                        )
                    }
                    }
                    }
                    Text(
                        if (over) "${rupees(spent - state.dailyLimitPaise)} over today’s ${rupees(state.dailyLimitPaise)}"
                        else "${rupees(state.dailyLimitPaise - spent)} left of ${rupees(state.dailyLimitPaise)}",
                        color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    HorizonArc(Modifier.padding(top = gap), intensity = budgetIntensity(spent, state.dailyLimitPaise), height = if (compact) 56.dp else 84.dp, secondary = tokens.accent2)
                    FlowRow(
                        Modifier.padding(top = gap),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.glass(CircleShape, 0.8f).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(
                                "THIS WEEK ${rupees(weekSpent)} / ${rupees(state.weeklyLimitPaise)}",
                                maxLines = 1,
                                softWrap = false,
                                style = labelStyle.copy(letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (streakCount > 0) Box(Modifier.glass(CircleShape, 0.8f).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(
                                "◉ $streakCount-DAY STREAK",
                                maxLines = 1,
                                softWrap = false,
                                style = labelStyle.copy(letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        val finishShape = RoundedCornerShape(18.dp)
                        Row(
                            Modifier.fillMaxWidth().padding(top = 16.dp).glass(finishShape)
                                .border(1.dp, Brush.horizontalGradient(listOf(tokens.accent, tokens.accent2)), finishShape)
                                .clickable(role = Role.Button) {
                                    if (state.haptics) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    onFinish(date.toString())
                                }
                                .defaultMinSize(minHeight = 56.dp).padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("FINISH TODAY", fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, color = tokens.text)
                            Spacer(Modifier.weight(1f))
                            Icon(PennyIcons.ArrowRight, null, tint = tokens.accent)
                        }
                    }
                }
            }
            item {
                Column {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            buildAnnotatedString { withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("02  ") }; append("PURCHASES") },
                            style = labelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { heading(); contentDescription = "Purchases" }
                        )
                        Text(
                            "${todayExpenses.size} ${if (todayExpenses.size == 1) "ITEM" else "ITEMS"}",
                            style = labelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (todayExpenses.isEmpty()) item { EmptyState(onAdd, onNoSpend = { onFinish(date.toString()) }) }
            else item {
                Column(
                    Modifier.fillMaxWidth().then(if (reduced) Modifier else Modifier.animateContentSize()).glass(RoundedCornerShape(24.dp))
                ) {
                    todayExpenses.forEachIndexed { index, e ->
                        key(e.id) {
                            PurchaseRow(e, flat = true, onClick = onPurchase?.let { open -> { open(e) } })
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
            Text(if (initial == null) "Add purchase" else "Edit purchase", style = MaterialTheme.typography.headlineSmall)
            Text("Step ${step + 1} of 2", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            if (step == 0) {
                AmountStep(amount, { amount = it }, dial, { dial = it })
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (amountToPaise(amount) == null || amountToPaise(amount) == 0L) error = "Enter an amount above ₹0" else { error = null; step = 1 } }, Modifier.fillMaxWidth()) { Text("Next") }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Amount ₹${amount.ifBlank { "0" }}", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { step = 0 }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Change") }
                }
                DetailsStep(state, category, { category = it }, reason, { reason = it }, merchant, { merchant = it }, date, { date = it }, note, { note = it }, saveAsPreset, { saveAsPreset = it })
                ErrorText(error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    val p = amountToPaise(amount)
                    if (p == null || p <= 0L) error = "Enter an amount above ₹0"
                    else if (reason.isBlank()) error = "Choose a reason"
                    else if (saveAsPreset && merchant.isNotBlank() && !hasPresetNamed(state.presets, merchant) && validatePreset(state.presets, merchant, category, reason) != null) error = validatePreset(state.presets, merchant, category, reason)
                    else {
                        if (saveAsPreset && merchant.isNotBlank() && !hasPresetNamed(state.presets, merchant)) onSavePreset(merchant, category, reason)
                        onSave(Expense(id = initial?.id ?: java.util.UUID.randomUUID().toString(), date = date, amountPaise = p, merchant = merchant, category = category, reason = reason, note = note))
                    }
                }, Modifier.fillMaxWidth()) { Text("Save purchase") }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun AmountStep(amount: String, onAmount: (String) -> Unit, dial: Boolean, onDial: (Boolean) -> Unit) {
    Text("Amount", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("₹${amount.ifBlank { "0" }}", style = MaterialTheme.typography.displayLarge, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Amount ${amount.ifBlank { "zero" }} rupees" })
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { Pill("Keypad", !dial, onClick = { onDial(false) }); Spacer(Modifier.width(8.dp)); Pill("Dial", dial, onClick = { onDial(true) }) }
    if (dial) RotaryAmountInput(value = amount, onValueChange = onAmount, haptics = true, onUseKeypad = { onDial(false) })
    else Keypad(amount, onAmount)
}

@Composable
private fun Keypad(amount: String, onAmount: (String) -> Unit) {
    // Stateless: the single amount state lives in the caller, so switching modes or navigating back keeps input.
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "⌫")).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { key ->
                val label = when (key) { "⌫" -> "Delete last character"; "." -> "Decimal point"; else -> "Digit $key" }
                OutlinedButton(onClick = { onAmount(when (key) { "⌫" -> removeRotaryLastCharacter(amount); "." -> appendRotaryDecimal(amount); else -> appendRotaryDigit(amount, key.single() - '0') }) }, Modifier.weight(1f).height(64.dp).semantics { contentDescription = label }) { Text(key, style = MaterialTheme.typography.titleLarge) }
            } }
        }
    }
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
        Button(onClick = onConfirm, Modifier.fillMaxWidth()) { Text(if (purchases.isEmpty()) "Mark as no-spend day" else "Yes, that's everything") }
        OutlinedButton(onClick = onAdd, Modifier.fillMaxWidth()) { Text("Add something I missed") }
        Spacer(Modifier.height(12.dp))
    } }
}

@Composable
fun ActivityScreen(state: BuddyState, now: ZonedDateTime, bottomBar: @Composable () -> Unit, onAdd: () -> Unit, snackbar: SnackbarHostState? = null, onPurchase: ((Expense) -> Unit)? = null) {
    Scaffold(
        topBar = { ScreenTitle("Activity") }, bottomBar = bottomBar,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Add purchase") }) }
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
