@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.spendingbuddy.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendingbuddy.app.*
import com.spendingbuddy.app.p2p.*
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

@Composable
fun TodayScreen(state: BuddyState, peerStatus: PeerStatus, now: ZonedDateTime, onAdd: () -> Unit, onFinish: (String) -> Unit, onSettings: () -> Unit, onActivity: () -> Unit, onBuddy: () -> Unit, onFinishYesterday: () -> Unit, onNudge: () -> Unit) {
    val date = now.toLocalDate()
    val spent = spentOn(state.expenses, date)
    val weekSpent = spentInWeek(state.expenses, date)
    val status = dayStatus(date, state.checkIns, now)
    val todayExpenses = state.expenses.filter { it.date == date.toString() }.sortedByDescending { it.updatedAt }
    val streakCount = streak(state.checkIns, now)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(date.format(dateFormatter), style = MaterialTheme.typography.displaySmall)
                    Text("Your shared diary", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (streakCount > 0) Pill("◉ $streakCount", tone = NotebookColors.Marigold.copy(alpha = .2f))
                IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Settings") }
            }
        },
        bottomBar = { AppNavBar(0) { if (it == 1) onActivity() else if (it == 2) onBuddy() } },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Add purchase") }) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            val incomingNudge = state.incomingNudge
            if (incomingNudge != null) item { NudgeBanner(incomingNudge, now, onFinish, onAdd) }
            val yesterday = date.minusDays(1)
            if (eligibleCheckIn(yesterday, now) && state.checkIns.none { it.date == yesterday.toString() }) item {
                NotebookSurface(Modifier.fillMaxWidth()) {
                    Text("Yesterday is still open", style = MaterialTheme.typography.titleMedium)
                    Text("Finish it before 10 AM to keep your streak.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onFinishYesterday) { Text("Finish yesterday") }
                }
            }
            item {
                NotebookSurface(Modifier.fillMaxWidth()) {
                    Text("Spent today", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rupees(spent), style = MaterialTheme.typography.displayLarge, modifier = Modifier.semantics { contentDescription = "${rupees(spent)} spent today" })
                    val over = spent > state.dailyLimitPaise
                    ProgressBar(spent, state.dailyLimitPaise, over, Modifier.padding(vertical = 10.dp))
                    Text(if (over) "${rupees(spent - state.dailyLimitPaise)} over today’s ${rupees(state.dailyLimitPaise)}" else "${rupees(state.dailyLimitPaise - spent)} left of ${rupees(state.dailyLimitPaise)}", color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("This week  ${rupees(weekSpent)} of ${rupees(state.weeklyLimitPaise)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    if (status == DayStatus.Done || status == DayStatus.Grace || status == DayStatus.Late) {
                        Stamp(finishedAt = state.checkIns.firstOrNull { it.date == date.toString() }?.completedAt)
                    } else {
                        Button(onClick = { onFinish(date.toString()) }, modifier = Modifier.fillMaxWidth()) { Text("Finish today") }
                    }
                }
            }
            if (state.peer != null && state.friend != null) item {
                BuddyStrip(state, peerStatus, onNudge)
            }
            item { Text("Today's purchases", style = MaterialTheme.typography.titleMedium) }
            if (todayExpenses.isEmpty()) item { EmptyState(onAdd, onNoSpend = { onFinish(date.toString()) }) }
            else items(todayExpenses, key = { it.id }) { PurchaseRow(it) }
        }
    }
}

@Composable
private fun NudgeBanner(nudge: Nudge, now: ZonedDateTime, onFinish: (String) -> Unit, onAdd: () -> Unit) {
    val sender = nudge.sender.ifBlank { "Your friend" }
    NotebookSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("$sender nudged you", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Icon(Icons.Rounded.NotificationsActive, null, tint = NotebookColors.Marigold) }
        Text("Finish today before it runs out.", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = onAdd) { Text("Add purchase") }; Button(onClick = { onFinish(now.toLocalDate().toString()) }) { Text("Finish today") } }
    }
}

