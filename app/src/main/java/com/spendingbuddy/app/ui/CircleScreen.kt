@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.spendingbuddy.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendingbuddy.app.BudgetSuggestion
import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.FriendSnapshot
import com.spendingbuddy.app.Member
import com.spendingbuddy.app.Visibility
import com.spendingbuddy.app.amountToPaise
import com.spendingbuddy.app.MAX_MEMBERS
import com.spendingbuddy.app.p2p.JoinProgress
import com.spendingbuddy.app.p2p.activeMembers
import com.spendingbuddy.app.p2p.canNudge
import com.spendingbuddy.app.p2p.currentAdminId
import com.spendingbuddy.app.p2p.isAdmin
import com.spendingbuddy.app.rupees
import com.spendingbuddy.app.spentOn
import com.spendingbuddy.app.streak
import com.spendingbuddy.app.todayIndia

/** The Circle tab: the no-circle state, or the member list with nudges, limit suggestions and admin tools. */
@Composable
fun CircleScreen(
    state: BuddyState,
    joinProgress: JoinProgress,
    reachableIds: Set<String>,
    actions: CircleActions,
    snackbar: SnackbarHostState,
    bottomBar: @Composable () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onInvite: () -> Unit,
    onCircleSettings: () -> Unit
) {
    val group = state.group
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTitle(group?.name ?: "Circle") {
                if (group != null) {
                    IconButton(onClick = onCircleSettings) { Icon(Icons.Rounded.Settings, "Circle settings") }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = bottomBar
    ) { padding ->
        if (group == null) {
            NoCircleContent(joinProgress, onCreate, onJoin, Modifier.padding(padding))
        } else {
            CircleContent(state, reachableIds, actions, onInvite, Modifier.padding(padding))
        }
    }
}

@Composable
private fun NoCircleContent(
    joinProgress: JoinProgress,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Rounded.Groups, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Spend side by side", style = MaterialTheme.typography.headlineSmall)
        Text(
            "A circle is a small group of friends who see each other's day, send nudges and suggest limits. The admin approves everyone who joins.",
            style = MaterialTheme.typography.bodyLarge
        )
        if (joinProgress is JoinProgress.Waiting || joinProgress is JoinProgress.Requesting) {
            NotebookSurface(Modifier.fillMaxWidth()) {
                Text("Join request in progress", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Your request was sent. The admin has not answered yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onJoin, modifier = Modifier.heightIn(min = 48.dp)) { Text("See request") }
            }
        }
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
            Text("Create a circle", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
            Text("Join with a code", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "Your data goes straight between phones, encrypted. No account, and no server keeps a copy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CircleContent(
    state: BuddyState,
    reachableIds: Set<String>,
    actions: CircleActions,
    onInvite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nowMs = System.currentTimeMillis()
    val admin = isAdmin(state)
    val adminId = currentAdminId(state)
    val members = activeMembers(state)
    val me = members.firstOrNull { it.endpointId == state.selfId }
    val others = members.filter { it.endpointId != state.selfId }
    val pendingSuggestions = state.suggestions.filter { it.status == "pending" }
    val requests = state.pendingJoins.filter { it.expiresAt > nowMs }
    var suggestTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LazyColumn(
        modifier.fillMaxSize().scrollFadingEdges(listState, bottom = 40.dp),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (pendingSuggestions.isNotEmpty()) {
            item { SectionTitle("Limit suggestions for you") }
            items(pendingSuggestions, key = { it.id }) { suggestion ->
                IncomingSuggestionCard(suggestion, actions)
            }
        }
        if (admin) {
            item { SectionTitle("Admin") }
            item { AdminPanel(requests, members.size >= MAX_MEMBERS, nowMs, actions, onInvite) }
        }
        item {
            SectionTitle("Members (${members.size} of $MAX_MEMBERS)")
            val left = MAX_MEMBERS - members.size
            Text(
                if (left <= 0) "Your circle is full ($MAX_MEMBERS members)." else "${members.size} of $MAX_MEMBERS members. $left ${if (left == 1) "spot" else "spots"} left.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            MemberCard(
                name = "You",
                isSelf = true,
                isAdmin = me != null && me.endpointId == adminId,
                snapshot = null,
                state = state,
                reachable = true,
                nowMs = nowMs,
                memberId = state.selfId,
                actions = actions,
                onSuggest = {}
            )
        }
        items(others, key = { it.endpointId }) { member ->
            MemberCard(
                name = member.label(),
                isSelf = false,
                isAdmin = member.endpointId == adminId,
                snapshot = state.friends[member.endpointId],
                state = state,
                reachable = member.endpointId in reachableIds,
                nowMs = nowMs,
                memberId = member.endpointId,
                actions = actions,
                onSuggest = { suggestTarget = member.endpointId }
            )
        }
        if (others.isEmpty()) {
            item {
                Text(
                    "Nobody else is here yet. Share a code from the admin section so friends can ask to join.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Text(
                "Updates travel directly between phones, so they arrive when both phones can reach each other. Nudges and suggestions wait for the other phone; delivery is not instant.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val target = suggestTarget?.let { id -> others.firstOrNull { it.endpointId == id } }
    if (target != null) {
        SuggestLimitsSheet(
            target = target,
            snapshot = state.friends[target.endpointId],
            sent = state.sentSuggestions.filter { it.targetId == target.endpointId }.sortedByDescending { it.createdAt },
            actions = actions,
            onDismiss = { suggestTarget = null }
        )
    }
}

@Composable
private fun AdminPanel(
    requests: List<com.spendingbuddy.app.JoinRequest>,
    full: Boolean,
    nowMs: Long,
    actions: CircleActions,
    onInvite: () -> Unit
) {
    NotebookSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Join requests", style = MaterialTheme.typography.titleMedium)
            if (requests.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Pill("${requests.size} waiting")
            }
        }
        if (full) {
            Text(
                "Your circle is full ($MAX_MEMBERS members), so new requests cannot be approved right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Text(
            "Requests reach you while Penny Circle is open or Stay reachable is on. They wait up to 24 hours.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (requests.isEmpty()) {
            Text(
                "No one is waiting. Anyone with your code can ask to join, and you approve each request.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        requests.forEach { request ->
            val name = request.displayName.ifBlank { "Someone" }
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(
                    "$name asked to join",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Requested ${clockTime(request.requestedAt)}, ${agoText(request.requestedAt, nowMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val busy = actions.isBusy("approve:${request.id}") || actions.isBusy("decline:${request.id}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Button(
                        onClick = { actions.approve(request.id) },
                        enabled = !busy && !full,
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Approve $name" }
                    ) { Text("Approve") }
                    OutlinedButton(
                        onClick = { actions.decline(request.id) },
                        enabled = !busy,
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Decline $name" }
                    ) { Text("Decline") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onInvite, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (full) "Join code (circle full)" else "Show join code")
        }
    }
}

@Composable
private fun IncomingSuggestionCard(suggestion: BudgetSuggestion, actions: CircleActions) {
    val sender = suggestion.sender.ifBlank { "A friend" }
    val busy = actions.isBusy("respond:${suggestion.id}")
    NotebookSurface(Modifier.fillMaxWidth()) {
        Text(
            "$sender suggests ${rupees(suggestion.dailyPaise)} a day and ${rupees(suggestion.weeklyPaise)} a week",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "Your limits only change if you accept.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = { actions.respond(suggestion.id, false) },
                enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Keep mine") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { actions.respond(suggestion.id, true) },
                enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Accept") }
        }
    }
}

@Composable
private fun MemberCard(
    name: String,
    isSelf: Boolean,
    isAdmin: Boolean,
    snapshot: FriendSnapshot?,
    state: BuddyState,
    reachable: Boolean,
    nowMs: Long,
    memberId: String,
    actions: CircleActions,
    onSuggest: () -> Unit
) {
    val today = todayIndia()
    val todayText = today.toString()

    // Totals always come from the shared numbers, never from purchase records.
    val hasData = isSelf || (snapshot != null && snapshot.receivedAt > 0L)
    val checkedIn = if (isSelf) state.checkIns.any { it.date == todayText } else snapshot?.checkIns?.any { it.date == todayText } == true
    val streakCount = if (isSelf) streak(state.checkIns) else snapshot?.streak ?: 0
    val spent = if (isSelf) spentOn(state.expenses, today) else snapshot?.dailySpentPaise ?: 0L
    val limit = if (isSelf) state.dailyLimitPaise else snapshot?.dailyLimitPaise ?: 0L
    val visibility = if (isSelf) state.visibility else snapshot?.visibility ?: Visibility.TOTALS_STATUS

    val statusText = when {
        !hasData -> "No update received yet"
        checkedIn -> "Checked in"
        else -> "Not checked in yet"
    }
    val connectionText = when {
        isSelf -> null
        reachable -> "Reachable now"
        snapshot != null && snapshot.receivedAt > 0L -> "Last seen ${agoText(snapshot.receivedAt, nowMs)}"
        else -> "Waiting to connect"
    }
    val amountText = if (hasData && limit > 0L) "${rupees(spent)} of ${rupees(limit)} today" else if (hasData) "${rupees(spent)} today" else null
    val over = hasData && limit > 0L && spent > limit

    val summary = buildString {
        append(name)
        if (isAdmin) append(", admin")
        append(", ").append(statusText.lowercase())
        if (hasData) append(", streak $streakCount")
        if (amountText != null) append(", ").append(amountText)
        if (connectionText != null) append(", ").append(connectionText.lowercase())
    }

    NotebookSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = summary }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f, fill = false))
                if (isAdmin) {
                    Spacer(Modifier.width(8.dp))
                    Pill("Admin")
                }
            }
            Text(
                statusText + if (hasData) " · ${streakCount}-day streak" else "",
                style = MaterialTheme.typography.bodyLarge,
                color = if (checkedIn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (checkedIn) FontWeight.SemiBold else null
            )
            if (amountText != null) {
                Text(
                    amountText + if (over) ", over the limit by ${rupees(spent - limit)}" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (limit > 0L) ProgressBar(spent, limit, over, Modifier.padding(top = 8.dp))
            }
            if (connectionText != null) {
                Text(
                    connectionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Text(
                if (visibility == Visibility.FULL_DETAILS) "Shares full details" else "Shares totals and status only",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (!isSelf && snapshot != null && visibility == Visibility.FULL_DETAILS) {
            val purchases = snapshot.expenses.filter { it.date == todayText }.sortedByDescending { it.updatedAt }.take(5)
            purchases.forEach { PurchaseRow(it) }
        }

        if (!isSelf) {
            val pendingNudge = state.outgoingNudges.lastOrNull { it.targetId == memberId && it.deliveredAt == 0L }
            val allowed = canNudge(state, memberId)
            val nudgeNote = when {
                !hasData -> null
                checkedIn -> "$name has already checked in today."
                !allowed -> "You already nudged $name today. One nudge per day."
                else -> null
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { actions.nudge(memberId, name) },
                    enabled = allowed && !checkedIn && !actions.isBusy("nudge:$memberId"),
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Nudge $name" }
                ) { Text("Nudge") }
                OutlinedButton(
                    onClick = onSuggest,
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Suggest new limits to $name" }
                ) { Text("Suggest new limits") }
            }
            if (nudgeNote != null) {
                Text(
                    nudgeNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (pendingNudge != null) {
                Text(
                    "Nudge sent, waiting for $name's phone to receive it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SuggestLimitsSheet(
    target: Member,
    snapshot: FriendSnapshot?,
    sent: List<BudgetSuggestion>,
    actions: CircleActions,
    onDismiss: () -> Unit
) {
    val name = target.label()
    val id = target.endpointId
    var daily by rememberSaveable(id) { mutableStateOf(snapshot?.dailyLimitPaise?.takeIf { it > 0L }?.let { (it / 100L).toString() } ?: "") }
    var weekly by rememberSaveable(id) { mutableStateOf(snapshot?.weeklyLimitPaise?.takeIf { it > 0L }?.let { (it / 100L).toString() } ?: "") }
    var error by rememberSaveable(id) { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Suggest limits to $name", style = MaterialTheme.typography.headlineSmall)
            Text(
                "$name decides whether to accept. Their limits do not change unless they say yes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = daily,
                onValueChange = { daily = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Daily limit (₹)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = weekly,
                onValueChange = { weekly = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Weekly limit (₹)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            ErrorText(error)
            Button(
                onClick = {
                    val d = amountToPaise(daily)
                    val w = amountToPaise(weekly)
                    when {
                        d == null || d <= 0L -> error = "Enter a daily amount above ₹0."
                        w == null || w <= 0L -> error = "Enter a weekly amount above ₹0."
                        w < d -> error = "The weekly limit should be at least the daily limit."
                        else -> actions.suggest(id, d, w, name) { onDismiss() }
                    }
                },
                enabled = !actions.isBusy("suggest"),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("Send suggestion") }

            if (sent.isNotEmpty()) {
                SectionTitle("Your earlier suggestions", Modifier.padding(top = 6.dp))
                sent.take(5).forEach { s ->
                    val state = when (s.status) {
                        "accepted" -> "Accepted"
                        "declined" -> "Declined"
                        else -> if (s.delivered) "Pending, $name has it" else "Pending, waiting for $name's phone"
                    }
                    Text(
                        "${rupees(s.dailyPaise)} a day, ${rupees(s.weeklyPaise)} a week: $state",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.heightIn(min = 32.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
