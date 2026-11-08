package com.spendingbuddy.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import kotlin.math.max

/** Imports a shared screenshot without sending the image or its OCR text off-device. */
class ScreenshotImporter(private val context: Context) {
    private val appContext = context.applicationContext
    private val importsDir: File
        get() = File(appContext.filesDir, IMPORT_DIRECTORY)

    suspend fun importUri(uri: Uri): Pair<ImportImage, List<ImportCandidate>> {
        val id = UUID.randomUUID().toString()
        val destination = File(importsDir, "$id.png")
        try {
            importsDir.mkdirs()
            copyCapped(uri, destination)
        } catch (cancelled: CancellationException) {
            destination.delete()
            throw cancelled
        } catch (error: Exception) {
            destination.delete()
            return ImportImage(id = id, path = destination.absolutePath, status = "failed", error = safeMessage(error)) to emptyList()
        }

        // Keep a successfully copied image when recognition fails so the user can retry OCR.
        return try {
            recognize(destination, id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ImportImage(id = id, path = destination.absolutePath, status = "failed", error = safeMessage(error)) to emptyList()
        }
    }

    suspend fun retry(image: ImportImage): Pair<ImportImage, List<ImportCandidate>> {
        val file = File(image.path)
        val id = image.id
        return try {
            if (!isPrivateImport(file) || !file.isFile) {
                return image.copy(status = "failed", error = "Temporary screenshot copy is unavailable") to emptyList()
            }
            recognize(file, id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            image.copy(status = "failed", error = safeMessage(error)) to emptyList()
        }
    }

    /** Removes only the app-owned temporary copy, never the user's source/gallery URI. */
    fun delete(image: ImportImage) {
        val file = File(image.path)
        if (isPrivateImport(file)) file.delete()
    }

    private suspend fun recognize(file: File, imageId: String): Pair<ImportImage, List<ImportCandidate>> {
        val bitmap = decodeSampled(file)
            ?: return ImportImage(imageId, file.absolutePath, "failed", "The image could not be decoded") to emptyList()
        return try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val text = try {
                recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
            } finally {
                recognizer.close()
            }
            val candidates = TransactionParser.parse(text, imageId)
            ImportImage(imageId, file.absolutePath, "ready") to candidates
        } finally {
            bitmap.recycle()
        }
    }

    private fun copyCapped(uri: Uri, destination: File) {
        val expectedLength = appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        if (expectedLength != null && expectedLength > MAX_BYTES) {
            throw IllegalArgumentException("Screenshot is larger than 20 MB")
        }
        val temporary = File(destination.parentFile, ".${destination.name}.part")
        try {
            var copied = 0L
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        copied += count
                        if (copied > MAX_BYTES) throw IllegalArgumentException("Screenshot is larger than 20 MB")
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            } ?: throw IllegalArgumentException("Unable to read shared image")
            if (!temporary.renameTo(destination)) throw IllegalStateException("Unable to save temporary screenshot")
        } finally {
            temporary.delete()
        }
    }

    private fun decodeSampled(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_BITMAP_DIMENSION) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun isPrivateImport(file: File): Boolean {
        return try {
            val root = importsDir.canonicalFile.toPath()
            file.canonicalFile.toPath().startsWith(root)
        } catch (_: Exception) {
            false
        }
    }

    private fun safeMessage(error: Exception): String = error.message?.takeIf { it.isNotBlank() } ?: "Screenshot processing failed"

    private companion object {
        const val IMPORT_DIRECTORY = "imports"
        const val MAX_BYTES = 20L * 1024L * 1024L
        const val COPY_BUFFER_SIZE = 32 * 1024
        const val MAX_BITMAP_DIMENSION = 2048
    }
}