@Composable
private fun BuddyStrip(state: BuddyState, peerStatus: PeerStatus, onNudge: () -> Unit) {
    val friend = state.friend ?: return
    val friendSpent = spentOn(friend.expenses, todayIndia())
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${friend.name.ifBlank { "Friend" }} · ${rupees(friendSpent)} today", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (peerStatus.friendReachable && friend.checkIns.none { it.date == todayIndia().toString() }) TextButton(onClick = onNudge) { Text(if (state.outgoingNudge == null) "Nudge" else "Nudge again") }
    }
}

@Composable
private fun Stamp(finishedAt: Long?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("DONE", color = NotebookColors.Marigold, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.border(3.dp, NotebookColors.Marigold, RoundedCornerShape(8.dp)).padding(horizontal = 18.dp, vertical = 5.dp))
        Text(if (finishedAt != null) "Finished at ${java.time.Instant.ofEpochMilli(finishedAt).atZone(INDIA_ZONE).format(DateTimeFormatter.ofPattern("h:mm a"))}" else "Finished today", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AddSheet(state: BuddyState, initial: Expense? = null, onDismiss: () -> Unit, onSave: (Expense) -> Unit, onPreset: (String, Boolean) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(if (initial == null) 0 else 1) }
    var amount by rememberSaveable { mutableStateOf(initial?.amountPaise?.let { (it / 100.0).toString().trimEnd('0').trimEnd('.') } ?: "") }
    var category by rememberSaveable { mutableStateOf(initial?.category ?: state.lastCategory) }
    var reason by rememberSaveable { mutableStateOf(initial?.reason ?: "") }
    var merchant by rememberSaveable { mutableStateOf(initial?.merchant ?: "") }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var date by rememberSaveable { mutableStateOf(initial?.date ?: todayIndia().toString()) }
    var dial by rememberSaveable { mutableStateOf(state.amountInput == "dial") }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().imePadding()) {
            Text(if (initial == null) "Add purchase" else "Edit purchase", style = MaterialTheme.typography.headlineSmall)
            Text("Step ${step + 1} of 2", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            if (step == 0) {
                AmountStep(amount, { amount = it }, dial, { dial = it })
                Spacer(Modifier.height(8.dp))
                Button(onClick = { if (amountToPaise(amount) == null || amountToPaise(amount) == 0L) error = "Enter an amount above ₹0" else { error = null; step = 1 } }, Modifier.fillMaxWidth()) { Text("Next") }
            } else {
                DetailsStep(state, category, { category = it }, reason, { reason = it }, merchant, { merchant = it }, date, { date = it }, note, { note = it })
                ErrorText(error)
                Button(onClick = {
                    val p = amountToPaise(amount)
                    if (p == null || p <= 0L) error = "Enter an amount above ₹0"
                    else if (reason.isBlank()) error = "Choose a reason"
                    else onSave(Expense(id = initial?.id ?: java.util.UUID.randomUUID().toString(), date = date, amountPaise = p, merchant = merchant, category = category, reason = reason, note = note))
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { FilterChip(dial, { onDial(false) }, label = { Text("Keypad") }); Spacer(Modifier.width(8.dp)); FilterChip(dial, { onDial(true) }, label = { Text("Dial") }) }
    if (dial) RotaryAmountInput(value = amount, onValueChange = onAmount, haptics = true, modifier = Modifier.heightIn(max = 300.dp))
    else Keypad(onAmount)
}

@Composable
private fun Keypad(onAmount: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    // The visible value is owned by AmountStep through callback; each key appends from the latest text via a local bridge.
    val current = rememberUpdatedState(value)
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(".", "0", "⌫")).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { key ->
                OutlinedButton(onClick = { val next = when (key) { "⌫" -> current.value.dropLast(1); "." -> if (current.value.contains('.')) current.value else if (current.value.isBlank()) "0." else current.value + "."; else -> appendKey(current.value, key.single()) }; value = next; onAmount(next) }, Modifier.weight(1f).height(64.dp)) { Text(key, style = MaterialTheme.typography.titleLarge) }
            } }
        }
    }
}
private fun appendKey(current: String, digit: Char): String { if (current.contains('.') && current.substringAfter('.').length >= 2) return current; if (current == "0") return digit.toString(); return current + digit }

@Composable
private fun DetailsStep(state: BuddyState, category: String, onCategory: (String) -> Unit, reason: String, onReason: (String) -> Unit, merchant: String, onMerchant: (String) -> Unit, date: String, onDate: (String) -> Unit, note: String, onNote: (String) -> Unit) {
    Text("Category", style = MaterialTheme.typography.titleMedium)
    LazyRowChips(state.categories, category, onCategory)
    Spacer(Modifier.height(8.dp)); Text("Why did you buy it?", style = MaterialTheme.typography.titleMedium)
    LazyRowChips(state.reasons, reason, onReason)
    if (reason == IMPULSE) Text("Marked as impulse. Worth a pause next time.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
    OutlinedTextField(merchant, onMerchant, Modifier.fillMaxWidth(), label = { Text("Merchant (optional)") }, singleLine = true)
    if (state.name.isNotBlank()) Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Pill("Today", date == todayIndia().toString(), onClick = { onDate(todayIndia().toString()) }); Pill("Yesterday", date == todayIndia().minusDays(1).toString(), onClick = { onDate(todayIndia().minusDays(1).toString()) }) }
    OutlinedTextField(note, onNote, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Note") }, singleLine = true)
}

@Composable
private fun LazyRowChips(values: List<String>, selected: String, onSelected: (String) -> Unit) { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { values.forEach { Pill(it, it == selected, onClick = { onSelected(it) }) } } }

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
fun ActivityScreen(state: BuddyState, now: ZonedDateTime, onAdd: () -> Unit, onBack: (() -> Unit)? = null) {
    val date = now.toLocalDate(); val start = weekStart(date); val days = (0..6).map { start.plusDays(it.toLong()) }; val weekSpent = spentInWeek(state.expenses, date)
    Scaffold(topBar = { ScreenTitle("Activity", onBack) }, floatingActionButton = { ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("Add purchase") }) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { NotebookSurface(Modifier.fillMaxWidth()) { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = {}) { Icon(Icons.Rounded.ChevronLeft, "Previous week") }; Text(if (start == weekStart(date)) "This week" else "${start.format(shortDateFormatter)} – ${days.last().format(shortDateFormatter)}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); IconButton(onClick = {}) { Icon(Icons.Rounded.ChevronRight, "Next week") } }; Text("${rupees(weekSpent)} of ${rupees(state.weeklyLimitPaise)}", style = MaterialTheme.typography.titleMedium); ProgressBar(weekSpent, state.weeklyLimitPaise, weekSpent > state.weeklyLimitPaise, Modifier.padding(vertical = 10.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { days.forEach { day -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(day.dayOfWeek.name.take(1)); Text(rupees(spentOn(state.expenses, day)).removePrefix("₹"), style = MaterialTheme.typography.labelMedium) } } } } }
            item { Text("Where it went", style = MaterialTheme.typography.titleMedium) }
            val categoryTotals = state.expenses.filter { it.date in days.map(LocalDate::toString) }.groupBy { it.category }.mapValues { netSpending(it.value) }.toList().sortedByDescending { it.second }
            items(categoryTotals) { (cat, total) -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(cat, Modifier.width(100.dp)); ProgressBar(total, weekSpent.coerceAtLeast(1), false, Modifier.weight(1f).padding(horizontal = 8.dp)); Text(rupees(total)) } }
            if (state.expenses.none { it.date in days.map(LocalDate::toString) }) item { Text("No purchases this week.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            days.sortedDescending().forEach { day ->
                val purchases = state.expenses.filter { it.date == day.toString() }
                item { Text("${day.format(shortDateFormatter)}   ${rupees(spentOn(state.expenses, day))}   ${when (dayStatus(day, state.checkIns, now)) { DayStatus.Done -> "Done"; DayStatus.Grace -> "Grace"; DayStatus.Late -> "Finished late"; DayStatus.Missed -> "Missed"; DayStatus.Open -> "Open" }}", style = MaterialTheme.typography.titleMedium) }
                items(purchases, key = { it.id }) { PurchaseRow(it) }
            }
        }
    }
}

@Composable
fun BuddyScreen(state: BuddyState, peer: PeerStatus, now: ZonedDateTime, onInvite: () -> Unit, onJoin: () -> Unit, onNudge: (Int) -> Unit, onSuggest: () -> Unit, onRespond: (String, Boolean) -> Unit, onSync: () -> Unit, onUnlink: () -> Unit, onReachable: (Boolean) -> Unit, onBack: (() -> Unit)? = null) {
    val friend = state.friend
    Scaffold(topBar = { ScreenTitle("Buddy", onBack) }, containerColor = MaterialTheme.colorScheme.background) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (state.peer == null || friend == null) {
                item { NotebookSurface(Modifier.fillMaxWidth()) {
                    Text("Spend side by side", style = MaterialTheme.typography.headlineSmall)
                    Text("Link with one friend to see each other's day, send nudges and suggest limits.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onInvite, Modifier.weight(1f)) { Text("Show my invite code") }
                        OutlinedButton(onClick = onJoin, Modifier.weight(1f)) { Text("Join with a code") }
                    }
                    Text("Your data goes straight between your two phones, end-to-end encrypted. No account, and no server keeps a copy.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 14.dp))
                } }
            } else {
                item { NotebookSurface(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(46.dp), CircleShape, MaterialTheme.colorScheme.primary.copy(alpha = .12f)) { Box(contentAlignment = Alignment.Center) { Text(friend.name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge) } }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(friend.name, style = MaterialTheme.typography.headlineSmall); Text(syncCopy(friend.name, peer), style = MaterialTheme.typography.bodyMedium, color = if (peer.friendReachable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text("◉ ${streak(friend.checkIns, now)}-day streak", color = NotebookColors.Marigold, style = MaterialTheme.typography.labelLarge)
                    }
                } }
                item { Text("Today, side by side", style = MaterialTheme.typography.titleMedium) }
                item { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)).padding(16.dp)) {
                    BuddyColumn("You", spentOn(state.expenses, todayIndia()), state.dailyLimitPaise, state.checkIns.any { it.date == todayIndia().toString() }, Modifier.weight(1f))
                    VerticalDivider(Modifier.height(100.dp).padding(horizontal = 8.dp))
                    BuddyColumn(friend.name, spentOn(friend.expenses, todayIndia()), friend.dailyLimitPaise, friend.checkIns.any { it.date == todayIndia().toString() }, Modifier.weight(1f))
                } }
                if (friend.checkIns.none { it.date == todayIndia().toString() }) item { NudgeControls(state, peer, onNudge) }
                item { Column { Text("Limits", style = MaterialTheme.typography.titleMedium); Text("${friend.name}: ${rupees(friend.dailyLimitPaise)} a day · ${rupees(friend.weeklyLimitPaise)} a week", style = MaterialTheme.typography.bodyLarge); TextButton(onClick = onSuggest) { Text("Suggest new limits") } } }
                state.suggestions.filter { it.status == "pending" }.forEach { suggestion -> item { SuggestionCard(suggestion, onRespond) } }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Stay reachable", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge); Switch(state.stayReachable, onReachable) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onSync) { Text("Sync now") }; TextButton(onClick = onUnlink) { Text("Unlink") } } }
            }
        }
    }
}
private fun syncCopy(name: String, peer: PeerStatus): String = when { peer.lastError != null -> "Can't reach $name's phone right now. Updates will go out when it's back."; peer.lastExchangeAt == 0L -> "Waiting for the first exchange"; System.currentTimeMillis() - peer.lastExchangeAt < 120_000 -> "Synced just now · ${if (peer.viaRelay == true) "Via relay" else "Direct"}"; else -> "Synced ${((System.currentTimeMillis() - peer.lastExchangeAt) / 3_600_000).coerceAtLeast(1)} h ago" }
@Composable private fun BuddyColumn(title: String, spent: Long, limit: Long, finished: Boolean, modifier: Modifier = Modifier) { Column(modifier) { Text(title, style = MaterialTheme.typography.titleMedium); Text("${rupees(spent)} of ${rupees(limit)}", style = MaterialTheme.typography.bodyLarge); ProgressBar(spent, limit, spent > limit, Modifier.padding(vertical = 8.dp)); Text(if (finished) "Finished" else "Not finished", style = MaterialTheme.typography.bodyMedium, color = if (finished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun NudgeControls(state: BuddyState, peer: PeerStatus, onNudge: (Int) -> Unit) { Column { Text("${state.friend?.name ?: "Your friend"} hasn't finished today.", style = MaterialTheme.typography.bodyLarge); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { onNudge(30) }, enabled = peer.friendReachable) { Text("30 min") }; OutlinedButton(onClick = { onNudge(60) }, enabled = peer.friendReachable) { Text("1 hour") }; Button(onClick = { onNudge(30) }, enabled = peer.friendReachable) { Text("Send nudge") } } } }
@Composable private fun SuggestionCard(s: BudgetSuggestion, onRespond: (String, Boolean) -> Unit) { NotebookSurface(Modifier.fillMaxWidth()) { Text("${s.sender.ifBlank { "Your friend" }} suggests ${rupees(s.dailyPaise)} a day, ${rupees(s.weeklyPaise)} a week", style = MaterialTheme.typography.bodyLarge); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { onRespond(s.id, false) }) { Text("Keep mine") }; Button(onClick = { onRespond(s.id, true) }) { Text("Accept") } } } }

