package com.spendingbuddy.app

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.Locale

/*
 * On-device purchase detection. A payment SMS or payment-app notification is parsed here into an amount and a
 * merchant, then either added as a purchase, queued for the user to confirm, or ignored. Nothing in this file
 * touches the network, and the message text itself is never stored: only the amount, merchant name and time.
 */

/** What kind of payment a message describes, as far as the text shows. */
enum class PaymentKind { CARD, UPI_MERCHANT, UPI_PERSON, CASH, BANK_TRANSFER, UNKNOWN }

data class ParsedPayment(
    val amountPaise: Long,
    val merchant: String,
    /** Bank reference number when the message has one; used to tell two equal payments apart. */
    val ref: String = "",
    val kind: PaymentKind = PaymentKind.UNKNOWN
)

/** Reads one message. Returns null for anything that is not a completed outgoing payment. */
object PaymentMessageParser {
    // Accept ungrouped, Western grouped, and Indian grouped figures, but never a valid prefix of a bad token.
    private val numberToken = "(?:[0-9]{1,2}(?:,[0-9]{2})*,[0-9]{3}|[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.[0-9]{1,2})?(?![0-9,]|\\.[0-9])"
    private val currencyAmount = Regex("""(?i)(?:₹|\brs\.?|\binr\.?)\s*:?\s*($numberToken)""")
    private val bareDebitAmount = Regex("""(?i)\bdebited\s+(?:by|for|with)\s+($numberToken)""")
    private val debitVerb = Regex(
        """(?i)\b(debited|spent|sent|paid|withdrawn|deducted)\b|\bdebit(?=\s+(?:INR|Rs[.:]?|₹))|""" +
            """\bhas\s+a\s+debit\s+by\s+transfer\b|\b(?:thanks|thank\s+you)\s+for\s+(?:paying|using\s+(?:your\s+)?[^.\n]{0,60}\b(?:debit|credit)\s+card)\b"""
    )
    private val creditVerb = Regex("""(?i)\b(credited|received|deposited)\b""")
    private val amountContext = Regex("""(?i)\b(bal|balance|limit|lmt|avl|avail|available|outstanding|due|debited|debit|spent|sent|paid|withdrawn|deducted|amount)\b""")
    private val balanceLabels = setOf("bal", "balance", "limit", "lmt", "avl", "avail", "available", "outstanding", "due")

    private val otp = Regex("""(?i)\b(otp|one[\s-]time\s+pass\w*|verification\s+code)\b""")
    private val otpWarning = Regex("""(?i)\b(share|disclose|ask(?:s|ed)?\s+for|reveal)\b[^.]{0,24}\b(otp|one[\s-]time\s+pass\w*)\b""")
    private val otpCredential = Regex(
        """(?i)(?:\b(?:otp|one[\s-]time\s+password|verification\s+code)\b\s*[:#-]?\s*[0-9]{4,8}\b|""" +
            """\b(?:use|enter|input|confirm|verify|provide)\b[^.]{0,30}\b(?:otp|one[\s-]time\s+password|verification\s+code)\b|""" +
            """\b(?:otp|one[\s-]time\s+password|verification\s+code)\b[^.]{0,30}\b(?:for|to\s+confirm|to\s+complete)\s+(?:this\s+)?(?:payment|transaction|purchase)\b)"""
    )
    private val notDoneYet = Regex(
        """(?i)\b(?:will|shall|may|can|would)\s+(?:be\s+)?(?:debit(?:ed)?|deduct(?:ed)?|charge(?:d)?|pay|send|spend|withdraw)\b|\bto\s+be\s+debited\b|\bis\s+due\b|\bdue\s+(on|by|date)\b|\bpayment\s+due\b|""" +
            """\b(min(?:imum)?|total)\s+(amt|amount)\s+due\b|\boverdue\b|\bhas\s+requested\b|\brequest(?:ed)?\s+(money|payment)\b|""" +
            """\bcollect\s+request\b|\bpending\b|\bawaiting\b[^.]{0,30}\b(?:confirmation|completion|settlement)\b|""" +
            """\b(?:in\s+progress|processing)\b|\bnot\s+(?:yet\s+)?(?:paid|completed|successful|processed|settled)\b|""" +
            """\b(?:payment|transaction)\b[^.]{0,30}\bpending\b|\be-?mandate\b|\bmandate\b[^.]{0,40}\b(created|registered|set\s+up)\b|\bscheduled\b"""
    )
    private val failed = Regex(
        """(?i)\bfail(?:ed|ure)\b|\bdeclined\b|\bunsuccessful\b|\bnot\s+(?:been\s+)?(?:processed|successful|completed)\b|""" +
            """\bcould\s*n[o']t\b|\bcancell?ed\b|\brevers(?:ed|al)\b|\brefund(?:ed)?\b|\bbounced\b|\binsufficient\b|""" +
            """\bno\s+(?:debit|payment|deduction|charge)\b|\bnot\s+(?:debited|paid|deducted|charged|made)\b"""
    )
    private val promo = Regex("""(?i)\b(offers?|voucher|coupon|pre-?approved|apply\s+now|t&c|tnc|congrat\w*|win|won|lucky)\b""")
    private val rewardEligibility = Regex("""(?i)\bqualif(?:y|ying)\s+for\s+(?:cash\s*back|rewards?|bonus)\b""")
    private val cash = Regex("""(?i)\bwithdrawn\b|\batm\b|\bcash\s+w(?:dl|ithdrawal)\b""")
    private val card = Regex("""(?i)\bcard\b""")
    private val bankTransfer = Regex("""(?i)\b(?:NEFT|RTGS|IMPS|net\s*banking)\b""")
    private val cardBill = Regex("""(?i)\b(?:credit\s+card|card)\s+(?:bill|statement|dues?)\b""")
    private val cardUseMerchant = Regex("""(?i)\btowards\s+(.+?)\s+in\s+[^.\n]{1,40}\s+at\s+[0-9]{4}-[0-9]{2}-[0-9]{2}\b""")