/** Conservative, text-only parser for review drafts. It never confirms or saves an expense. */
object TransactionParser {
    private val amountPattern = Regex(
        """(?i)(₹|rs\.?|inr)\s*((?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+|[0-9]{1,3}(?:,[0-9]{2})+,[0-9]{3})(?:\.[0-9]{1,2})?)(?![0-9.,])"""
    )
    private val fullDatePatterns = listOf(
        Regex("""^\s*(\d{4})[-/]([01]?\d)[-/]([0-3]?\d)\s*[,:]?\s*$"""),
        Regex("""^\s*([0-3]?\d)[-/]([01]?\d)[-/](\d{4})\s*[,:]?\s*$"""),
        Regex("""^\s*([0-3]?\d)\s+([A-Za-z]{3,9})\s+(\d{4})\s*[,:]?\s*$"""),
        Regex("""^\s*([A-Za-z]{3,9})\s+([0-3]?\d),?\s+(\d{4})\s*[,:]?\s*$"""),
    )
    private val partialDatePatterns = listOf(
        Regex("""^\s*([0-3]?\d)[-/]([01]?\d)\s*[,:]?\s*$"""),
        Regex("""^\s*([0-3]?\d)\s+([A-Za-z]{3,9})\s*[,:]?\s*$"""),
        Regex("""^\s*([A-Za-z]{3,9})\s+([0-3]?\d)\s*[,:]?\s*$"""),
    )
    private val monthNumbers = mapOf(
        "jan" to 1, "january" to 1, "feb" to 2, "february" to 2,
        "mar" to 3, "march" to 3, "apr" to 4, "april" to 4,
        "may" to 5, "jun" to 6, "june" to 6, "jul" to 7, "july" to 7,
        "aug" to 8, "august" to 8, "sep" to 9, "sept" to 9, "september" to 9,
        "oct" to 10, "october" to 10, "nov" to 11, "november" to 11,
        "dec" to 12, "december" to 12
    )
    private val transactionWords = Regex("""(?i)\b(paid|pay|payment|sent|send|debited|debit|transferred|transfer|purchase)\b""")
    private val incomingWords = Regex("""(?i)\b(received|receive|credited|credit|refund|refunded|cashback|reversal|reversed|returned|from)\b""")
    private val terminalWords = Regex("""(?i)\b(failed|failure|cancelled|canceled|declined)\b""")
    private val metadataHeading = Regex("""(?i)^(?:total|balance|transaction\s+history|transactions?|search(?:ing)?|upi\s*id|status|date|time|phone(?:\s+number)?|account|bank)\b.*$""")
    private val statusLine = Regex("""(?i)^(?:successful?|completed?|pending|processing|failed|failure|cancelled|canceled|declined|paid|payment)\b.*$""")
    private val timeLine = Regex("""^\s*\d{1,2}:\d{2}(?::\d{2})?\s*(?:[AaPp][Mm])?\s*$""")
    private val phoneLine = Regex("""^\s*\+?\d[\d\s().-]{6,}\d\s*$""")

    fun parse(text: String, imageId: String): List<ImportCandidate> {
        val lines = preserveLines(text)
        if (lines.isEmpty()) return emptyList()
        val headings = lines.mapIndexedNotNull { index, line -> parseDateHeading(line)?.let { index to it } }
        val output = mutableListOf<ImportCandidate>()

        lines.forEachIndexed { lineIndex, line ->
            amountPattern.findAll(line).forEach { match ->
                val window = transactionWindow(lines, lineIndex)
                val localText = window.joinToString("\n")
                if (terminalWords.containsMatchIn(localText)) return@forEach
                val incoming = incomingWords.containsMatchIn(localText)
                val outgoing = window.any(::isPaymentContextLine)
                val merchant = extractMerchant(lines, lineIndex, match.range)
                // A bare amount is not enough: it could be a balance, total, or unrelated number.
                if (!outgoing && !incoming && merchant.isBlank()) return@forEach

                val date = dateFor(lineIndex, lines, headings)
                val warnings = mutableListOf("Review extracted details")
                if (date == null) warnings += "Date not detected; enter it before saving"
                else if (date.warning != null) warnings += date.warning
                if (merchant.isBlank()) warnings += "Merchant not detected; add it manually"
                if (incoming) warnings += "Incoming/refund transaction; do not count automatically"
                output += ImportCandidate(
                    imageId = imageId,
                    amountText = "₹${match.groupValues[2]}",
                    merchant = merchant,
                    date = date?.value.orEmpty(),
                    selected = !incoming,
                    warning = warnings.distinct().joinToString("; "),
                    rawText = window.joinToString("\n").trim()
                )
            }
        }
        return output
    }