@Composable
fun InviteScreen(invite: InviteInfo?, qr: Bitmap?, onBack: () -> Unit, onShare: () -> Unit, onCancel: () -> Unit) { Scaffold(topBar = { ScreenTitle("Invite your friend", onBack) }) { pad -> Column(Modifier.fillMaxSize().padding(pad).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Scan this code", style = MaterialTheme.typography.headlineSmall); qr?.let { Image(it.asImageBitmap(), "Invite QR code", Modifier.size(240.dp).padding(16.dp)) }; Text(invite?.code ?: "Preparing invite…", style = MaterialTheme.typography.bodyMedium); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedButton(onClick = onShare) { Text("Share") }; TextButton(onClick = onCancel) { Text("Cancel") } }; Text("Expires in 15 minutes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Waiting for your friend…", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 24.dp)) } } }

@Composable
fun JoinScreen(onBack: () -> Unit, onScan: () -> Unit, onJoin: (String) -> Unit) { var code by rememberSaveable { mutableStateOf("") }; Scaffold(topBar = { ScreenTitle("Join with a code", onBack) }) { pad -> Column(Modifier.fillMaxSize().padding(pad).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Button(onClick = onScan, modifier = Modifier.fillMaxWidth().height(56.dp)) { Icon(Icons.Rounded.QrCodeScanner, null); Spacer(Modifier.width(8.dp)); Text("Scan QR code") }; Text("Or paste the invite code", style = MaterialTheme.typography.titleMedium); OutlinedTextField(value = code, onValueChange = { code = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("Invite code") }); Button(onClick = { onJoin(code) }, modifier = Modifier.fillMaxWidth(), enabled = code.isNotBlank()) { Text("Join") } } } }

@Composable
fun SettingsScreen(state: BuddyState, onBack: () -> Unit, onName: (String) -> Unit, onLimits: () -> Unit, onPresets: () -> Unit, onAmountInput: (String) -> Unit, onHaptics: (Boolean) -> Unit, onReminders: (Boolean) -> Unit, onAlerts: (Boolean) -> Unit, onReachable: (Boolean) -> Unit, onExport: () -> Unit, onRestore: () -> Unit, onBattery: () -> Unit) { var name by rememberSaveable(state.name) { mutableStateOf(state.name) }; Scaffold(topBar = { ScreenTitle("Settings", onBack) }) { pad -> LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { item { Text("Profile", style = MaterialTheme.typography.titleMedium); OutlinedTextField(name, { name = it; onName(it) }, Modifier.fillMaxWidth(), label = { Text("Name shown to your friend") }) }; item { SettingsButton("Spending limits", onLimits) }; item { SettingsButton("Presets", onPresets) }; item { Text("Amount entry", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)); Row { FilterChip(state.amountInput == "keypad", { onAmountInput("keypad") }, label = { Text("Keypad") }); Spacer(Modifier.width(8.dp)); FilterChip(state.amountInput == "dial", { onAmountInput("dial") }, label = { Text("Rotary dial") }) } }; item { SettingSwitch("Haptics", state.haptics, onHaptics) }; item { Text("Reminders", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)); SettingSwitch("10 PM check-in, 10 AM grace deadline", state.reminders, onReminders); SettingSwitch("Friend activity alerts", state.friendAlerts, onAlerts) }; item { SettingSwitch("Stay reachable", state.stayReachable, onReachable); SettingsButton("Battery settings", onBattery) }; item { Text("Your data", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)); SettingsButton("Export backup", onExport); SettingsButton("Restore backup", onRestore) }; item { Text("Version 0.2.0 · Sync is direct between your two phones.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 18.dp)) } } } }
@Composable private fun SettingsButton(text: String, onClick: () -> Unit) { TextButton(onClick, Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 14.dp, horizontal = 8.dp)) { Text(text, Modifier.weight(1f)); Icon(Icons.Rounded.ChevronRight, null) } }
@Composable private fun SettingSwitch(text: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge); Switch(checked, onChange) } }

@Composable
fun LimitsScreen(state: BuddyState, onBack: () -> Unit, onSave: (Long, Long) -> Unit) { var daily by rememberSaveable { mutableStateOf((state.dailyLimitPaise / 100).toString()) }; var weekly by rememberSaveable { mutableStateOf((state.weeklyLimitPaise / 100).toString()) }; Scaffold(topBar = { ScreenTitle("Spending limits", onBack) }) { pad -> Column(Modifier.fillMaxSize().padding(pad).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Text("Set the limits you want to see each day and week.", style = MaterialTheme.typography.bodyLarge); OutlinedTextField(daily, { daily = it }, Modifier.fillMaxWidth(), label = { Text("Daily limit (₹)") }); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("300", "500", "800", "1000").forEach { Pill("₹$it", daily == it, onClick = { daily = it }) } }; OutlinedTextField(weekly, { weekly = it }, Modifier.fillMaxWidth(), label = { Text("Weekly limit (₹)") }); TextButton(onClick = { weekly = ((daily.toLongOrNull() ?: 0) * 7).toString() }) { Text("Use 7 × daily") }; Button(onClick = { onSave((daily.toLongOrNull() ?: 0) * 100, (weekly.toLongOrNull() ?: 0) * 100) }, Modifier.fillMaxWidth()) { Text("Save limits") } } } }

@Composable
fun ImportScreen(
    state: BuddyState,
    onBack: () -> Unit,
    onAddMore: () -> Unit,
    onRetry: (String) -> Unit,
    onRemove: (String) -> Unit,
    onUpdate: (ImportCandidate) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
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
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    "done" -> "Ready"
                                    "error" -> "Couldn't read: ${image.error}"
                                    else -> image.status
                                }, Modifier.weight(1f)
                            )
                            if (image.status == "error") TextButton(onClick = { onRetry(image.id) }) { Text("Retry") }
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

