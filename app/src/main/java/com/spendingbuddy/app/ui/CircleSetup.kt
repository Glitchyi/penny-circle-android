package com.spendingbuddy.app.ui

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
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
    onCreated: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { ScreenTitle("Create a circle", onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Name your circle", style = MaterialTheme.typography.headlineSmall)
            Text(
                "You will be the admin. You approve everyone who asks to join with your code.",
                style = MaterialTheme.typography.bodyLarge
            )
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= MAX_GROUP_NAME) name = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Circle name") },
                singleLine = true,
                isError = error != null,
                supportingText = { Text(error ?: "Up to $MAX_GROUP_NAME characters") }
            )
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (!isValidGroupName(trimmed)) {
                        error = "Enter a name between 1 and $MAX_GROUP_NAME characters."
                    } else {
                        actions.createCircle(trimmed, onCreated)
                    }
                },
                enabled = !actions.isBusy("create"),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text(if (actions.isBusy("create")) "Creating…" else "Create circle") }
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
    var code by rememberSaveable { mutableStateOf("") }
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
        topBar = { ScreenTitle(if (groupName.isBlank()) "Join code" else "Join code for $groupName", onBack) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (full) {
                NotebookSurface(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                    Text("Your circle is full ($MAX_MEMBERS members)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "New join codes and requests are paused. If someone leaves, a spot opens up.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else NotebookSurface(Modifier.fillMaxWidth()) {
                Text("Share this code", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$memberCount of $MAX_MEMBERS members",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (code.isEmpty()) {
                    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (actions.isBusy("code")) CircularProgressIndicator(Modifier.padding(end = 12.dp))
                        Text(
                            if (actions.isBusy("code")) "Making a code…" else "No code yet.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (!actions.isBusy("code")) {
                        OutlinedButton(
                            onClick = { actions.issueCode(false) { code = it } },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Try again") }
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
                                contentDescription = "QR of the join code. The other person can scan it from Join with a code.",
                                modifier = Modifier.padding(vertical = 8.dp).size(220.dp)
                            )
                        }
                    }
                }
                val hours = if (state.joinNonceExpiresAt > nowMs) hoursLeft(state.joinNonceExpiresAt, nowMs) else 24L
                Text(
                    "Expires in ${hours}h",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { actions.share(code) },
                    enabled = code.isNotEmpty() && !full,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("Share") }
                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        actions.notify("Code copied")
                    },
                    enabled = code.isNotEmpty() && !full,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("Copy") }
            }
            OutlinedButton(
                onClick = { actions.issueCode(true) { code = it; actions.notify("New code ready. The old one no longer works.") } },
                enabled = !actions.isBusy("code") && !full,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("New code") }
            NotebookSurface(Modifier.fillMaxWidth()) {
                Text("Keep Penny Circle open", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Keep Penny Circle open (or turn on Stay reachable in Settings) so requests reach you. Requests wait up to 24 hours, and you cannot approve one while your phone is offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                val reach = when (peerStatus.phase) {
                    LinkPhase.Online -> "Online now"
                    LinkPhase.Starting -> "Connecting…"
                    LinkPhase.NoNetwork -> "No network"
                    LinkPhase.Error -> "Not reachable right now"
                    LinkPhase.Off -> "Not listening right now"
                }
                Text(
                    "Right now: $reach. Stay reachable is ${if (stayReachable) "on" else "off"}.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp).semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Text(
                "A code on its own does not let anyone in. Each person who uses it appears in your join requests, and you approve or decline them. Making a new code stops older codes from working; requests already waiting stay waiting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---- Join with a code ------------------------------------------------------------------------------------------

private fun codeProblem(text: String): String? {
    if (text.isBlank()) return null
    return try {
        if (parseJoinCode(text).expired) "This code has expired. Ask the admin for a new one." else null
    } catch (e: IllegalArgumentException) {
        e.message ?: "That circle code is not valid. Ask for a new one."
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
    onScan: (((String) -> Unit) -> Unit)? = null
) {
    var code by rememberSaveable { mutableStateOf("") }
    var touched by rememberSaveable { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val problem = codeProblem(code)

    LaunchedEffect(inCircle, joinProgress is JoinProgress.Joined) {
        if (inCircle || joinProgress is JoinProgress.Joined) onJoined()
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { ScreenTitle("Join with a code", onBack) },
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
                            Text("Sending your request…", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "This can take a moment if the admin's phone is asleep or offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                is JoinProgress.Waiting -> {
                    NotebookSurface(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                        Text("Request sent. Waiting for the admin to approve", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "The admin has to open Penny Circle on their phone to approve. Requests wait up to 24 hours. Yours expires at ${clockTime(joinProgress.expiresAt)} (about ${hoursLeft(joinProgress.expiresAt, System.currentTimeMillis())}h from now). After approval you join when both phones can reach each other; delivery is not instant.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { actions.join(code) },
                                enabled = code.isNotBlank() && !actions.isBusy("join"),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) { Text("Check again") }
                            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
                        }
                    }
                }

                else -> {
                    if (joinProgress is JoinProgress.Refused) {
                        NotebookSurface(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
                            Text("The circle could not take this request", style = MaterialTheme.typography.titleMedium)
                            Text(
                                joinProgress.reason.ifBlank { "No reason was given." } + " You can try another code.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                    Text("Paste the circle code", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Ask the circle's admin to share a code. The admin approves your request before you see anything.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it; touched = false },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("Circle code") },
                        isError = problem != null && (touched || code.length > 12),
                        supportingText = {
                            Text(if (problem != null && (touched || code.length > 12)) problem else "Starts with pc1.")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                    OutlinedButton(
                        onClick = {
                            val pasted = clipboard.getText()?.text.orEmpty()
                            if (pasted.isBlank()) actions.notify("The clipboard is empty.") else code = pasted
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Paste from clipboard") }
                    if (onScan != null) {
                        OutlinedButton(
                            onClick = { onScan { scanned -> code = scanned.trim(); touched = true } },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Scan a QR code") }
                    }
                    Button(
                        onClick = {
                            touched = true
                            if (code.isBlank()) return@Button
                            if (codeProblem(code) == null) actions.join(code)
                        },
                        enabled = !actions.isBusy("join"),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) { Text("Send request") }
                }
            }
        }
    }
}