    private val nameEnd = """(?=\s+(?:on|via|using|ref|refno|upi|txn|avl|bal|balance|available|account|bank|from|for|thru|through|if|not|call|sms|dated|dt|is|has|was)\b|\s*[,;(]|\.\s|\.?\s*$|\s+-\s)"""
    private val upiPath = Regex("""(?i)\bUPI/(P2M|P2A)/[0-9]+/([^/\n]+)""")
    private val upiPathTail = Regex("""(?i)(?:\s+(?:ref(?:no)?|reference|utr|rrn|txn|upi|on|call|sms|not|bank|axis\s+bank)\b|\.\s+|\s+-\s+).*$""")
    private val atName = Regex("""(?i)\bat\s+(.+?)$nameEnd""")
    private val creditedName = Regex("""(?i);\s*(.+?)\s+credited\b""")
    private val toName = Regex("""(?i)\b(?:trf\s+to|transfer(?:red)?\s+to|paid\s+to|sent\s+to|towards|to)\s+(?:vpa\s+)?(.+?)$nameEnd""")
    private val notAName = Regex("""(?i)^(a/?c|acct?|account|your|card|bank|the|self|wallet|beneficiary|mobile|upi\s*id)\b""")
    private val reference = Regex("""(?i)\b(?:ref(?:erence)?\.?\s*(?:no\.?|number|id)?|refno|utr|rrn|txn\s*(?:id|no\.?)?|upi)\s*[:#-]?\s*([0-9]{6,})""")
    private val loneNameLineBlock = Regex("""(?i)a/?c|card|bank|avl|limit|\bbal|not you|sms|call|debited|spent|\binr\b|\brs\b|₹|upi|[0-9]{2}[-/:][0-9]{2}""")
    // A clipped identifier or bank footer is never a safe merchant to auto-name.
    private val unsafeMerchant = Regex("""(?i)\b(?:a/?c|account|beneficiary|vpa|upi|ref(?:erence)?|utr|rrn|txn|transaction|balance|bal|avl|available|limit|call|sms|helpline|customer care)\b""")

