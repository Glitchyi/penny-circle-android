package com.spendingbuddy.app

import java.time.LocalDate

const val INCOMING_WARNING = "Incoming/refund transaction; do not count automatically"

fun ImportCandidate.isIncoming(): Boolean = warning.contains(INCOMING_WARNING)
fun kindFor(candidate: ImportCandidate): String = if (candidate.isIncoming()) "refund" else "expense"

/** Errors attach to editable fields; validation never rewrites extracted text or selections. */
data class ImportCandidateErrors(
    val amount: String? = null,
    val date: String? = null,
    val merchant: String? = null,
    val category: String? = null,
    val reason: String? = null
) {
    val first: String? get() = amount ?: date ?: merchant ?: category ?: reason
}

fun importCandidateErrors(candidate: ImportCandidate, today: LocalDate = todayIndia()): ImportCandidateErrors {
    val amount = amountToPaise(candidate.amountText)
    val date = runCatching { LocalDate.parse(candidate.date) }.getOrNull()
    val missingDetails = "Add a merchant, category and reason for each purchase"
    return ImportCandidateErrors(
        amount = when {
            amount == null -> "Fix the amount for ${candidate.merchant.ifBlank { "a purchase" }}"
            amount <= 0L -> "Amounts must be above ₹0"
            else -> null
        },
        date = when {
            date == null -> "Confirm the date for ${candidate.merchant.ifBlank { "each purchase" }}"
            date > today -> "Purchase dates can't be in the future"
            else -> null
        },
        merchant = missingDetails.takeIf { candidate.merchant.isBlank() },
        category = missingDetails.takeIf { candidate.category.isBlank() },
        reason = missingDetails.takeIf { candidate.reason.isBlank() }
    )
}

fun importReviewError(candidates: List<ImportCandidate>, today: LocalDate = todayIndia()): String? {
    val selected = candidates.filter { it.selected }
    if (selected.isEmpty()) return "Select at least one purchase, or add it by hand"
    return selected.firstNotNullOfOrNull { importCandidateErrors(it, today).first }
}

/** Image processing may refresh the candidate set while locally edited fields remain unfinished. */
fun mergeImportReviewDrafts(drafts: List<ImportCandidate>, incoming: List<ImportCandidate>): List<ImportCandidate> =
    incoming.map { candidate -> drafts.firstOrNull { it.id == candidate.id } ?: candidate }

/** Called only after explicit review/save; duplicates remain selected unless the owner changes them. */
fun reviewedImportExpenses(candidates: List<ImportCandidate>, today: LocalDate = todayIndia()): List<Expense>? {
    if (importReviewError(candidates, today) != null) return null
    return candidates.filter { it.selected }.map {
        Expense(date = it.date, amountPaise = requireNotNull(amountToPaise(it.amountText)),
            merchant = it.merchant.trim(), category = it.category, reason = it.reason, kind = kindFor(it))
    }
}
