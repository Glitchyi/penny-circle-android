package com.spendingbuddy.app.ui

import com.spendingbuddy.app.R

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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
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
    fun failure(key: String): String? = null
    fun clearFailure(key: String) {}

    fun createCircle(name: String, onDone: () -> Unit)
    fun issueCode(rotate: Boolean, onCode: (String) -> Unit)
    fun join(code: String)
    fun cancelJoin(onDone: () -> Unit)
    fun checkJoin()
    fun retryMember(memberId: String)
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

internal fun agoText(context: android.content.Context, ms: Long, nowMs: Long): String {
    val minutes = ((nowMs - ms) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1L -> context.getString(R.string.circle_copy_just_now)
        minutes < 60L -> context.getString(R.string.circle_copy_min_ago, minutes)
        minutes < 24L * 60L -> context.getString(R.string.circle_copy_h_ago, minutes / 60L)
        else -> context.getString(R.string.circle_copy_d_ago, minutes / (24L * 60L))
    }
}

internal fun hoursLeft(expiresAt: Long, nowMs: Long): Long =
    ceil((expiresAt - nowMs) / 3_600_000.0).toLong().coerceAtLeast(0L)

internal fun Member.label(context: android.content.Context): String = displayName.ifBlank { context.getString(R.string.circle_copy_friend) }

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
    supporting: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
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

/** The running command or its retained failure belongs beside the active control, including in sheets. */
@Composable
internal fun CircleActionFeedback(actions: CircleActions, key: String, progress: String) {
    val failure = actions.failure(key)
    val message = if (actions.isBusy(key)) progress else failure ?: return
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (failure != null && !actions.isBusy(key)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            if (failure != null && !actions.isBusy(key)) error(failure)
        }
    )
}
