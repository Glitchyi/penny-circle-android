package com.spendingbuddy.app.ui

import com.spendingbuddy.app.R

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.MAX_MEMBERS
import com.spendingbuddy.app.p2p.JoinProgress
import com.spendingbuddy.app.p2p.LinkPhase
import com.spendingbuddy.app.p2p.PeerStatus
import com.spendingbuddy.app.p2p.activeMembers
import com.spendingbuddy.app.p2p.MAX_GROUP_NAME
import com.spendingbuddy.app.p2p.isValidGroupName
import com.spendingbuddy.app.p2p.parseJoinCode

// ---- Create a circle -------------------------------------------------------------------------------------------

@Composable
fun CircleCreateScreen(
    actions: CircleActions,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    draftName: String = "",
    onDraftNameChange: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by rememberSaveable { mutableStateOf(draftName) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { ScreenTitle(context.getString(R.string.circle_copy_create_a_circle), onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(context.getString(R.string.circle_copy_name_your_circle), style = MaterialTheme.typography.headlineSmall)
            Text(context.getString(R.string.circle_copy_all_fields_are_required_unless_marked_optional), style = MaterialTheme.typography.bodyMedium)
            Text(
                context.getString(R.string.circle_copy_you_will_be_the_admin_you_approve),
                style = MaterialTheme.typography.bodyLarge
            )
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= MAX_GROUP_NAME) { name = it; onDraftNameChange(it) }; error = null; actions.clearFailure("create") },
                modifier = Modifier.fillMaxWidth().semantics { error?.let { this.error(it) } },
                label = { Text(context.getString(R.string.circle_copy_circle_name)) },
                singleLine = true,
                isError = error != null,
                supportingText = { Text(error ?: context.getString(R.string.circle_copy_up_to_characters, MAX_GROUP_NAME), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            )
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (!isValidGroupName(trimmed)) {
                        error = context.getString(R.string.circle_copy_enter_a_name_between_1_and_characters, MAX_GROUP_NAME)
                    } else {
                        actions.createCircle(trimmed, onCreated)
                    }
                },
                enabled = !actions.isBusy("create"),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text(if (actions.isBusy("create")) context.getString(R.string.circle_copy_creating) else context.getString(R.string.circle_copy_create_circle)) }
            CircleActionFeedback(actions, "create", context.getString(R.string.circle_copy_creating))
        }
    }
}

// ---- Share code (admin) ----------------------------------------------------------------------------------------

