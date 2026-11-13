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
const val STATE_SCHEMA_VERSION = 3
val PAYMENT_UPDATE_MINUTES = listOf(1, 2, 5)
const val DEFAULT_PAYMENT_UPDATE_MINUTES = 2
val FINISH_COUNTDOWN_LEAD_MINUTES = listOf(15, 30, 60)
const val DEFAULT_FINISH_COUNTDOWN_LEAD_MINUTES = 30

fun paymentUpdateDurationMillis(minutes: Int): Long {
    require(minutes in PAYMENT_UPDATE_MINUTES)
    return minutes * 60_000L
}

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
    val updatedAt: Long = System.currentTimeMillis(),
    /** "" = typed in by hand; [SOURCE_AUTO] = added from a payment message read on this phone. Never sent to the circle. */
    val source: String = "",
    /** Epoch ms until which an auto-added purchase is kept from the circle so it can be undone first. 0 = shareable. */
    val heldUntil: Long = 0,
    /** The category of an auto-added purchase could not be guessed and the user has not chosen one yet. */
    val needsCategory: Boolean = false
)

const val SOURCE_AUTO = "auto"
/** How long an auto-added purchase stays private to this phone, so it can be undone before friends see it. */
const val AUTO_HOLD_MS = 10 * 60_000L

/** Personal rule: purchases at [merchant] get this category and reason. Never synced to the circle. */
@Serializable
data class MerchantRule(
    /** Normalised merchant name, see merchantKey(). */
    val key: String = "",
    /** The name as shown to the user. */
    val merchant: String = "",
    val category: String = "Other",
    val reason: String = "Essential"
)

/** A detected payment the app was not sure about and is asking the user to add or skip. Kept on this phone only. */
@Serializable
data class PendingDetection(
    val id: String = UUID.randomUUID().toString(),
    val amountPaise: Long = 0,
    val merchant: String = "",
    val date: String = todayIndia().toString(),
    val category: String = "Other",
    val reason: String = "Essential",
    val needsCategory: Boolean = false,
    /** Short plain reason why it was not added automatically, shown to the user. */
    val why: String = "",
    val detectedAt: Long = System.currentTimeMillis()
)

/** A payment already handled, kept briefly so the same payment seen twice (app notification + bank SMS) is counted once. */
@Serializable
data class DetectionMark(val amountPaise: Long = 0, val at: Long = 0, val ref: String = "")

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

const val MAX_MEMBERS = 12
const val MAX_DISPLAY_NAME = 40
const val MEMBER_ACTIVE = "active"
const val MEMBER_LEFT = "left"

/** A Penny Circle: a small group of friends. [secret] is the group's own random secret; it is shared only with admitted members and is never placed in an invite code. */
@Serializable
data class Group(
    val id: String = "", val name: String = "", val secret: String = "", val createdAt: Long = 0,
    /** The admin only approves join requests; they cannot remove anyone. */
    val adminId: String = "",
    /** Monotonically increasing; a claim about the admin with an epoch not above ours is stale and ignored. */
    val adminEpoch: Long = 0
)

/** What a member shares with the circle. TOTALS_STATUS (default): totals, limits, check-in status and streak, no purchase records. FULL_DETAILS: reviewed records too. */
@Serializable
enum class Visibility { TOTALS_STATUS, FULL_DETAILS }

/** A join request waiting for the admin's decision. Holds no data from the requester beyond a name and endpoint info. */
@Serializable
data class JoinRequest(
    val id: String = "",
    val endpointId: String = "",
    val displayName: String = "",
    val relayUrl: String? = null,
    val directAddresses: List<String> = emptyList(),
    val requestedAt: Long = 0,
    val expiresAt: Long = 0
)

/**
 * A group member, identified by their iroh endpoint id (an ed25519 public key). status is "active" or "left"
 * (a tombstone: a left member never comes back under the same endpoint id).
 */
@Serializable
data class Member(
    val endpointId: String = "",
    val displayName: String = "",
    /** Last known home relay URL and direct addresses; dialing by id alone also works via discovery. */
    val relayUrl: String? = null,
    val directAddresses: List<String> = emptyList(),
    val joinedAt: Long = 0,
    val status: String = MEMBER_ACTIVE,
    val leftAt: Long = 0
) { val active: Boolean get() = status == MEMBER_ACTIVE }