@Composable
fun OnboardingScreen(step: Int, name: String, daily: String, weekly: String, onName: (String) -> Unit, onDaily: (String) -> Unit, onWeekly: (String) -> Unit, onNext: () -> Unit, onSkip: () -> Unit, onReminders: () -> Unit) { Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad -> Column(Modifier.fillMaxSize().padding(pad).padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) { Column { Text("Spending Buddy", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary); Text("Step ${step + 1} of 3", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp)); when (step) { 0 -> { Text("What should your friend call you?", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 38.dp)); OutlinedTextField(name, onName, Modifier.fillMaxWidth().padding(top = 20.dp), label = { Text("Your name") }, singleLine = true) }; 1 -> { Text("Pick a daily limit", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 38.dp)); Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("300", "500", "800", "1000").forEach { Pill("₹$it", daily == it, onClick = { onDaily(it); onWeekly((it.toLong() * 7).toString()) }) } }; OutlinedTextField(daily, onDaily, Modifier.fillMaxWidth(), label = { Text("Daily limit (₹)") }); OutlinedTextField(weekly, onWeekly, Modifier.fillMaxWidth().padding(top = 12.dp), label = { Text("Weekly limit (₹)") }) }; else -> { Text("Stay on track", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 38.dp)); Text("A 10 PM reminder helps you finish the day. If you miss it, finish before 10 AM to keep your streak. Your friend can send a nudge when you need one.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp)) } } }; Column { Button(onClick = if (step == 2) onReminders else onNext, Modifier.fillMaxWidth().height(52.dp)) { Text(if (step == 2) "Turn on reminders" else "Continue") }; TextButton(onClick = onSkip, Modifier.align(Alignment.CenterHorizontally)) { Text(if (step == 2) "Not now" else "Skip") } } } } }