    fun parse(text: String, title: String = ""): ParsedPayment? {
        val raw = listOf(title, text).filter { it.isNotBlank() }.joinToString("\n")
        if (raw.isBlank() || raw.length > 1200) return null
        val flat = raw.replace(Regex("""\s+"""), " ").trim()

        if (otpCredential.containsMatchIn(flat)) return null
        if (otp.containsMatchIn(flat) && !otpWarning.containsMatchIn(flat)) return null
        if (notDoneYet.containsMatchIn(flat) || failed.containsMatchIn(flat) || promo.containsMatchIn(flat) || rewardEligibility.containsMatchIn(flat)) return null
        val debit = debitVerb.find(flat) ?: return null
        // "debited ...; SWIGGY credited" is a payment. "credited ... sent by Rahul" is money coming in.
        creditVerb.find(flat)?.let { credit -> if (credit.range.first < debit.range.first) return null }

        val amount = amountNear(flat, debit.range.first) ?: return null
        if (amount <= 0 || amount > MAX_DETECTED_PAISE) return null

        val path = upiPath.find(raw)
        var kind = when {
            cash.containsMatchIn(flat) -> PaymentKind.CASH
            path != null -> if (path.groupValues[1].equals("P2M", true)) PaymentKind.UPI_MERCHANT else PaymentKind.UPI_PERSON
            bankTransfer.containsMatchIn(flat) || cardBill.containsMatchIn(flat) -> PaymentKind.BANK_TRANSFER
            card.containsMatchIn(flat) -> PaymentKind.CARD
            else -> PaymentKind.UNKNOWN
        }
        val rawName = when {
            kind == PaymentKind.CASH -> ""
            path != null -> upiPathTail.replace(path.groupValues[2], "").trim()
            cardUseMerchant.containsMatchIn(flat) -> cardUseMerchant.find(flat)?.groupValues?.get(1)
            else -> listOf(atName, creditedName, toName).firstNotNullOfOrNull { pattern ->
                // A name needs letters: "SMS BLOCK to 919951860002" is a helpline, not a shop.
                pattern.findAll(flat).map { it.groupValues[1].trim() }.firstOrNull { it.any(Char::isLetter) && !notAName.containsMatchIn(it) }
            } ?: loneNameLine(raw)
        }.orEmpty()
        if (kind == PaymentKind.UNKNOWN && isPersonHandle(rawName)) kind = PaymentKind.UPI_PERSON
        val merchant = cleanMerchant(rawName).takeUnless { unsafeMerchant.containsMatchIn(it) }.orEmpty()
        return ParsedPayment(amount, merchant, reference.find(flat)?.groupValues?.get(1).orEmpty(), kind)
    }

    /** The amount closest to the debit word, skipping balances and credit limits. */
    private fun amountNear(flat: String, verbAt: Int): Long? {
        val candidates = currencyAmount.findAll(flat)
            .filterNot {
                val before = flat.substring(maxOf(0, it.range.first - 160), it.range.first)
                amountContext.findAll(before).lastOrNull()?.value?.lowercase(Locale.ROOT) in balanceLabels
            }
            .map { it.range.first to it.groupValues[1] }.toList()
        val chosen = candidates.minByOrNull { kotlin.math.abs(it.first - verbAt) }?.second
            ?: bareDebitAmount.find(flat)?.groupValues?.get(1)
        return chosen?.let(::toPaise)
    }

    private fun toPaise(number: String): Long? = try {
        BigDecimal(number.replace(",", "")).setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    } catch (_: Exception) { null }

    /** Some banks put the shop name alone on its own line (for example Axis card alerts). */
    private fun loneNameLine(raw: String): String? {
        val lines = raw.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.size < 4) return null
        return lines.drop(1).firstOrNull { it.length in 2..40 && it.any(Char::isLetter) && !loneNameLineBlock.containsMatchIn(it) }
    }

    /** A UPI id whose name part is a phone number belongs to a person, not a shop. */
    private fun isPersonHandle(name: String): Boolean {
        val local = name.trim().substringBefore('@', "")
        return local.length >= 8 && local.all { it.isDigit() }
    }

    internal fun cleanMerchant(value: String): String {
        var name = value.trim().trim('.', ',', ';', ':', '-', '*', ' ')
        if (name.contains('@') && !name.contains(' ')) name = name.substringBefore('@')
        name = name.replace(Regex("""\s+"""), " ").take(60).trim()
        if (name.isEmpty() || name.none(Char::isLetter)) return ""
        val letters = name.filter(Char::isLetter)
        val oneCase = letters.all(Char::isUpperCase) || letters.all(Char::isLowerCase)
        return if (!oneCase) name else name.split(' ').joinToString(" ") { word ->
            word.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }
}