    /** Keeps OCR line boundaries and only joins a small neighborhood around each amount. */
    private fun preserveLines(text: String): List<String> = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .split('\n')
        .map { it.trim() }
        .let { lines -> if (lines.any { it.isNotEmpty() }) lines else emptyList() }

    private fun transactionWindow(lines: List<String>, index: Int): List<String> {
        var start = index
        for (candidate in (index - 1).downTo((index - 2).coerceAtLeast(0))) {
            if (isTransactionBoundary(lines[candidate])) break
            start = candidate
        }
        // Read the amount row and its own preceding context only. Looking forward can
        // attach the next row's merchant/status to a bare amount before its own amount.
        return lines.subList(start, index + 1)
    }

    private fun isTransactionBoundary(line: String): Boolean =
        line.isBlank() || amountPattern.containsMatchIn(line) || parseDateHeading(line) != null

    private fun extractMerchant(lines: List<String>, amountLineIndex: Int, amountRange: IntRange): String {
        val amountLine = lines[amountLineIndex]
        val sameLineAfter = amountLine.substring(amountRange.last + 1)
        val sameLineBefore = amountLine.substring(0, amountRange.first)
        val explicitCandidates = buildList {
            addAll(explicitMerchantParts(sameLineAfter))
            addAll(explicitMerchantParts(sameLineBefore))
            for (distance in 1..2) {
                val before = amountLineIndex - distance
                if (before >= 0) {
                    if (isTransactionBoundary(lines[before])) break
                    addAll(explicitMerchantParts(lines[before]))
                }
            }
        }
        explicitCandidates.firstOrNull { isPlausibleMerchant(it) }?.let { return cleanMerchant(it) }

        // Google Pay can put a merchant name on the line immediately before its amount.
        // Only accept plain text from the preceding bounded row, never another amount row
        // or a date/status/navigation heading.
        for (distance in 1..2) {
            val before = amountLineIndex - distance
            if (before < 0 || isTransactionBoundary(lines[before])) break
            val candidate = lines[before]
            if (isPlausibleMerchant(candidate)) return cleanMerchant(candidate)
        }
        return listOf(sameLineBefore, sameLineAfter)
            .firstOrNull { isPlausibleMerchant(it) }
            ?.let(::cleanMerchant)
            .orEmpty()
    }

    private fun explicitMerchantParts(line: String): List<String> {
        val trimmed = line.trim()
        if (trimmed.isBlank()) return emptyList()
        val results = mutableListOf<String>()
        Regex("""(?i)\b(?:paid|pay|payment|sent|send|transferred|transfer|purchase)\s+to\s+(.+)$""").find(trimmed)?.groupValues?.getOrNull(1)?.let(results::add)
        Regex("""(?i)\bto\s+(.+)$""").find(trimmed)?.groupValues?.getOrNull(1)?.let(results::add)
        Regex("""(?i)\b(?:received|receive|credited|credit|refund|refunded|cashback|reversal|reversed|returned)\s+from\s+(.+)$""").find(trimmed)?.groupValues?.getOrNull(1)?.let(results::add)
        Regex("""(?i)\bfrom\s+(.+)$""").find(trimmed)?.groupValues?.getOrNull(1)?.let(results::add)
        Regex("""(?i)\bat\s+(.+)$""").find(trimmed)?.groupValues?.getOrNull(1)?.let(results::add)
        return results
    }