@Composable
fun CircleInviteScreen(
    state: BuddyState,
    actions: CircleActions,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    peerStatus: PeerStatus = PeerStatus(),
    stayReachable: Boolean = state.stayReachable,
    makeQr: ((String) -> Bitmap)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var code by rememberSaveable { mutableStateOf("") }
    var confirmReplace by rememberSaveable { mutableStateOf(false) }
    val memberCount = activeMembers(state).size
    val full = memberCount >= MAX_MEMBERS
    val clipboard = LocalClipboardManager.current
    val nowMs = System.currentTimeMillis()
    val groupName = state.group?.name.orEmpty()

    LaunchedEffect(Unit) {
        if (code.isEmpty() && !full) actions.issueCode(false) { code = it }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { ScreenTitle(if (groupName.isBlank()) context.getString(R.string.circle_copy_join_code) else context.getString(R.string.circle_copy_join_code_for, groupName), onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (full) {
                NotebookSurface(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                    Text(context.getString(R.string.circle_copy_your_circle_is_full_members_2, MAX_MEMBERS), style = MaterialTheme.typography.titleMedium)
                    Text(
                        context.getString(R.string.circle_copy_new_join_codes_and_requests_are_paused),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else NotebookSurface(Modifier.fillMaxWidth()) {
                Text(context.getString(R.string.circle_copy_share_this_code), style = MaterialTheme.typography.titleMedium)
                Text(
                    context.getString(R.string.circle_copy_of_members, memberCount, MAX_MEMBERS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (code.isEmpty()) {
                    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (actions.isBusy("code")) CircularProgressIndicator(Modifier.padding(end = 12.dp))
                        Text(
                            if (actions.isBusy("code")) context.getString(R.string.circle_copy_making_a_code) else context.getString(R.string.circle_copy_no_code_yet),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (!actions.isBusy("code")) {
                        OutlinedButton(
                            onClick = { actions.issueCode(false) { code = it } },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(context.getString(R.string.circle_copy_try_again)) }
                    }
                } else {
                    Text(
                        code,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    if (makeQr != null && !full) {
                        val qr = remember(code) { runCatching { makeQr(code).asImageBitmap() }.getOrNull() }
                        if (qr != null) {
                            Image(
                                bitmap = qr,
                                contentDescription = context.getString(R.string.circle_copy_qr_of_the_join_code_the_other),
                                modifier = Modifier.padding(vertical = 8.dp).size(220.dp)
                            )
                        }
                    }
                }
                val hours = if (state.joinNonceExpiresAt > nowMs) hoursLeft(state.joinNonceExpiresAt, nowMs) else 24L
                Text(
                    context.getString(R.string.circle_copy_expires_in_h, hours),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { actions.share(code) },
                    enabled = code.isNotEmpty() && !full && !actions.isBusy("code"),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text(context.getString(R.string.circle_copy_share)) }
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        actions.notify(context.getString(R.string.circle_copy_code_copied))
                    },
                    enabled = code.isNotEmpty() && !full && !actions.isBusy("code"),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text(context.getString(R.string.circle_copy_copy)) }
            }
            OutlinedButton(
                onClick = { confirmReplace = true },
                enabled = !actions.isBusy("code") && !full,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(if (actions.isBusy("code")) context.getString(R.string.circle_copy_making_a_code) else context.getString(R.string.circle_copy_create_new_code)) }
            CircleActionFeedback(actions, "code", context.getString(R.string.circle_copy_making_a_code))
            NotebookSurface(Modifier.fillMaxWidth()) {
                Text(context.getString(R.string.circle_copy_keep_penny_circle_open), style = MaterialTheme.typography.titleMedium)
                Text(
                    context.getString(R.string.circle_copy_keep_penny_circle_open_or_turn_on),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                val reach = when (peerStatus.phase) {
                    LinkPhase.Online -> context.getString(R.string.circle_copy_online_now)
                    LinkPhase.Starting -> context.getString(R.string.circle_copy_connecting)
                    LinkPhase.NoNetwork -> context.getString(R.string.circle_copy_no_network)
                    LinkPhase.Error -> context.getString(R.string.circle_copy_not_reachable_right_now)
                    LinkPhase.Off -> context.getString(R.string.circle_copy_not_listening_right_now)
                }
                Text(
                    context.getString(if (peerStatus.phase == LinkPhase.Starting) R.string.circle_copy_right_now_stay_reachable_is else R.string.circle_reach_state, reach, context.getString(if (stayReachable) R.string.circle_state_on else R.string.circle_state_off)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp).semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Text(
                context.getString(R.string.circle_copy_a_code_on_its_own_does_not),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    if (confirmReplace) AlertDialog(
        onDismissRequest = { confirmReplace = false },
        title = { Text(context.getString(R.string.circle_copy_replace_the_join_code)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(context.getString(R.string.circle_copy_the_code_you_already_shared_will_stop))
                CircleActionFeedback(actions, "code", context.getString(R.string.circle_copy_making_a_code))
            }
        },
        confirmButton = {
            Button(onClick = {
                actions.issueCode(true) { code = it; confirmReplace = false; actions.notify(context.getString(R.string.circle_copy_new_code_ready_the_old_one_no)) }
            }, enabled = !actions.isBusy("code")) { Text(if (actions.isBusy("code")) context.getString(R.string.circle_copy_making_a_code) else context.getString(R.string.circle_copy_create_new_code)) }
        },
        dismissButton = { TextButton(onClick = { confirmReplace = false }, enabled = !actions.isBusy("code")) { Text(context.getString(R.string.circle_copy_keep_current_code)) } }
    )
}

// ---- Join with a code ------------------------------------------------------------------------------------------

private fun codeProblem(text: String, context: android.content.Context): String? {
    if (text.isBlank()) return context.getString(R.string.circle_copy_paste_or_scan_a_circle_code)
    return try {
        if (parseJoinCode(text).expired) context.getString(R.string.circle_copy_this_code_has_expired_ask_the_admin) else null
    } catch (e: IllegalArgumentException) {
        context.getString(R.string.circle_copy_that_circle_code_is_not_valid_ask)
    }
}

@Composable
fun CircleJoinScreen(
    joinProgress: JoinProgress,
    inCircle: Boolean,
    actions: CircleActions,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onJoined: () -> Unit,
    onScan: (((String) -> Unit) -> Unit)? = null,
    draftCode: String = "",
    onDraftCodeChange: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var code by rememberSaveable { mutableStateOf(draftCode) }
    var touched by rememberSaveable { mutableStateOf(false) }
    var wasFocused by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val problem = codeProblem(code, context)

    LaunchedEffect(inCircle, joinProgress is JoinProgress.Joined) {
        if (inCircle || joinProgress is JoinProgress.Joined) onJoined()
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { ScreenTitle(context.getString(R.string.circle_copy_join_with_a_code), onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (joinProgress) {
                is JoinProgress.Requesting -> {
                    NotebookSurface(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.padding(end = 12.dp))
                            Text(context.getString(R.string.circle_copy_sending_your_request), style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            context.getString(R.string.circle_copy_this_can_take_a_moment_if_the),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        TextButton(onClick = { actions.cancelJoin { onDraftCodeChange(""); onBack() } }, modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.circle_copy_cancel)) }
                    }
                }

                is JoinProgress.Waiting -> {
                    NotebookSurface(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                        Text(context.getString(R.string.circle_copy_request_sent_waiting_for_the_admin_to), style = MaterialTheme.typography.titleMedium)
                        Text(
                            context.getString(R.string.circle_copy_the_admin_has_to_open_penny_circle, clockTime(joinProgress.expiresAt), hoursLeft(joinProgress.expiresAt, System.currentTimeMillis())),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { actions.checkJoin() },
                                enabled = !actions.isBusy("join"),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) { Text(if (actions.isBusy("join")) context.getString(R.string.circle_copy_checking) else context.getString(R.string.circle_copy_check_again)) }
                            TextButton(onClick = { actions.cancelJoin { onDraftCodeChange(""); onBack() } }, modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.circle_copy_cancel)) }
                        }
                        CircleActionFeedback(actions, "join", context.getString(R.string.circle_copy_checking))
                        Text(context.getString(R.string.circle_cancel_request_help), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                else -> {
                    if (joinProgress is JoinProgress.Refused) {
                        NotebookSurface(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                            Text(context.getString(R.string.circle_copy_the_circle_could_not_take_this_request), style = MaterialTheme.typography.titleMedium)
                            Text(
                                joinProgress.reason.ifBlank { context.getString(R.string.circle_copy_no_reason_was_given) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                    Text(context.getString(R.string.circle_copy_paste_the_circle_code), style = MaterialTheme.typography.headlineSmall)
                    Text(context.getString(R.string.circle_copy_all_fields_are_required_unless_marked_optional), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        context.getString(R.string.circle_copy_ask_the_circles_admin_to_share_a),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it; onDraftCodeChange(it); actions.clearFailure("join") },
                        modifier = Modifier.fillMaxWidth().onFocusChanged {
                            if (it.isFocused) wasFocused = true else if (wasFocused) touched = true
                        }.semantics { if (touched && problem != null) error(problem) },
                        minLines = 3,
                        label = { Text(context.getString(R.string.circle_copy_circle_code)) },
                        isError = problem != null && touched,
                        supportingText = {
                            Text(if (problem != null && touched) problem else context.getString(R.string.circle_copy_starts_with_pc1), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                    OutlinedButton(
                        onClick = {
                            val pasted = clipboard.getText()?.text.orEmpty()
                            if (pasted.isBlank()) actions.notify(context.getString(R.string.circle_copy_the_clipboard_is_empty)) else { code = pasted; onDraftCodeChange(pasted) }
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(context.getString(R.string.circle_copy_paste_from_clipboard)) }
                    if (onScan != null) {
                        OutlinedButton(
                            onClick = { onScan { scanned -> code = scanned.trim(); onDraftCodeChange(code); touched = true } },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text(context.getString(R.string.circle_copy_scan_a_qr_code)) }
                    }
                    Button(
                        onClick = {
                            touched = true
                            if (codeProblem(code, context) == null) actions.join(code)
                        },
                        enabled = !actions.isBusy("join"),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) { Text(context.getString(R.string.circle_copy_send_request)) }
                    CircleActionFeedback(actions, "join", context.getString(R.string.circle_copy_sending_your_request))
                }
            }
        }
    }
}
