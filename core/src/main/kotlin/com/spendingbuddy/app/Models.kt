package com.spendingbuddy.app

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

val INDIA_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")
val DEFAULT_CATEGORIES = listOf("Food", "Transport", "Shopping", "Bills", "Other")
val DEFAULT_REASONS = listOf("Essential", "Convenience", "Social", "Impulse")
const val IMPULSE = "Impulse"
const val STATE_SCHEMA_VERSION = 2

fun todayIndia(): LocalDate = LocalDate.now(INDIA_ZONE)
fun rupees(paise: Long): String = (if (paise < 0) "-₹" else "₹") + java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")).apply {
    minimumFractionDigits = if (paise % 100L == 0L) 0 else 2
    maximumFractionDigits = 2
}.format(BigDecimal.valueOf(kotlin.math.abs(paise), 2))

/** Postel's law: accept "250", "₹250", "1,250.5", " 250 " — reject negatives and sub-paise precision. */
fun amountToPaise(text: String): Long? = try {
    val clean = text.trim().removePrefix("₹").replace(",", "").trim()
    val n = BigDecimal(clean)
    if (n.signum() < 0 || n.scale() > 2 || n > BigDecimal("99999999.99")) null
    else n.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact()
} catch (_: Exception) { null }

@Serializable
data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val date: String = todayIndia().toString(),
    val amountPaise: Long = 0,
    val merchant: String = "",
    val category: String = "Food",
    val reason: String = "Essential",
    val note: String = "",
    /** "expense" | "refund" | "transfer" */
    val kind: String = "expense",
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class CheckIn(
    val date: String,
    val completedAt: Long,
    val grace: Boolean = false,
    val onTime: Boolean = true,
    val noSpend: Boolean = false
)

@Serializable
data class ImportCandidate(
    val id: String = UUID.randomUUID().toString(), val imageId: String = "",
    val amountText: String = "", val merchant: String = "", val date: String = "",
    val category: String = "Food", val reason: String = "Essential",
    val selected: Boolean = true, val possibleDuplicate: Boolean = false,
    val warning: String = "Review extracted details", val rawText: String = ""
)

@Serializable
data class ImportImage(val id: String = UUID.randomUUID().toString(), val path: String = "", val status: String = "pending", val error: String = "")

/** The single paired friend, identified by their iroh endpoint id (an ed25519 public key). */
@Serializable
data class PeerLink(
    val endpointId: String,
    val name: String = "",
    val pairedAt: Long = 0,
    /** Last known home relay URL and direct addresses; dialing by id alone also works via discovery. */
    val relayUrl: String? = null,
    val directAddresses: List<String> = emptyList()
)

/** The friend's own data, as last received over the peer link. Read-only on this phone. */
@Serializable
data class FriendSnapshot(
    val name: String = "",
    val expenses: List<Expense> = emptyList(),
    val checkIns: List<CheckIn> = emptyList(),
    val dailyLimitPaise: Long = 0,
    val weeklyLimitPaise: Long = 0,
    /** Friend's own monotonically increasing data revision. */
    val revision: Long = 0,
    /** When this phone last received an exchange from the friend (epoch ms). */
    val receivedAt: Long = 0
)

/**
 * A one-off message queued in the outbox until the friend acknowledges it.
 * type: "nudge" | "budget_suggestion" | "suggestion_reply" | "unlink"
 */
@Serializable
data class PeerEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val createdAt: Long = System.currentTimeMillis(),
    val minutes: Int = 0,
    val dailyPaise: Long = 0,
    val weeklyPaise: Long = 0,
    val suggestionId: String = "",
    val accepted: Boolean = false
)

/** A nudge received from the friend. The countdown starts when it reaches this phone. */
@Serializable
data class Nudge(
    val id: String,
    val sender: String = "",
    val minutes: Int = 30,
    val sentAt: Long = 0,
    val deliveredAt: Long = 0,
    val endsAt: Long = 0
)

/** A nudge this phone sent. deliveredAt == 0 means it is still waiting for the friend's phone. */
@Serializable
data class OutgoingNudge(
    val id: String,
    val minutes: Int,
    val sentAt: Long,
    val deliveredAt: Long = 0
) { val endsAt: Long get() = if (deliveredAt == 0L) 0L else deliveredAt + minutes * 60_000L }

