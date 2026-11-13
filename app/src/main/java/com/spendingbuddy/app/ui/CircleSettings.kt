package com.spendingbuddy.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
        topBar = { ScreenTitle(if (group == null) "Circle" else "${group.name} settings", onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (group == null) {
            Text(
                "You are not in a circle.",
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
                    Text("What you share", style = MaterialTheme.typography.titleMedium)
                    val busy = actions.isBusy("visibility")
                    ChoiceRow(
                        selected = state.visibility == Visibility.TOTALS_STATUS,
                        label = "Totals & status",
                        supporting = "Your spending totals, limits, check-in status and streak. No purchase details.",
                        onClick = { if (!busy && state.visibility != Visibility.TOTALS_STATUS) actions.setVisibility(Visibility.TOTALS_STATUS) }
                    )
                    ChoiceRow(
                        selected = state.visibility == Visibility.FULL_DETAILS,
                        label = "Full details",
                        supporting = "Also the purchases you have reviewed and saved.",
                        onClick = { if (!busy && state.visibility != Visibility.FULL_DETAILS) actions.setVisibility(Visibility.FULL_DETAILS) }
                    )
                    Text(
                        "A change applies from now on. Anything already shared with the circle cannot be recalled.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }

            if (admin) {
                item {
                    NotebookSurface(Modifier.fillMaxWidth()) {
                        Text("Transfer admin", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "The admin approves join requests. Pick who takes over; you will stay a regular member.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (others.isEmpty()) {
                            Text(
                                "No one else is in the circle yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            Column(Modifier.padding(top = 4.dp)) {
                                others.forEach { m ->
                                    ChoiceRow(
                                        selected = transferTo == m.endpointId,
                                        label = m.label(),
                                        onClick = { transferTo = m.endpointId }
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { confirmTransfer = true },
                                enabled = transferTo != null && !actions.isBusy("transfer"),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) { Text("Transfer admin") }
                        }
                    }
                }
            }

            item {
                NotebookSurface(Modifier.fillMaxWidth()) {
                    Text("Leave circle", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Your own data stays on this phone. You stop seeing the circle and it stops seeing you. You cannot rejoin with this phone's identity unless the admin approves a new request.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { confirmLeave = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 48.dp)
                    ) { Text("Leave circle") }
                }
            }
        }
    }

    val transferName = others.firstOrNull { it.endpointId == transferTo }?.label()
    if (confirmTransfer && transferTo != null && transferName != null) {
        AlertDialog(
            onDismissRequest = { confirmTransfer = false },
            title = { Text("Make $transferName the admin?") },
            text = {
                Text("$transferName will approve join requests and make join codes. You will no longer be the admin, and you cannot take the role back yourself.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = transferTo ?: return@Button
                        confirmTransfer = false
                        actions.transferAdmin(id) { transferTo = null }
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Transfer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmTransfer = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
            }
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Leave ${group?.name ?: "this circle"}?") },
            text = {
                Column {
                    if (admin && others.isNotEmpty()) {
                        Text("You are the admin, so the role goes to someone else when you leave. Choose who, or let the longest-standing member take it.")
                        Column(Modifier.padding(top = 8.dp)) {
                            ChoiceRow(selected = successor == null, label = "Longest-standing member", onClick = { successor = null })
                            others.forEach { m ->
                                ChoiceRow(selected = successor == m.endpointId, label = m.label(), onClick = { successor = m.endpointId })
                            }
                        }
                    } else if (admin) {
                        Text("You are the only member, so nobody needs to take over as admin. Your circle's code stops working.")
                    } else {
                        Text("The circle is told that you left. Your own data stays on this phone.")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmLeave = false
                        actions.leave(if (admin) successor else null) { successor = null; onLeft() }
                    },
                    enabled = !actions.isBusy("leave"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Stay") }
            }
        )
    }
}
