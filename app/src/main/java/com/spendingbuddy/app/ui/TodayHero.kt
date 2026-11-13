package com.spendingbuddy.app.ui

import androidx.compose.ui.platform.LocalContext
import com.spendingbuddy.app.R

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendingbuddy.app.BuddyState
import com.spendingbuddy.app.Expense
import com.spendingbuddy.app.PendingDetection
import com.spendingbuddy.app.paymentReviewReason
import com.spendingbuddy.app.dayTotals
import com.spendingbuddy.app.rupees
import com.spendingbuddy.app.weekStart
import java.time.LocalDate
import java.time.format.TextStyle as DayNameStyle
import java.util.Locale

/** One day of the current Monday-Sunday week, as shown in the ring row. */
internal class WeekDay(val name: String, val spent: Long, val finished: Boolean, val future: Boolean, val today: Boolean)

internal fun weekDays(state: BuddyState, today: LocalDate): List<WeekDay> {
    val start = weekStart(today)
    val totals = dayTotals(state.expenses, today)
    return (0..6).map { i ->
        val day = start.plusDays(i.toLong())
        WeekDay(
            name = day.dayOfWeek.getDisplayName(DayNameStyle.FULL, Locale.ENGLISH),
            spent = totals[i],
            finished = state.checkIns.any { it.date == day.toString() },
            future = day.isAfter(today),
            today = day == today
        )
    }
}

/**
 * The top of Today: what was spent, how much of the limit is left, and the week so far. The numbers are large and
 * heavy on purpose, and the "left" or "over" line always has words and an icon, never colour alone.
 */