/** Anything above this is treated as a parsing mistake rather than a purchase (Rs 10,00,000). */
const val MAX_DETECTED_PAISE = 100_000_000L
/** Payments at or above this are never added without asking (Rs 20,000): they are often rent or transfers. */
const val ASK_ABOVE_PAISE = 2_000_000L
const val DUPLICATE_WINDOW_MS = 3 * 60_000L
const val MANUAL_DUPLICATE_WINDOW_MS = 10 * 60_000L
const val MAX_DETECTION_MARKS = 40
const val MAX_PENDING_DETECTIONS = 20
const val MAX_MERCHANT_RULES = 200

/** Lower-case letters and digits only, so "SWIGGY", "Swiggy " and "swiggy@icici" are the same shop. */
fun merchantKey(name: String): String = name.substringBefore('@').lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

/** Bank and payment alerts come from named senders such as "AD-HDFCBK", never from an ordinary phone number. */
fun looksLikeBusinessSender(address: String): Boolean {
    val a = address.trim()
    if (a.isEmpty() || Regex("""^\+?[0-9][0-9\s-]{6,}$""").matches(a)) return false
    return a.count(Char::isLetter) >= 3
}

private class BuiltIn(val category: String, val reason: String, vararg val words: String)

private val builtInMerchants = listOf(
    BuiltIn("Food", "Convenience", "swiggy", "zomato", "blinkit", "zepto", "instamart", "dunzo", "eatclub", "faasos", "magicpin"),
    BuiltIn("Food", "Essential", "bigbasket", "dmart", "grofers", "reliancefresh", "reliancesmart", "jiomart", "grocery", "kirana", "supermarket",
        "dominos", "mcdonald", "kfc", "starbucks", "burger", "pizza", "cafe", "restaurant", "bakery", "sweets", "juice", "dhaba", "canteen", "biryani", "chai"),
    BuiltIn("Transport", "Essential", "uber", "olacabs", "olamoney", "rapido", "irctc", "metro", "dmrc", "bmrc", "redbus", "indigo", "petrol", "fuel",
        "hpcl", "bpcl", "iocl", "indianoil", "fastag", "parking", "nammayatri", "blusmart", "yulu"),
    BuiltIn("Shopping", "Essential", "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "decathlon", "croma", "ikea", "tatacliq", "snapdeal",
        "lifestyle", "zara", "uniqlo"),
    BuiltIn("Bills", "Essential", "airtel", "jio", "vodafone", "bsnl", "electricity", "bescom", "tneb", "tatapower", "adani", "indane", "broadband",
        "actfibernet", "recharge", "netflix", "spotify", "hotstar", "youtube", "googleone", "insurance", "licofindia")
)

private fun builtInFor(merchant: String): BuiltIn? {
    val key = merchantKey(merchant)
    val words = merchant.substringBefore('@').lowercase(Locale.ROOT).split(Regex("[^\\p{L}\\p{N}]+"))
    return if (key.length < 3) null else builtInMerchants.firstOrNull { b -> b.words.any { it == key || it in words } }
}

data class CategoryGuess(val category: String, val reason: String, val needsCategory: Boolean)

/** The user's own rule first, then what they chose last time for this shop, then the built-in list, else "Other". */
fun guessCategory(state: BuddyState, merchant: String): CategoryGuess {
    val key = merchantKey(merchant)
    if (key.isNotEmpty()) {
        state.merchantRules.firstOrNull { it.key == key }?.let { return CategoryGuess(it.category, it.reason, false) }
        state.expenses.filter { !it.needsCategory && merchantKey(it.merchant) == key }.maxByOrNull { it.updatedAt }
            ?.let { return CategoryGuess(it.category, it.reason, false) }
        builtInFor(merchant)?.let { return CategoryGuess(it.category, it.reason, false) }
    }
    return CategoryGuess("Other", "Essential", true)
}

/** True when the user has a rule for this shop, has logged it before, or it is on the built-in list. */
fun isKnownMerchant(state: BuddyState, merchant: String): Boolean {
    val key = merchantKey(merchant)
    if (key.isEmpty()) return false
    return state.merchantRules.any { it.key == key } || state.expenses.any { merchantKey(it.merchant) == key } || builtInFor(merchant) != null
}

