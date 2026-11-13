package com.spendingbuddy.app.ui

import com.spendingbuddy.app.Expense
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spendingbuddy.app.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayHeaderFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private const val CHART_BAR_MAX_DP = 72

/** Swipeable weeks: page 0 is the current week, higher pages are earlier weeks (bounded by the earliest purchase, 1..52 pages). */
@Composable
fun ActivityPager(state: BuddyState, now: ZonedDateTime, modifier: Modifier = Modifier, onPurchase: ((Expense) -> Unit)? = null) {
    val today = now.toLocalDate()
    val pageCount = remember(state.expenses, today) { weeksBack(state.expenses, today) }
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()
    HorizontalPager(pagerState, modifier.fillMaxSize(), key = { it }) { page ->
        WeekPage(
            state = state, now = now, pagesBack = page, pageCount = pageCount,
            onEarlier = { scope.launch { pagerState.animateScrollToPage(page + 1) } },
            onLater = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
            onPurchase = onPurchase
        )
    }
}

@Composable
private fun WeekPage(state: BuddyState, now: ZonedDateTime, pagesBack: Int, pageCount: Int, onEarlier: () -> Unit, onLater: () -> Unit, onPurchase: ((Expense) -> Unit)? = null) {
    val today = now.toLocalDate()
    val start = weekStartForPage(today, pagesBack)
    val range = weekRange(start)
    val groups = remember(state.expenses, start) { groupByDay(state.expenses, start) }
    val totals = remember(state.expenses, start) { dayTotals(state.expenses, start) }
    val weekSpent = spentInWeek(state.expenses, start)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // Item 0 is the summary card; an empty week has one empty-state item; otherwise each day is a header plus its purchases.
    val headerIndex = remember(groups) {
        var i = 1
        groups.associate { g -> val at = i; i += 1 + g.purchases.size; g.date to at }
    }
    LazyColumn(
        Modifier.fillMaxSize().scrollFadingEdges(listState), state = listState,
        contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "summary") {
            NotebookSurface(Modifier.fillMaxWidth()) {
                WeekHeader(range, today, pagesBack, pageCount, onEarlier, onLater)
                Spacer(Modifier.height(8.dp))
                val over = weekSpent > state.weeklyLimitPaise
                Text("${rupees(weekSpent)} of ${rupees(state.weeklyLimitPaise)}", style = MaterialTheme.typography.titleMedium)
                ProgressBar(weekSpent, state.weeklyLimitPaise, over, Modifier.padding(vertical = 8.dp))
                Text(
                    if (over) "${rupees(weekSpent - state.weeklyLimitPaise)} over the weekly limit" else "${rupees(state.weeklyLimitPaise - weekSpent)} left this week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                DayChart(start, totals, today, hasPurchases = groups.map { it.date }.toSet()) { day ->
                    headerIndex[day]?.let { scope.launch { listState.animateScrollToItem(it) } }
                }
            }
        }
        if (groups.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nothing logged this week.", style = MaterialTheme.typography.titleMedium)
                    Text("Purchases and no-spend days will show up here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else groups.forEach { g ->
            item(key = "day-${g.date}") {
                val status = when (dayStatus(g.date, state.checkIns, now)) {
                    DayStatus.Done -> "Done"; DayStatus.Grace -> "Grace"; DayStatus.Late -> "Finished late"
                    DayStatus.Missed -> "Not finished"; DayStatus.Open -> "Open"
                }
                Column(Modifier.padding(top = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(g.date.format(dayHeaderFormatter), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(rupees(g.totalPaise), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Rule(Modifier.padding(top = 6.dp))
                }
            }
            items(g.purchases.size, key = { "p-${g.purchases[it].id}" }) { i -> val e = g.purchases[i]; PurchaseRow(e, onClick = onPurchase?.let { open -> { open(e) } }) }
        }
    }
}

@Composable
private fun WeekHeader(range: WeekRange, today: LocalDate, pagesBack: Int, pageCount: Int, onEarlier: () -> Unit, onLater: () -> Unit) {
    val atOldest = pagesBack >= pageCount - 1
    val atNewest = pagesBack == 0
    val relative = when (pagesBack) { 0 -> "This week"; 1 -> "Last week"; else -> "$pagesBack weeks ago" }
    val note = when {
        atOldest && atNewest -> "Your first week"
        atOldest -> "Earliest week with entries"
        else -> relative
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onEarlier, enabled = !atOldest, modifier = Modifier.size(48.dp).semantics { stateDescription = if (atOldest) "No earlier weeks" else "Shows the earlier week" }) {
            Icon(Icons.Rounded.ChevronLeft, "Earlier week")
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(weekRangeLabel(range, today), style = MaterialTheme.typography.titleLarge)
            Text(if (atOldest && !atNewest) "$relative · $note" else note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onLater, enabled = !atNewest, modifier = Modifier.size(48.dp).semantics { stateDescription = if (atNewest) "You are on the current week" else "Shows the later week" }) {
            Icon(Icons.Rounded.ChevronRight, "Later week")
        }
    }
}

@Composable
private fun DayChart(start: LocalDate, totals: List<Long>, today: LocalDate, hasPurchases: Set<LocalDate>, onDay: (LocalDate) -> Unit) {
    val max = totals.maxOfOrNull { it }?.coerceAtLeast(1L) ?: 1L
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        totals.forEachIndexed { i, total ->
            val day = start.plusDays(i.toLong())
            val isToday = day == today
            val tappable = day in hasPurchases
            val label = "${dayNames[i]} ${day.format(dayHeaderFormatter).substringAfter(' ')}: ${rupees(total)}" +
                (if (isToday) ", today" else "") + (if (!tappable) ", no purchases" else ", show purchases")
            val base = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
            Column(
                (if (tappable) base.clickable(role = Role.Button) { onDay(day) } else base)
                    .clearAndSetSemantics { contentDescription = label },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(rupees(total).replace("₹", ""), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                val h = if (total <= 0) 3 else (CHART_BAR_MAX_DP * total / max).toInt().coerceAtLeast(6)
                Box(
                    Modifier.fillMaxWidth(.6f).height(h.dp).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = .35f))
                )
                Text(dayNames[i], style = MaterialTheme.typography.labelMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                // Today is marked by weight and a dot, not by colour alone.
                Box(Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(if (isToday) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent))
            }
        }
    }
}
