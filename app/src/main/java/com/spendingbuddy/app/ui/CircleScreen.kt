@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.spendingbuddy.app.ui

import com.spendingbuddy.app.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendingbuddy.app.BudgetSuggestion
import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.FriendSnapshot
import com.spendingbuddy.app.ImpulseStats
import com.spendingbuddy.app.impulseStats
import com.spendingbuddy.app.Member
import com.spendingbuddy.app.Visibility
import com.spendingbuddy.app.dailyLimitInputError
import com.spendingbuddy.app.formatLimitInput
import com.spendingbuddy.app.parseOnboardingLimits
import com.spendingbuddy.app.weeklyLimitInputError
import com.spendingbuddy.app.MAX_MEMBERS
import com.spendingbuddy.app.p2p.JoinProgress
import com.spendingbuddy.app.p2p.MemberLink
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
    onCircleSettings: () -> Unit,
    memberLinks: Map<String, MemberLink> = emptyMap()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val group = state.group
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            ScreenTitle(context.getString(R.string.circle_copy_circle)) {
                if (group != null) {
                    IconButton(onClick = onCircleSettings) { Icon(Icons.Rounded.Settings, context.getString(R.string.circle_copy_circle_settings)) }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = bottomBar
    ) { padding ->
        if (group == null) {
            NoCircleContent(joinProgress, onCreate, onJoin, Modifier.padding(padding))
        } else {
            CircleContent(state, reachableIds, actions, onInvite, Modifier.padding(padding), memberLinks)
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
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val tokens = LocalPennyTokens.current
        NotebookSurface(Modifier.fillMaxWidth().enterRise(0)) {
            Box(
                Modifier.size(52.dp).background(
                    Brush.linearGradient(listOf(tokens.accent.copy(alpha = 0.30f), tokens.accent2.copy(alpha = 0.22f))),
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Groups, null, Modifier.size(28.dp), tint = if (tokens.dark) Color.White else tokens.accent) }
            Spacer(Modifier.height(14.dp))
            Text(context.getString(R.string.circle_copy_spend_side_by_side), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                context.getString(R.string.circle_copy_a_circle_is_a_small_group_of),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (joinProgress is JoinProgress.Waiting || joinProgress is JoinProgress.Requesting) {
            NotebookSurface(Modifier.fillMaxWidth()) {
                Text(context.getString(R.string.circle_copy_join_request_in_progress), style = MaterialTheme.typography.titleMedium)
                Text(
                    context.getString(if (joinProgress is JoinProgress.Requesting) R.string.circle_copy_sending_your_request else R.string.circle_copy_your_request_was_sent_the_admin_has),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onJoin, modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.circle_copy_see_request)) }
            }
        }
        AccentButton(onClick = onCreate, modifier = Modifier.fillMaxWidth().enterRise(1)) {
            Text(context.getString(R.string.circle_copy_create_a_circle))
        }
        SecondaryButton(onClick = onJoin, modifier = Modifier.fillMaxWidth().enterRise(2)) {
            Text(context.getString(R.string.circle_copy_join_with_a_code))
        }
        Text(
            context.getString(R.string.circle_copy_your_data_goes_straight_between_phones_encrypted),
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
    modifier: Modifier = Modifier,
    memberLinks: Map<String, MemberLink> = emptyMap()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
        state.group?.let { group -> item { Text(group.name, style = MaterialTheme.typography.titleLarge) } }
        if (pendingSuggestions.isNotEmpty()) {
            item { SectionTitle(context.getString(R.string.circle_copy_limit_suggestions_for_you)) }
            items(pendingSuggestions, key = { it.id }) { suggestion ->
                IncomingSuggestionCard(suggestion, actions)
            }
        }
        if (admin) {
            item { SectionTitle(context.getString(R.string.circle_copy_admin)) }
            item { AdminPanel(requests, members.size >= MAX_MEMBERS, nowMs, actions, onInvite) }
        }
        item {
            SectionTitle(context.getString(R.string.circle_copy_members_of, members.size, MAX_MEMBERS))
            val left = MAX_MEMBERS - members.size
            Text(
                if (left <= 0) context.getString(R.string.circle_copy_your_circle_is_full_members, MAX_MEMBERS) else context.resources.getQuantityString(R.plurals.circle_members_spots, left, members.size, MAX_MEMBERS, left),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            MemberCard(
                name = context.getString(R.string.circle_copy_you),
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
                name = member.label(context),
                isSelf = false,
                isAdmin = member.endpointId == adminId,
                snapshot = state.friends[member.endpointId],
                state = state,
                reachable = member.endpointId in reachableIds,
                nowMs = nowMs,
                memberId = member.endpointId,
                actions = actions,
                onSuggest = { suggestTarget = member.endpointId },
                link = memberLinks[member.endpointId]
            )
        }
        if (others.isEmpty()) {
            item {
                Text(
                    context.getString(R.string.circle_copy_nobody_else_is_here_yet_share_a),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Text(
                context.getString(R.string.circle_copy_updates_travel_directly_between_phones_so_they),
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
    val context = androidx.compose.ui.platform.LocalContext.current
    var declineId by rememberSaveable { mutableStateOf<String?>(null) }
    val declining = requests.firstOrNull { it.id == declineId }
    NotebookSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(context.getString(R.string.circle_copy_join_requests), style = MaterialTheme.typography.titleMedium)
            if (requests.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                Pill(context.getString(R.string.circle_copy_waiting, requests.size))
            }
        }
        if (full) {
            Text(
                context.getString(R.string.circle_copy_your_circle_is_full_members_so_new, MAX_MEMBERS),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Text(
            context.getString(R.string.circle_copy_requests_reach_you_while_penny_circle_is),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (requests.isEmpty()) {
            Text(
                context.getString(R.string.circle_copy_no_one_is_waiting_anyone_with_your),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        requests.forEach { request ->
            val name = request.displayName.ifBlank { context.getString(R.string.circle_copy_someone) }
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(
                    context.getString(R.string.circle_copy_asked_to_join, name),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    context.getString(R.string.circle_copy_requested, clockTime(request.requestedAt), agoText(context, request.requestedAt, nowMs)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val busy = actions.isBusy("approve:${request.id}") || actions.isBusy("decline:${request.id}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Button(
                        onClick = { actions.approve(request.id) },
                        enabled = !busy && !full,
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = context.getString(R.string.circle_copy_approve, name) }
                    ) { Text(if (actions.isBusy("approve:${request.id}")) context.getString(R.string.circle_copy_approving) else context.getString(R.string.circle_copy_approve_2)) }
                }
                TextButton(
                    onClick = { actions.clearFailure("decline:${request.id}"); declineId = request.id },
                    enabled = !busy,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = context.getString(R.string.circle_copy_decline, name) }
                ) { Text(context.getString(R.string.circle_copy_decline_2)) }
                CircleActionFeedback(actions, "approve:${request.id}", context.getString(R.string.circle_copy_approving))
                CircleActionFeedback(actions, "decline:${request.id}", context.getString(R.string.circle_copy_declining))
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onInvite, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (full) context.getString(R.string.circle_copy_join_code_circle_full) else context.getString(R.string.circle_copy_show_join_code))
        }
    }
    if (declining != null) {
        val name = declining.displayName.ifBlank { context.getString(R.string.circle_copy_someone) }
        val key = "decline:${declining.id}"
        AlertDialog(
            onDismissRequest = { declineId = null },
            title = { Text(context.getString(R.string.circle_copy_decline_s_request, name)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(context.getString(R.string.circle_copy_this_circle_will_reject_further_requests_from))
                CircleActionFeedback(actions, key, context.getString(R.string.circle_copy_declining))
            } },
            confirmButton = { Button(
                onClick = { actions.decline(declining.id) },
                enabled = !actions.isBusy(key),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
            ) { Text(if (actions.isBusy(key)) context.getString(R.string.circle_copy_declining) else context.getString(R.string.circle_copy_decline_2)) } },
            dismissButton = { TextButton(onClick = { declineId = null }, enabled = !actions.isBusy(key)) { Text(context.getString(R.string.circle_copy_cancel)) } }
        )
    }
}

@Composable
private fun IncomingSuggestionCard(suggestion: BudgetSuggestion, actions: CircleActions) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sender = suggestion.sender.ifBlank { context.getString(R.string.circle_copy_a_friend) }
    val busy = actions.isBusy("respond:${suggestion.id}")
    NotebookSurface(Modifier.fillMaxWidth()) {
        Text(
            context.getString(R.string.circle_copy_suggests_a_day_and_a_week, sender, rupees(suggestion.dailyPaise), rupees(suggestion.weeklyPaise)),
            style = MaterialTheme.typography.bodyLarge
        )
        CircleActionFeedback(actions, "respond:${suggestion.id}", context.getString(R.string.circle_copy_sending_your_answer))
        Text(
            context.getString(R.string.circle_copy_your_limits_only_change_if_you_accept),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = { actions.respond(suggestion.id, false) },
                enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(context.getString(R.string.circle_copy_keep_mine)) }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { actions.respond(suggestion.id, true) },
                enabled = !busy,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(context.getString(R.string.circle_copy_accept)) }
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
    onSuggest: () -> Unit,
    link: MemberLink? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
        !hasData -> context.getString(R.string.circle_copy_no_update_received_yet)
        checkedIn -> context.getString(R.string.circle_copy_checked_in)
        else -> context.getString(R.string.circle_copy_not_checked_in_yet)
    }
    val connectionText = when {
        isSelf -> null
        reachable -> context.getString(R.string.circle_copy_reachable_now)
        snapshot != null && snapshot.receivedAt > 0L -> context.getString(R.string.circle_copy_last_seen, agoText(context, snapshot.receivedAt, nowMs))
        else -> context.getString(R.string.circle_copy_waiting_to_connect)
    }
    val amountText = if (hasData && limit > 0L) context.getString(R.string.circle_copy_of_today, rupees(spent), rupees(limit)) else if (hasData) context.getString(R.string.circle_copy_today, rupees(spent)) else null
    val over = hasData && limit > 0L && spent > limit
    // Impulse habit: shared as totals only. Older apps do not send it (impulseFreeDays = -1), so nothing is shown.
    val impulse = when {
        isSelf -> impulseStats(state.expenses, today)
        snapshot != null && hasData && snapshot.impulseFreeDays >= 0 -> ImpulseStats(snapshot.impulseWeekPaise, snapshot.impulseWeekCount, snapshot.impulseFreeDays)
        else -> null
    }
    val impulseText = impulse?.let {
        if (it.weekCount > 0) context.resources.getQuantityString(R.plurals.circle_impulse_week, it.freeDays, it.freeDays, rupees(it.weekPaise))
        else context.resources.getQuantityString(R.plurals.circle_impulse_free, it.freeDays, it.freeDays)
    }

    NotebookSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f, fill = false))
                if (isAdmin) {
                    Spacer(Modifier.width(8.dp))
                    Pill(context.getString(R.string.circle_copy_admin))
                }
            }
            Text(
                if (hasData) context.getString(R.string.circle_status_streak, statusText, streakCount) else statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (checkedIn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = if (hasData) context.getString(R.string.circle_status_named_streak, name, statusText, streakCount)
                        else context.getString(R.string.circle_copy_template, name, statusText)
                },
                fontWeight = if (checkedIn) FontWeight.SemiBold else null
            )
            if (amountText != null) {
                Text(
                    if (over) context.getString(R.string.circle_amount_over, amountText, rupees(spent - limit)) else amountText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (limit > 0L) ProgressBar(spent, limit, over, Modifier.padding(top = 8.dp))
            }
            if (impulseText != null) {
                Text(
                    impulseText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (impulse != null && impulse.freeDays == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (connectionText != null) {
                val conciseConnection = when {
                    link?.error != null -> context.getString(R.string.circle_copy_connection_issue)
                    reachable -> context.getString(R.string.circle_copy_reachable_now)
                    else -> context.getString(R.string.circle_copy_waiting_to_connect)
                }
                Text(conciseConnection, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; contentDescription = context.getString(R.string.circle_copy_template, name, conciseConnection) })
                if (connectionText != conciseConnection) {
                Text(
                    connectionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
                }
            }
            Text(
                if (visibility == Visibility.FULL_DETAILS) context.getString(R.string.circle_copy_shares_full_details) else context.getString(R.string.circle_copy_shares_totals_and_status_only),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (!isSelf && link?.error != null) {
            Text(context.getString(R.string.circle_copy_connection_issue_2, link.error), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            Text(if (link.lastExchangeAt > 0L) context.getString(R.string.circle_copy_last_successful_exchange, agoText(context, link.lastExchangeAt, nowMs)) else context.getString(R.string.circle_copy_no_successful_exchange_yet),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { actions.retryMember(memberId) }, enabled = !actions.isBusy("retry:$memberId")) {
                Text(if (actions.isBusy("retry:$memberId")) context.getString(R.string.circle_copy_checking) else context.getString(R.string.circle_copy_try_again))
            }
            CircleActionFeedback(actions, "retry:$memberId", context.getString(R.string.circle_copy_checking))
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
                checkedIn -> context.getString(R.string.circle_copy_has_already_checked_in_today, name)
                !allowed -> context.getString(R.string.circle_copy_you_already_nudged_today_one_nudge_per, name)
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
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = context.getString(R.string.circle_copy_nudge, name) }
                ) { Text(if (actions.isBusy("nudge:$memberId")) context.getString(R.string.circle_copy_sending_nudge) else context.getString(R.string.circle_copy_nudge_2)) }
                OutlinedButton(
                    onClick = onSuggest,
                    modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = context.getString(R.string.circle_copy_suggest_new_limits_to, name) }
                ) { Text(context.getString(R.string.circle_copy_suggest_new_limits)) }
            }
            CircleActionFeedback(actions, "nudge:$memberId", context.getString(R.string.circle_copy_sending_nudge))
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
                    context.getString(R.string.circle_copy_nudge_sent_waiting_for_s_phone_to, name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite }
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val name = target.label(context)
    val id = target.endpointId
    var daily by rememberSaveable(id) { mutableStateOf(snapshot?.dailyLimitPaise?.let(::formatLimitInput) ?: "") }
    var weekly by rememberSaveable(id) { mutableStateOf(snapshot?.weeklyLimitPaise?.let(::formatLimitInput) ?: "") }
    var dailyError by rememberSaveable(id) { mutableStateOf<String?>(null) }
    var weeklyError by rememberSaveable(id) { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        DialogSystemBars()
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).navigationBarsPadding().imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(context.getString(R.string.circle_copy_suggest_limits_to, name), style = MaterialTheme.typography.headlineSmall)
            Text(context.getString(R.string.circle_copy_all_fields_are_required_unless_marked_optional), style = MaterialTheme.typography.bodyMedium)
            Text(
                context.getString(R.string.circle_copy_decides_whether_to_accept_their_limits_do, name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = daily,
                onValueChange = { daily = it; if (dailyError != null) dailyError = dailyLimitInputError(it) },
                modifier = Modifier.fillMaxWidth().semantics { dailyError?.let { error(it) } },
                label = { Text(context.getString(R.string.circle_copy_daily_limit)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = dailyError != null,
                supportingText = dailyError?.let { message -> { Text(context.budgetValidationMessage(message).orEmpty(), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) } }
            )
            OutlinedTextField(
                value = weekly,
                onValueChange = { weekly = it; if (weeklyError != null) weeklyError = weeklyLimitInputError(it) },
                modifier = Modifier.fillMaxWidth().semantics { weeklyError?.let { error(it) } },
                label = { Text(context.getString(R.string.circle_copy_weekly_limit)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = weeklyError != null,
                supportingText = weeklyError?.let { message -> { Text(context.budgetValidationMessage(message).orEmpty(), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) } }
            )
            Button(
                onClick = {
                    dailyError = dailyLimitInputError(daily)
                    weeklyError = weeklyLimitInputError(weekly)
                    parseOnboardingLimits(daily, weekly)?.let {
                        actions.suggest(id, it.dailyPaise, it.weeklyPaise, name) { onDismiss() }
                    }
                },
                enabled = !actions.isBusy("suggest"),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(if (actions.isBusy("suggest")) context.getString(R.string.circle_copy_sending_suggestion) else context.getString(R.string.circle_copy_send_suggestion)) }
            CircleActionFeedback(actions, "suggest", context.getString(R.string.circle_copy_sending_suggestion))

            if (sent.isNotEmpty()) {
                SectionTitle(context.getString(R.string.circle_copy_your_earlier_suggestions), Modifier.padding(top = 6.dp))
                sent.take(5).forEach { s ->
                    val state = when (s.status) {
                        "accepted" -> context.getString(R.string.circle_copy_accepted)
                        "declined" -> context.getString(R.string.circle_copy_declined)
                        else -> if (s.delivered) context.getString(R.string.circle_copy_pending_has_it, name) else context.getString(R.string.circle_copy_pending_waiting_for_s_phone, name)
                    }
                    Text(
                        context.getString(R.string.circle_copy_a_day_a_week, rupees(s.dailyPaise), rupees(s.weeklyPaise), state),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.heightIn(min = 32.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