@Composable
internal fun TodayHero(spent: Long, limit: Long, days: List<WeekDay>, streak: Int, compact: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    val over = spent > limit
    val label = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, letterSpacing = 1.4.sp)
    val percent = if (limit <= 0) 100 else ((spent.coerceAtLeast(0) * 100) / limit).toInt()
    Column(modifier.glass(RoundedCornerShape(24.dp)).padding(horizontal = 20.dp, vertical = if (compact) 16.dp else 20.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stackLabels = LocalDensity.current.fontScale >= 1.3f || maxWidth < 300.dp
            if (stackLabels) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(context.getString(R.string.today_hero_spent_today), style = label, color = t.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading(); contentDescription = context.getString(R.string.today_hero_spent_today_baddc) })
                    Text(context.getString(R.string.today_hero_percent_of_limit, percent), style = label, color = if (over) t.danger else t.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.End).clearAndSetSemantics { })
                }
            } else Row(verticalAlignment = Alignment.CenterVertically) {
                Text(context.getString(R.string.today_hero_spent_today), style = label, color = t.muted, modifier = Modifier.weight(1f).semantics { heading(); contentDescription = context.getString(R.string.today_hero_spent_today_baddc) })
                Text(context.getString(R.string.today_hero_percent_of_limit, percent), style = label, color = if (over) t.danger else t.muted, modifier = Modifier.clearAndSetSemantics { })
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            // The amount never grows past the card, whatever the font scale.
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, LocalDensity.current.fontScale.coerceAtMost(1.3f))) {
                val size = heroFontSizeSp(
                    availableWidthDp = maxWidth.value,
                    digitCount = (spent / 100).toString().length,
                    hasPaise = spent % 100L != 0L,
                    fontScale = LocalDensity.current.fontScale,
                    maxSp = if (compact) 60f else 76f,
                    minSp = 34f
                ).sp
                Row(Modifier.bumpOnChange(spent).clearAndSetSemantics { contentDescription = context.getString(R.string.today_hero_amount_spent_today, rupees(spent)) }) {
                    Text("₹", fontSize = size * 0.5f, color = t.muted, fontFamily = PennyType.Sans, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = (size.value * 0.14f).dp, end = 2.dp))
                    RollingAmount(
                        rupees = spent / 100,
                        style = TextStyle(fontFamily = PennyType.Sans, fontWeight = FontWeight.Bold, fontSize = size, lineHeight = size, letterSpacing = (-0.045f * size.value).sp),
                        color = if (over) t.danger else t.text
                    )
                    if (spent % 100L != 0L) Text(
                        String.format(Locale.ROOT, ".%02d", spent % 100),
                        style = TextStyle(fontFamily = PennyType.Mono, fontWeight = FontWeight.Medium, fontSize = size * 0.3f),
                        color = t.muted,
                        modifier = Modifier.padding(top = (size.value * 0.2f).dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))
        LimitBar(progress = budgetIntensity(spent, limit), over = over)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Three states, each with its own words and icon: comfortable, close to the limit, and over it.
            val near = !over && percent >= 80
            val tone = if (over) t.danger else if (near) t.text else if (t.dark) lerp(t.accent, t.text, 0.4f) else t.accent
            Icon(if (over || near) Icons.Rounded.ErrorOutline else Icons.Rounded.Check, null, tint = tone, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (over) context.getString(R.string.today_hero_amount_over_limit, rupees(spent - limit)) else if (near) context.getString(R.string.today_hero_amount_only_left, rupees(limit - spent)) else context.getString(R.string.today_hero_amount_left, rupees(limit - spent)),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = tone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(context.getString(R.string.today_hero_of_limit, rupees(limit)), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PennyType.Mono), color = t.muted, maxLines = 1)
        }
        HorizontalDivider(Modifier.padding(vertical = if (compact) 12.dp else 16.dp), thickness = 1.dp, color = t.text.copy(alpha = 0.08f))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stackLabels = LocalDensity.current.fontScale >= 1.3f || maxWidth < 300.dp
            if (stackLabels) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(context.getString(R.string.today_hero_this_week), style = label, color = t.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading(); contentDescription = context.getString(R.string.today_hero_this_week_8c4ee) })
                    Text(
                        if (streak > 0) context.getString(R.string.today_hero_streak_display, streak) else context.getString(R.string.today_hero_no_streak_yet),
                        style = label,
                        color = if (streak > 0) t.text else t.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.End).semantics { contentDescription = if (streak > 0) context.getString(R.string.today_hero_streak_description, streak) else context.getString(R.string.today_hero_no_streak_yet_9a45d) }
                    )
                }
            } else Row(verticalAlignment = Alignment.CenterVertically) {
                Text(context.getString(R.string.today_hero_this_week), style = label, color = t.muted, modifier = Modifier.weight(1f).semantics { heading(); contentDescription = context.getString(R.string.today_hero_this_week_8c4ee) })
                Text(
                    if (streak > 0) context.getString(R.string.today_hero_streak_display, streak) else context.getString(R.string.today_hero_no_streak_yet),
                    style = label,
                    color = if (streak > 0) t.text else t.muted,
                    modifier = Modifier.semantics { contentDescription = if (streak > 0) context.getString(R.string.today_hero_streak_description, streak) else context.getString(R.string.today_hero_no_streak_yet_9a45d) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        WeekRings(days, limit)
    }
}

/** Thick budget bar. It fills from empty when the screen opens and turns to the danger colour once over. */
@Composable
private fun LimitBar(progress: Float, over: Boolean, height: Dp = 12.dp) {
    val t = LocalPennyTokens.current
    val reduced = LocalReducedMotion.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    // Read only while drawing, so the animation never recomposes the hero.
    val level = animateFloatAsState(if (shown || reduced) progress.coerceIn(0f, 1f) else 0f, if (reduced) snap() else PennyMotion.spatialSlow(), label = "limitBar")
    val from = if (over) t.danger else t.accent
    val to = from
    val track = t.text.copy(alpha = if (t.dark) 0.10f else 0.08f)
    Box(
        Modifier.fillMaxWidth().height(height).clearAndSetSemantics { }.drawBehind {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(track, cornerRadius = radius)
            val w = size.width * level.value
            if (w > 0.5f) drawRoundRect(Brush.horizontalGradient(listOf(from, to), endX = size.width), size = Size(w.coerceAtLeast(size.height), size.height), cornerRadius = radius)
        }
    )
}

/** Seven small rings, Monday to Sunday. Each fills with that day's share of the daily limit; today's is larger. */
@Composable
private fun WeekRings(days: List<WeekDay>, limit: Long) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    val reduced = LocalReducedMotion.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        days.forEach { day ->
            val over = day.spent > limit
            val target = if (day.future) 0f else budgetIntensity(day.spent, limit)
            val level = animateFloatAsState(if (shown || reduced) target else 0f, if (reduced) snap() else PennyMotion.spatialSlow(), label = "ring")
            val ring = if (day.today) 40.dp else 30.dp
            val stroke = if (day.today) 5.dp else 4.dp
            val color = if (over) t.danger else t.accent
            val track = t.text.copy(alpha = if (day.future) 0.06f else if (t.dark) 0.14f else 0.10f)
            val description = context.getString(when {
                day.future -> R.string.today_hero_day_future
                day.today && over && day.finished -> R.string.today_hero_day_today_amount_over_finished
                day.today && over -> R.string.today_hero_day_today_amount_over
                day.today && day.finished -> R.string.today_hero_day_today_amount_finished
                day.today -> R.string.today_hero_day_today_amount
                over && day.finished -> R.string.today_hero_day_amount_over_finished
                over -> R.string.today_hero_day_amount_over
                day.finished -> R.string.today_hero_day_amount_finished
                else -> R.string.today_hero_day_amount
            }, day.name, rupees(day.spent))
            Column(Modifier.clearAndSetSemantics { contentDescription = description }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(ring).drawBehind {
                        val s = stroke.toPx()
                        val inset = Offset(s / 2f, s / 2f)
                        val arc = Size(size.width - s, size.height - s)
                        drawArc(track, 0f, 360f, false, inset, arc, style = Stroke(s))
                        val sweep = 360f * level.value
                        if (sweep > 1f) drawArc(color, -90f, sweep, false, inset, arc, style = Stroke(s, cap = StrokeCap.Round))
                    },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        over -> Text("!", color = t.danger, fontWeight = FontWeight.Bold, fontSize = if (day.today) 16.sp else 13.sp, fontFamily = PennyType.Sans)
                        day.finished -> Icon(Icons.Rounded.Check, null, tint = if (t.dark) t.text else t.accent, modifier = Modifier.size(if (day.today) 18.dp else 14.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    day.name.take(1),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = PennyType.Mono, fontWeight = if (day.today) FontWeight.Bold else FontWeight.Medium),
                    color = if (day.today) t.text else t.muted
                )
            }
        }
    }
}