/** One member's own data, as last received from them. Read-only on this phone; each member owns their own limits. */
@Serializable
data class FriendSnapshot(
    val name: String = "",
    val expenses: List<Expense> = emptyList(),
    val checkIns: List<CheckIn> = emptyList(),
    val dailyLimitPaise: Long = 0,
    val weeklyLimitPaise: Long = 0,
    /** The member's own monotonically increasing data revision. */
    val revision: Long = 0,
    /** When this phone last received an exchange from the member (epoch ms). */
    val receivedAt: Long = 0,
    /** Totals and status the member shared (always present, even when [expenses] is empty under TOTALS_STATUS). */
    val dailySpentPaise: Long = 0,
    val weeklySpentPaise: Long = 0,
    val streak: Int = 0,
    val visibility: Visibility = Visibility.TOTALS_STATUS,
    /** Impulse (frivolous) spending this week, and days in a row without one. -1 = the member's app is older and does not share it. */
    val impulseWeekPaise: Long = 0,
    val impulseWeekCount: Int = 0,
    val impulseFreeDays: Int = -1
)

/**
 * A one-off message queued in the outbox until [target] acknowledges it.
 * type: "nudge" | "budget_suggestion" | "suggestion_reply" | "leave". [target] is the addressee's endpoint id.
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
    val accepted: Boolean = false,
    val target: String = ""
)

/** A nudge received from a member. The countdown starts when it reaches this phone. */
@Serializable
data class Nudge(
    val id: String,
    val sender: String = "",
    val minutes: Int = 30,
    val sentAt: Long = 0,
    val deliveredAt: Long = 0,
    val endsAt: Long = 0,
    val senderId: String = ""
)

/** A nudge this phone sent to [targetId]. deliveredAt == 0 means it is still waiting for their phone. */
@Serializable
data class OutgoingNudge(
    val id: String,
    val minutes: Int,
    val sentAt: Long,
    val deliveredAt: Long = 0,
    val targetId: String = ""
) { val endsAt: Long get() = if (deliveredAt == 0L) 0L else deliveredAt + minutes * 60_000L }

/**
 * status: "pending" | "accepted" | "declined". A suggestion is addressed to exactly one member ([targetId]);
 * only that member may accept or decline it, and the reply is only honoured from them.
 */
@Serializable
data class BudgetSuggestion(
    val id: String,
    val dailyPaise: Long,
    val weeklyPaise: Long,
    val sender: String = "",
    val createdAt: Long = 0,
    val status: String = "pending",
    /** For suggestions this phone sent: whether the target's phone has received it. */
    val delivered: Boolean = false,
    val senderId: String = "",
    val targetId: String = ""
)

