package com.spendingbuddy.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.spendingbuddy.app.INDIA_ZONE
import com.spendingbuddy.app.Member
import com.spendingbuddy.app.Visibility
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

/**
 * Everything the circle screens can ask for. Implemented in AppNav on top of PeerSyncManager.
 * Each call guards against double taps (see [isBusy]), and shows a calm snackbar if it fails.
 */
interface CircleActions {
    /** True while the action with this key is running. Keys: create, code, join, approve:ID, decline:ID, leave, transfer, visibility, nudge:ID, suggest, respond:ID. */
    fun isBusy(key: String): Boolean

    fun createCircle(name: String, onDone: () -> Unit)
    fun issueCode(rotate: Boolean, onCode: (String) -> Unit)
    fun join(code: String)
    fun approve(requestId: String)
    fun decline(requestId: String)
    fun leave(successorId: String?, onDone: () -> Unit)
    fun transferAdmin(memberId: String, onDone: () -> Unit)
    fun setVisibility(visibility: Visibility)
    fun nudge(memberId: String, name: String)
    fun suggest(targetId: String, dailyPaise: Long, weeklyPaise: Long, name: String, onDone: () -> Unit)
    fun respond(suggestionId: String, accepted: Boolean)
    fun share(code: String)

    /** Non-blocking success feedback. */
    fun notify(message: String)
}

private val clockFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

internal fun clockTime(ms: Long): String = Instant.ofEpochMilli(ms).atZone(INDIA_ZONE).format(clockFormatter)

internal fun agoText(ms: Long, nowMs: Long): String {
    val minutes = ((nowMs - ms) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1L -> "just now"
        minutes < 60L -> "$minutes min ago"
        minutes < 24L * 60L -> "${minutes / 60L} h ago"
        else -> "${minutes / (24L * 60L)} d ago"
    }
}

internal fun hoursLeft(expiresAt: Long, nowMs: Long): Long =
    ceil((expiresAt - nowMs) / 3_600_000.0).toLong().coerceAtLeast(0L)

internal fun Member.label(): String = displayName.ifBlank { "Friend" }

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** A radio row with a 48dp minimum touch target; the whole row is one selectable element. */
@Composable
internal fun ChoiceRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