/** status: "pending" | "accepted" | "declined" */
@Serializable
data class BudgetSuggestion(
    val id: String,
    val dailyPaise: Long,
    val weeklyPaise: Long,
    val sender: String = "",
    val createdAt: Long = 0,
    val status: String = "pending",
    /** For suggestions this phone sent: whether the friend's phone has received it. */
    val delivered: Boolean = false
)

@Serializable
data class BuddyState(
    val schemaVersion: Int = STATE_SCHEMA_VERSION,
    val name: String = "",
    val expenses: List<Expense> = emptyList(),
    val checkIns: List<CheckIn> = emptyList(),
    val dailyLimitPaise: Long = 50000,
    val weeklyLimitPaise: Long = 300000,
    val categories: List<String> = DEFAULT_CATEGORIES,
    val reasons: List<String> = DEFAULT_REASONS,
    val haptics: Boolean = true,
    val reminders: Boolean = true,
    val importImages: List<ImportImage> = emptyList(),
    val importCandidates: List<ImportCandidate> = emptyList(),
    // v0.2 preferences
    val onboardingDone: Boolean = false,
    /** "keypad" | "dial" — the rotary dial is an optional, playful input mode. */
    val amountInput: String = "keypad",
    val lastCategory: String = "Food",
    val friendAlerts: Boolean = true,
    val stayReachable: Boolean = false,
    // shared-data revision: increments whenever data the friend can see changes
    val revision: Long = 0,
    // peer-to-peer link (no hosted backend)
    val peer: PeerLink? = null,
    val friend: FriendSnapshot? = null,
    val outbox: List<PeerEvent> = emptyList(),
    /** Event ids received from the friend (bounded), used to de-duplicate re-sent events. */
    val seenEventIds: List<String> = emptyList(),
    val incomingNudge: Nudge? = null,
    val outgoingNudge: OutgoingNudge? = null,
    /** Suggestions the friend sent to this phone. */
    val suggestions: List<BudgetSuggestion> = emptyList(),
    /** The latest suggestion this phone sent to the friend. */
    val sentSuggestion: BudgetSuggestion? = null,
    val lastNudgedDate: String = ""
)

fun netSpending(expenses: List<Expense>): Long = expenses.sumOf { if (it.kind == "refund") -it.amountPaise else if (it.kind == "transfer") 0L else it.amountPaise }
fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())
fun eligibleCheckIn(date: LocalDate, now: ZonedDateTime = ZonedDateTime.now(INDIA_ZONE)): Boolean = !now.isAfter(date.plusDays(1).atTime(10, 0).atZone(INDIA_ZONE))
fun streak(checkIns: List<CheckIn>, now: ZonedDateTime = ZonedDateTime.now(INDIA_ZONE)): Int {
    val valid = checkIns.filter { it.onTime }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }.toSet()
    val today = now.toLocalDate()
    var cursor = when {
        today in valid -> today
        today.minusDays(1) in valid -> today.minusDays(1)
        now.toLocalTime().isBefore(java.time.LocalTime.of(10, 0)) -> today.minusDays(2)
        else -> return 0
    }
    var count = 0
    while (cursor in valid) { count++; cursor = cursor.minusDays(1) }
    return count
}

/** Day status used by Today, Activity and Buddy screens. */
enum class DayStatus { Done, Grace, Late, Open, Missed }

fun dayStatus(date: LocalDate, checkIns: List<CheckIn>, now: ZonedDateTime = ZonedDateTime.now(INDIA_ZONE)): DayStatus {
    val c = checkIns.firstOrNull { it.date == date.toString() }
    return when {
        c != null && c.grace -> DayStatus.Grace
        c != null && c.onTime -> DayStatus.Done
        c != null -> DayStatus.Late
        date >= now.toLocalDate() -> DayStatus.Open
        eligibleCheckIn(date, now) -> DayStatus.Open
        else -> DayStatus.Missed
    }
}

fun spentOn(expenses: List<Expense>, date: LocalDate): Long = netSpending(expenses.filter { it.date == date.toString() })
fun spentInWeek(expenses: List<Expense>, anyDay: LocalDate): Long {
    val start = weekStart(anyDay); val end = start.plusDays(7)
    return netSpending(expenses.filter { e -> runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { !it.isBefore(start) && it.isBefore(end) } ?: false })
}