/** A personal quick-fill for the purchase form: [name] is the merchant or short reason text. Never synced to the circle. */
@Serializable
data class Preset(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "Food",
    val reason: String = "Essential",
    /** At most one preset is the default; it pre-fills a new purchase. */
    val isDefault: Boolean = false
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
    /** Opt-in to a local 9:30–10 PM reminder timer. Never included in circle snapshots. */
    val finishCountdown: Boolean = false,
    /** Local payment budget-card display duration; it does not change the 10-minute Undo hold. */
    val paymentUpdateMinutes: Int = DEFAULT_PAYMENT_UPDATE_MINUTES,
    /** Local lead time before the fixed 10 PM IST reminder. Never synced. */
    val finishCountdownLeadMinutes: Int = DEFAULT_FINISH_COUNTDOWN_LEAD_MINUTES,
    val importImages: List<ImportImage> = emptyList(),
    val importCandidates: List<ImportCandidate> = emptyList(),
    val onboardingDone: Boolean = false,
    /** "keypad" | "dial" — the rotary dial is an optional, playful input mode. */
    val amountInput: String = "keypad",
    /** App look: "light_horizon" | "violet_night" | "mint_night" | "signal_blue". Older states load as the light default. */
    val theme: String = "light_horizon",
    val lastCategory: String = "Food",
    /** Personal presets (this phone only: not part of circle snapshots; included in the user's own backup). Older states load as empty. */
    val presets: List<Preset> = emptyList(),
    /** Read payment notifications and bank SMS on this phone and log purchases from them. Off until the user turns it on. */
    val autoDetect: Boolean = false,
    /** The user has seen (and answered) the Today card that offers automatic tracking. */
    val autoDetectOffered: Boolean = false,
    /** Personal merchant -> category rules for auto-added purchases (this phone only; included in the user's own backup). */
    val merchantRules: List<MerchantRule> = emptyList(),
    /** Detected payments waiting for an Add or Skip (this phone only, never synced or backed up). */
    val pendingDetections: List<PendingDetection> = emptyList(),
    /** Recently handled payments, for de-duplication (bounded, this phone only). */
    val recentDetections: List<DetectionMark> = emptyList(),
    /** Check-ins that an auto-added purchase re-opened, keyed by that purchase's id, so Undo can put them back. */
    val heldCheckIns: Map<String, CheckIn> = emptyMap(),
    val friendAlerts: Boolean = true,
    val stayReachable: Boolean = false,
    // shared-data revision: increments whenever data the group can see changes
    val revision: Long = 0,
    // group (no hosted backend). selfId is this phone's iroh endpoint id; "" until [ensureSelf] runs.
    val selfId: String = "",
    val group: Group? = null,
    /** All members including this phone and tombstoned (left) ones. */
    val members: List<Member> = emptyList(),
    /** Each other member's last received data, keyed by endpoint id. */
    val friends: Map<String, FriendSnapshot> = emptyMap(),
    val outbox: List<PeerEvent> = emptyList(),
    /** Event ids received per sender endpoint id (bounded), used to de-duplicate re-sent events and as acks. */
    val seenEventIds: Map<String, List<String>> = emptyMap(),
    /** The latest nudge received from any member. */
    val incomingNudge: Nudge? = null,
    val outgoingNudges: List<OutgoingNudge> = emptyList(),
    /** Suggestions other members sent to this phone. */
    val suggestions: List<BudgetSuggestion> = emptyList(),
    /** Suggestions this phone sent. */
    val sentSuggestions: List<BudgetSuggestion> = emptyList(),
    val lastNudgedDate: String = "",
    /** What this phone shares with the circle from now on. Switching only affects data sent afterwards. */
    val visibility: Visibility = Visibility.TOTALS_STATUS,
    /** Admin only: join requests awaiting approval (capped), endpoint ids declined, and the current code nonce. */
    val pendingJoins: List<JoinRequest> = emptyList(),
    val declinedJoinIds: List<String> = emptyList(),
    val joinNonce: String = "",
    val joinNonceExpiresAt: Long = 0,
    /** Sender endpoint id -> Asia/Kolkata date of the last nudge accepted from them (one per day). */
    val lastNudgeFrom: Map<String, String> = emptyMap(),
    /** Broadcast sequencing: our last used sequence number, and the highest seen from each member. */
    val outSeq: Long = 0,
    val lastSeqSeen: Map<String, Long> = emptyMap()
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

/** An impulse (frivolous) purchase: the user marked its reason as Impulse. */
fun isImpulse(e: Expense): Boolean = e.kind == "expense" && e.reason == IMPULSE

data class ImpulseStats(val weekPaise: Long, val weekCount: Int, val freeDays: Int)

/**
 * Impulse spending in the Monday-Sunday week of [today], and how many days in a row up to today had no impulse
 * purchase. The run is counted from the first day anything was logged, so a new user does not start with a long one.
 */
fun impulseStats(expenses: List<Expense>, today: LocalDate): ImpulseStats {
    val start = weekStart(today); val end = start.plusDays(7)
    val dated = expenses.mapNotNull { e -> runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { it to e } }.filter { !it.first.isAfter(today) }
    val week = dated.filter { isImpulse(it.second) && !it.first.isBefore(start) && it.first.isBefore(end) }
    val lastImpulse = dated.filter { isImpulse(it.second) }.maxOfOrNull { it.first }
    val firstLogged = dated.minOfOrNull { it.first }
    val free = when {
        firstLogged == null -> 0
        lastImpulse == null -> java.time.temporal.ChronoUnit.DAYS.between(firstLogged, today).toInt() + 1
        else -> java.time.temporal.ChronoUnit.DAYS.between(lastImpulse, today).toInt()
    }
    return ImpulseStats(week.sumOf { it.second.amountPaise }, week.size, free.coerceIn(0, 9_999))
}

fun spentOn(expenses: List<Expense>, date: LocalDate): Long = netSpending(expenses.filter { it.date == date.toString() })
fun spentInWeek(expenses: List<Expense>, anyDay: LocalDate): Long {
    val start = weekStart(anyDay); val end = start.plusDays(7)
    return netSpending(expenses.filter { e -> runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { !it.isBefore(start) && it.isBefore(end) } ?: false })
}