    private fun isPlausibleMerchant(value: String): Boolean {
        val candidate = cleanMerchant(value)
        if (candidate.length < 2 || amountPattern.containsMatchIn(candidate)) return false
        if (isMetadataLine(candidate) || terminalWords.containsMatchIn(candidate) || statusLine.matches(candidate)) return false
        return candidate.any { it.isLetter() }
    }

    private fun isMetadataLine(line: String): Boolean {
        val candidate = line.trim()
        return candidate.isBlank() || parseDateHeading(candidate) != null || metadataHeading.matches(candidate) ||
            statusLine.matches(candidate) || timeLine.matches(candidate) || phoneLine.matches(candidate)
    }

    private fun isPaymentContextLine(line: String): Boolean {
        val candidate = line.trim()
        if (candidate.isBlank() || parseDateHeading(candidate) != null || metadataHeading.matches(candidate) ||
            timeLine.matches(candidate) || phoneLine.matches(candidate)) return false
        // A standalone payment label is useful context for a review candidate with no
        // merchant, while completed/pending/status rows are not financial confirmation.
        val statusOnly = Regex("""(?i)^(?:successful?|completed?|pending|processing|failed|failure|cancelled|canceled|declined)(?:\b.*)?$|(?:payment|paid)\s+(?:successful?|completed?|pending|processing|failed|failure|cancelled|canceled|declined)\b.*$""")
        return transactionWords.containsMatchIn(candidate) && !statusOnly.matches(candidate)
    }

    private fun cleanMerchant(value: String): String = value
        .replace(Regex("""[|•]+.*$"""), "")
        .replace(Regex("""(?i)\s+(?:on|at)\s+\d{1,4}[-/]\d{1,2}.*$"""), "")
        .trim(' ', ':', '-', '–', '.', ',')
        .replace(Regex("""\s+"""), " ")

    private fun dateFor(index: Int, lines: List<String>, headings: List<Pair<Int, DateInfo>>): DateInfo? {
        val heading = headings.lastOrNull { it.first <= index && index - it.first <= MAX_DATE_CONTEXT_LINES } ?: return null
        return heading.second
    }

    private fun parseDateHeading(line: String): DateInfo? {
        val normalized = line.trim().trimEnd(':', '|').trim()
        when (normalized.lowercase(Locale.ROOT)) {
            "today" -> return DateInfo(LocalDate.now(INDIA_ZONE).toString(), "Relative date 'Today'; review before saving")
            "yesterday" -> return DateInfo(LocalDate.now(INDIA_ZONE).minusDays(1).toString(), "Relative date 'Yesterday'; review before saving")
        }
        fullDatePatterns.forEach { pattern ->
            val match = pattern.matchEntire(normalized) ?: return@forEach
            val date = parseFullDate(match) ?: return@forEach
            return DateInfo(date.toString(), null)
        }
        partialDatePatterns.forEach { pattern ->
            if (pattern.matches(normalized)) return DateInfo(normalized, "Date year is uncertain; review before saving")
        }
        return null
    }

    private fun parseFullDate(match: MatchResult): LocalDate? {
        val values = match.groupValues.drop(1)
        return try {
            val (year, month, day) = when {
                values[0].length == 4 -> Triple(values[0].toInt(), values[1].toInt(), values[2].toInt())
                values[2].length == 4 && monthNumbers[values[0].lowercase(Locale.ROOT)] != null -> Triple(values[2].toInt(), monthNumbers.getValue(values[0].lowercase(Locale.ROOT)), values[1].toInt())
                values[2].length == 4 && monthNumbers[values[1].lowercase(Locale.ROOT)] != null -> Triple(values[2].toInt(), monthNumbers.getValue(values[1].lowercase(Locale.ROOT)), values[0].toInt())
                values[2].length == 4 -> Triple(values[2].toInt(), values[1].toInt(), values[0].toInt())
                else -> return null
            }
            LocalDate.of(year, month, day)
        } catch (_: Exception) {
            null
        }
    }

    private data class DateInfo(val value: String, val warning: String?)

    private const val MAX_DATE_CONTEXT_LINES = 20
}
