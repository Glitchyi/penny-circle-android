package com.spendingbuddy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/** Optional aggregate-only probe of the licensed UCI SMS corpus; ham/spam are not payment labels. */
class UciSmsSpamEvaluationTest {
    @Test fun reportParserCandidatesForCanonicalUciSmsCollection() {
        val suppliedPath = System.getProperty("publicSmsUciPath").orEmpty().trim()
        assumeTrue("Set -PpublicSmsUciPath to the extracted UCI SMSSpamCollection file to enable this evaluation", suppliedPath.isNotEmpty())

        val path = Path.of(suppliedPath)
        assertTrue("UCI data file path must exist", Files.isRegularFile(path))
        val bytes = Files.readAllBytes(path)
        assertEquals("Unexpected UCI data-file SHA-256", EXPECTED_DATA_SHA256, sha256(bytes))

        val counts = Counts()
        Files.newBufferedReader(path).useLines { lines ->
            lines.forEach { line ->
                val separator = line.indexOf('\t')
                assertTrue("UCI row must contain a label and tab", separator > 0)
                val label = line.substring(0, separator)
                assertTrue("UCI row label must be ham or spam", label == "ham" || label == "spam")
                val body = line.substring(separator + 1)
                counts.total++
                if (label == "ham") counts.ham++ else counts.spam++

                val parsed = PaymentMessageParser.parse(body)
                if (parsed == null) {
                    counts.parserIgnored++
                } else {
                    counts.parsedCandidates++
                    if (label == "ham") counts.hamParsed++ else counts.spamParsed++
                    when (applyDetection(BuddyState(autoDetect = true, onboardingDone = true), parsed, EVALUATION_TIME_MS)) {
                        is DetectionOutcome.Added -> {
                            counts.autoAddCandidates++
                            if (label == "ham") counts.hamAutoAdd++ else counts.spamAutoAdd++
                        }
                        is DetectionOutcome.Asked -> counts.confirmationCandidates++
                        DetectionOutcome.Ignored -> counts.detectorIgnored++
                    }
                }
            }
        }

        assertEquals("UCI catalog row count", EXPECTED_ROWS, counts.total)
        assertEquals("UCI ham row count", EXPECTED_HAM, counts.ham)
        assertEquals("UCI spam row count", EXPECTED_SPAM, counts.spam)
        // Aggregate only: the UCI labels say ham/spam, not payment/nonpayment. Do not print message text or row IDs.
        println(
            "UCI SMS aggregate evaluation (not accuracy): total=${counts.total}, ham=${counts.ham}, spam=${counts.spam}, " +
                "parsedCandidates=${counts.parsedCandidates} [ham=${counts.hamParsed}, spam=${counts.spamParsed}], " +
                "autoAddCandidates=${counts.autoAddCandidates} [ham=${counts.hamAutoAdd}, spam=${counts.spamAutoAdd}], " +
                "confirmationCandidates=${counts.confirmationCandidates}, parserIgnored=${counts.parserIgnored}, " +
                "detectorIgnored=${counts.detectorIgnored}"
        )
    }

    private class Counts {
        var total = 0
        var ham = 0
        var spam = 0
        var parsedCandidates = 0
        var hamParsed = 0
        var spamParsed = 0
        var autoAddCandidates = 0
        var hamAutoAdd = 0
        var spamAutoAdd = 0
        var confirmationCandidates = 0
        var parserIgnored = 0
        var detectorIgnored = 0
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private companion object {
        const val EXPECTED_ROWS = 5_574
        const val EXPECTED_HAM = 4_827
        const val EXPECTED_SPAM = 747
        const val EXPECTED_DATA_SHA256 = "7d039a24a6083ed9ef0f806ebad56bbb976e3aeb8de05669173bfdc4996c239d"
        const val EVALUATION_TIME_MS = 1_800_000_000_000L
    }
}