/** Flat card used for the automatic-tracking notices on Today. */
@Composable
private fun NoticeCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val t = LocalPennyTokens.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().background(t.accent.copy(alpha = if (t.dark) 0.10f else 0.06f), shape).border(1.dp, t.accent.copy(alpha = 0.30f), shape).padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp)
    ) { content() }
}

@Composable
private fun NoticeTitle(text: String) {
    val t = LocalPennyTokens.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = if (t.dark) lerp(t.accent, t.text, 0.4f) else t.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = t.text)
    }
}

/** Shown until the user turns automatic tracking on or says not now. */
@Composable
internal fun AutoPromptCard(onTurnOn: () -> Unit, onNotNow: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    NoticeCard(modifier) {
        NoticeTitle(context.getString(R.string.today_hero_track_purchases_automatically))
        Text(
            context.getString(R.string.today_hero_penny_circle_can_read_payment_alerts),
            style = MaterialTheme.typography.bodyMedium, color = t.muted, modifier = Modifier.padding(top = 6.dp, end = 8.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onNotNow, modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.today_hero_not_now)) }
            TextButton(onClick = onTurnOn, modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.today_hero_set_up_automatic_tracking)) }
        }
    }
}

/** A payment the app was not sure about. Nothing is saved until the user chooses. */
@Composable
internal fun PendingDetectionCard(pending: PendingDetection, onAdd: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    NoticeCard(modifier) {
        NoticeTitle(context.getString(R.string.today_hero_add_this_payment))
        Text(
            context.getString(R.string.today_hero_amount_merchant, rupees(pending.amountPaise), pending.merchant.ifBlank { context.getString(R.string.today_hero_no_shop_name) }),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = t.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, end = 8.dp)
        )
        Text(context.getString(R.string.today_hero_reason_sentence, context.paymentReviewReason(pending.why)), style = MaterialTheme.typography.bodyMedium, color = t.muted, modifier = Modifier.padding(end = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.today_hero_skip)) }
            TextButton(onClick = onAdd, modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.today_hero_add), fontWeight = FontWeight.Bold) }
        }
    }
}

/** A purchase that was just added automatically and can still be undone before the circle sees it. */
@Composable
internal fun HeldPurchaseCard(expense: Expense, inCircle: Boolean, minutesLeft: Long, onUndo: () -> Unit, onKeep: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val t = LocalPennyTokens.current
    NoticeCard(modifier) {
        NoticeTitle(context.getString(R.string.today_hero_added_automatically))
        Text(
            context.getString(R.string.today_hero_amount_merchant, rupees(expense.amountPaise), expense.merchant.ifBlank { expense.category }),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = t.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, end = 8.dp)
        )
        val minutes = minutesLeft.coerceAtLeast(1)
        Text(
            if (expense.needsCategory) context.getString(if (inCircle) R.string.today_hero_hold_circle_needs_category else R.string.today_hero_hold_solo_needs_category, minutes)
                else context.getString(if (inCircle) R.string.today_hero_hold_circle_category else R.string.today_hero_hold_solo_category, expense.category, minutes),
            style = MaterialTheme.typography.bodyMedium, color = t.muted, modifier = Modifier.padding(end = 8.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onUndo, modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.today_hero_undo)) }
            TextButton(onClick = onKeep, modifier = Modifier.heightIn(min = 48.dp)) { Text(context.getString(R.string.today_hero_keep), fontWeight = FontWeight.Bold) }
        }
    }
}
