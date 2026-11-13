package com.spendingbuddy.app.ui

import com.spendingbuddy.app.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.Visibility
import com.spendingbuddy.app.p2p.activeMembers
import com.spendingbuddy.app.p2p.isAdmin

/** Circle menu: what you share, admin handover and leaving. Reached from the gear on the Circle tab. */
@Composable
fun CircleSettingsScreen(
    state: BuddyState,
    actions: CircleActions,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onLeft: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val group = state.group
    val admin = isAdmin(state)
    val others = activeMembers(state).filter { it.endpointId != state.selfId }

    var transferTo by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmTransfer by rememberSaveable { mutableStateOf(false) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var successor by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { ScreenTitle(if (group == null) context.getString(R.string.circle_copy_circle) else context.getString(R.string.circle_copy_settings, group.name), onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (group == null) {
            Text(
                context.getString(R.string.circle_copy_you_are_not_in_a_circle),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(padding).padding(16.dp)
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).scrollFadingEdges(listState, bottom = 40.dp),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                NotebookSurface(Modifier.fillMaxWidth()) {
                    Text(context.getString(R.string.circle_copy_what_you_share), style = MaterialTheme.typography.titleMedium)
                    val busy = actions.isBusy("visibility")
                    ChoiceRow(
                        selected = state.visibility == Visibility.TOTALS_STATUS,
                        label = context.getString(R.string.circle_copy_totals_status),
                        supporting = context.getString(R.string.circle_copy_your_spending_totals_limits_check_in_status),
                        enabled = !busy,
                        onClick = { if (!busy && state.visibility != Visibility.TOTALS_STATUS) actions.setVisibility(Visibility.TOTALS_STATUS) }
                    )
                    ChoiceRow(
                        selected = state.visibility == Visibility.FULL_DETAILS,
                        label = context.getString(R.string.circle_copy_full_details),
                        supporting = context.getString(R.string.circle_copy_also_your_saved_purchase_details_including_auto),
                        enabled = !busy,
                        onClick = { if (!busy && state.visibility != Visibility.FULL_DETAILS) actions.setVisibility(Visibility.FULL_DETAILS) }
                    )
                    CircleActionFeedback(actions, "visibility", context.getString(R.string.circle_copy_updating_sharing))
                    Text(
                        context.getString(R.string.circle_copy_a_change_applies_from_now_on_anything),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            if (admin) {
                item {
                    NotebookSurface(Modifier.fillMaxWidth()) {
                        Text(context.getString(R.string.circle_copy_transfer_admin), style = MaterialTheme.typography.titleMedium)
                        Text(
                            context.getString(R.string.circle_copy_the_admin_approves_join_requests_pick_who),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (others.isEmpty()) {
                            Text(
                                context.getString(R.string.circle_copy_invite_another_member_from_circle_first_then),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            Column(Modifier.padding(top = 4.dp)) {
                                others.forEach { m ->
                                    ChoiceRow(
                                        selected = transferTo == m.endpointId,
                                        label = m.label(context),
                                        enabled = !actions.isBusy("transfer"),
                                        onClick = { transferTo = m.endpointId }
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { confirmTransfer = true },
                                enabled = transferTo != null && !actions.isBusy("transfer"),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) { Text(if (actions.isBusy("transfer")) context.getString(R.string.circle_copy_transferring) else context.getString(R.string.circle_copy_transfer_admin)) }
                            CircleActionFeedback(actions, "transfer", context.getString(R.string.circle_copy_transferring))
                        }
                    }
                }
            }

            item {
                NotebookSurface(Modifier.fillMaxWidth()) {
                    Text(context.getString(R.string.circle_copy_leave_circle), style = MaterialTheme.typography.titleMedium)
                    Text(
                        context.getString(R.string.circle_copy_your_own_data_stays_on_this_phone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { confirmLeave = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 48.dp),
                        enabled = !actions.isBusy("leave"),
                    ) { Text(if (actions.isBusy("leave")) context.getString(R.string.circle_copy_leaving) else context.getString(R.string.circle_copy_leave_circle)) }
                    CircleActionFeedback(actions, "leave", context.getString(R.string.circle_copy_leaving))
                }
            }
        }
    }

    val transferName = others.firstOrNull { it.endpointId == transferTo }?.label(context)
    if (confirmTransfer && transferTo != null && transferName != null) {
        AlertDialog(
            onDismissRequest = { confirmTransfer = false },
            title = { Text(context.getString(R.string.circle_copy_make_the_admin, transferName)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(context.getString(R.string.circle_copy_will_approve_join_requests_and_make_join, transferName))
                    CircleActionFeedback(actions, "transfer", context.getString(R.string.circle_copy_transferring))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = transferTo ?: return@Button
                        actions.transferAdmin(id) { confirmTransfer = false; transferTo = null }
                    },
                    enabled = !actions.isBusy("transfer"),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(if (actions.isBusy("transfer")) context.getString(R.string.circle_copy_transferring) else context.getString(R.string.circle_copy_transfer)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmTransfer = false }, enabled = !actions.isBusy("transfer"), modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.circle_copy_cancel)) }
            }
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(context.getString(R.string.circle_copy_leave, group?.name ?: context.getString(R.string.circle_copy_this_circle))) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (admin && others.isNotEmpty()) {
                        Text(context.getString(R.string.circle_copy_you_are_the_admin_so_the_role))
                        Column(Modifier.padding(top = 8.dp)) {
                            ChoiceRow(selected = successor == null, label = context.getString(R.string.circle_copy_longest_standing_member), enabled = !actions.isBusy("leave"), onClick = { successor = null })
                            others.forEach { m ->
                                ChoiceRow(selected = successor == m.endpointId, label = m.label(context), enabled = !actions.isBusy("leave"), onClick = { successor = m.endpointId })
                            }
                        }
                    } else if (admin) {
                        Text(context.getString(R.string.circle_copy_you_are_the_only_member_so_nobody))
                    } else {
                        Text(context.getString(R.string.circle_copy_the_circle_is_told_that_you_left))
                    }
                    CircleActionFeedback(actions, "leave", context.getString(R.string.circle_copy_leaving))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        actions.leave(if (admin) successor else null) { confirmLeave = false; successor = null; onLeft() }
                    },
                    enabled = !actions.isBusy("leave"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(if (actions.isBusy("leave")) context.getString(R.string.circle_copy_leaving) else context.getString(R.string.circle_copy_leave_2)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }, enabled = !actions.isBusy("leave"), modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.circle_copy_stay)) }
            }
        )
    }
}