/** Null when the payment is clear enough to add on its own; otherwise the plain reason for asking first. */
fun reasonToAsk(state: BuddyState, payment: ParsedPayment): String? = when {
    payment.kind == PaymentKind.CASH -> "Cash withdrawal"
    payment.merchant.isBlank() -> "No shop name in the message"
    payment.amountPaise >= ASK_ABOVE_PAISE -> "Large amount"
    payment.kind == PaymentKind.UPI_PERSON -> "Looks like a payment to a person"
    payment.kind == PaymentKind.BANK_TRANSFER -> "Bank transfer. Check what this was for"
    isKnownMerchant(state, payment.merchant) -> null
    payment.kind == PaymentKind.CARD || payment.kind == PaymentKind.UPI_MERCHANT -> null
    else -> "New name. Add it once and it will be automatic next time"
}

fun isDuplicateDetection(state: BuddyState, payment: ParsedPayment, now: Long): Boolean {
    val seen = state.recentDetections.any { m ->
        m.amountPaise == payment.amountPaise && kotlin.math.abs(now - m.at) <= DUPLICATE_WINDOW_MS &&
            (m.ref.isEmpty() || payment.ref.isEmpty() || m.ref == payment.ref)
    }
    if (seen) return true
    val today = dateInIndia(now)
    // The user typed this purchase in themselves a moment ago.
    return state.expenses.any { it.source != SOURCE_AUTO && it.kind == "expense" && it.date == today && it.amountPaise == payment.amountPaise && now - it.updatedAt in 0..MANUAL_DUPLICATE_WINDOW_MS }
}

private fun dateInIndia(ms: Long): String = Instant.ofEpochMilli(ms).atZone(INDIA_ZONE).toLocalDate().toString()

sealed interface DetectionOutcome {
    /** Not a payment, switched off, or already counted. Nothing changed. */
    data object Ignored : DetectionOutcome
    data class Added(val state: BuddyState, val expense: Expense) : DetectionOutcome
    data class Asked(val state: BuddyState, val pending: PendingDetection) : DetectionOutcome
}

/** Applies one parsed payment to [state]. Pure: the caller saves the returned state and shows the notification. */
fun applyDetection(state: BuddyState, payment: ParsedPayment, now: Long = System.currentTimeMillis()): DetectionOutcome {
    if (!state.autoDetect || !state.onboardingDone) return DetectionOutcome.Ignored
    if (isDuplicateDetection(state, payment, now)) return DetectionOutcome.Ignored
    val marks = (state.recentDetections.filter { now - it.at < 24 * 60 * 60_000L } + DetectionMark(payment.amountPaise, now, payment.ref)).takeLast(MAX_DETECTION_MARKS)
    val guess = guessCategory(state, payment.merchant)
    val date = dateInIndia(now)
    val ask = reasonToAsk(state, payment)
    if (ask != null) {
        val merchant = payment.merchant.ifBlank { if (payment.kind == PaymentKind.CASH) "Cash withdrawal" else "" }
        val pending = PendingDetection(amountPaise = payment.amountPaise, merchant = merchant, date = date, category = guess.category,
            reason = guess.reason, needsCategory = guess.needsCategory, why = ask, detectedAt = now)
        return DetectionOutcome.Asked(state.copy(recentDetections = marks, pendingDetections = (state.pendingDetections + pending).takeLast(MAX_PENDING_DETECTIONS)), pending)
    }
    val expense = Expense(date = date, amountPaise = payment.amountPaise, merchant = payment.merchant, category = guess.category, reason = guess.reason,
        updatedAt = now, source = SOURCE_AUTO, heldUntil = now + AUTO_HOLD_MS, needsCategory = guess.needsCategory)
    val reopened = state.checkIns.firstOrNull { it.date == date }
        ?: state.heldCheckIns.values.firstOrNull { it.date == date }
    return DetectionOutcome.Added(
        state.copy(
            recentDetections = marks,
            expenses = state.expenses + expense,
            checkIns = state.checkIns.filterNot { it.date == date },
            heldCheckIns = if (reopened != null) state.heldCheckIns + (expense.id to reopened) else state.heldCheckIns
        ),
        expense
    )
}

/** Removes an auto-added purchase and puts back the day's check-in if adding it had re-opened one. */
fun undoAutoExpense(state: BuddyState, id: String, now: Long = System.currentTimeMillis()): BuddyState {
    val expense = state.expenses.firstOrNull { it.id == id && it.source == SOURCE_AUTO && it.heldUntil > now } ?: return state
    val restored = state.heldCheckIns[id]?.takeIf { c ->
        state.checkIns.none { it.date == c.date } && state.expenses.none {
            it.id != id && it.date == c.date && (it.heldUntil > now || it.updatedAt >= c.completedAt)
        }
    }
    return state.copy(
        expenses = state.expenses.filterNot { it.id == id },
        checkIns = if (restored != null) state.checkIns + restored else state.checkIns,
        heldCheckIns = state.heldCheckIns - id
    )
}

/** The user confirmed an auto-added purchase: share it with the circle now instead of waiting out the hold. */
fun keepAutoExpense(state: BuddyState, id: String): BuddyState {
    val expense = state.expenses.firstOrNull { it.id == id && it.source == SOURCE_AUTO && it.heldUntil != 0L } ?: return state
    return state.copy(
        expenses = state.expenses.map { if (it.id == id) it.copy(heldUntil = 0) else it },
        heldCheckIns = state.heldCheckIns.filterValues { it.date != expense.date }
    )
}

/** Ends every hold that has run out. Returns [state] itself when nothing changed. */
fun releaseExpiredHolds(state: BuddyState, now: Long = System.currentTimeMillis()): BuddyState {
    val expired = state.expenses.filter { it.heldUntil != 0L && it.heldUntil <= now }.map { it.id }.toSet()
    if (expired.isEmpty()) return state
    val dates = state.expenses.filter { it.id in expired }.map { it.date }.toSet()
    return state.copy(expenses = state.expenses.map { if (it.id in expired) it.copy(heldUntil = 0) else it },
        heldCheckIns = state.heldCheckIns.filterValues { it.date !in dates })
}

/** Keep a reopened day's old status private for as long as all the purchases that reopened it are held. */
fun shareableCheckIns(state: BuddyState, now: Long): List<CheckIn> {
    val originals = state.heldCheckIns.values.distinctBy { it.date }.filter { original ->
        state.checkIns.none { it.date == original.date } &&
            state.expenses.any { it.date == original.date && it.heldUntil > now } &&
            state.expenses.none { it.date == original.date && it.heldUntil <= now && it.updatedAt >= original.completedAt }
    }
    return state.checkIns + originals
}

/** Purchases that are still inside their undo window. */
fun heldExpenses(state: BuddyState, now: Long = System.currentTimeMillis()): List<Expense> =
    state.expenses.filter { it.heldUntil > now }.sortedByDescending { it.updatedAt }

/** The user said yes to a queued payment. It was reviewed, so it is shared straight away. */
fun addPendingDetection(state: BuddyState, id: String, now: Long = System.currentTimeMillis()): Pair<BuddyState, Expense?> {
    val p = state.pendingDetections.firstOrNull { it.id == id } ?: return state to null
    val expense = Expense(date = p.date, amountPaise = p.amountPaise, merchant = p.merchant, category = p.category, reason = p.reason,
        updatedAt = now, source = SOURCE_AUTO, needsCategory = p.needsCategory)
    return state.copy(
        pendingDetections = state.pendingDetections.filterNot { it.id == id },
        expenses = state.expenses + expense,
        checkIns = state.checkIns.filterNot { it.date == p.date },
        heldCheckIns = state.heldCheckIns.filterValues { it.date != p.date }
    ) to expense
}

fun skipPendingDetection(state: BuddyState, id: String): BuddyState = state.copy(pendingDetections = state.pendingDetections.filterNot { it.id == id })

/** Adds or replaces the rule for [merchant]. Returns [state] unchanged for a blank name. */
fun upsertMerchantRule(state: BuddyState, merchant: String, category: String, reason: String): BuddyState {
    val key = merchantKey(merchant)
    if (key.isEmpty() || category.isBlank() || reason.isBlank()) return state
    val rule = MerchantRule(key, merchant.trim().take(60), category, reason)
    return state.copy(merchantRules = (state.merchantRules.filterNot { it.key == key } + rule).takeLast(MAX_MERCHANT_RULES))
}

fun deleteMerchantRule(state: BuddyState, key: String): BuddyState = state.copy(merchantRules = state.merchantRules.filterNot { it.key == key })
