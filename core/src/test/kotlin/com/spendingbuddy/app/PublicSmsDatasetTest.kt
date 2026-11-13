package com.spendingbuddy.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Evaluation against an attributed, pinned, synthetic public corpus; this is not live-bank certification. */
class PublicSmsDatasetTest {
    private data class Example(
        val id: String,
        val body: String,
        val bookable: Boolean,
        val amountPaise: Long?,
        val kind: String,
        val rail: String
    )

    private val examples: List<Example> by lazy {
        val stream = javaClass.getResourceAsStream("/sms-corpus/indian-payment-sms-dataset.json")
            ?: error("Pinned public SMS corpus resource is missing")
        val root = stream.bufferedReader().use { Json.parseToJsonElement(it.readText()).jsonObject }
        root.getValue("rows").jsonArray.map { rowElement ->
            val row = rowElement.jsonObject
            val expected = row.getValue("expected").jsonObject
            val amount = expected.getValue("transaction_amount_paise").takeUnless { it == JsonNull }?.jsonPrimitive?.long
            Example(
                id = row.getValue("id").jsonPrimitive.content,
                // Upstream stores multiline SMS examples as literal backslash-n sequences.
                body = row.getValue("raw_sms").jsonPrimitive.content.replace("\\n", "\n"),
                bookable = expected.getValue("bookable").jsonPrimitive.boolean,
                amountPaise = amount,
                kind = row.getValue("kind").jsonPrimitive.content,
                rail = row.getValue("rail").jsonPrimitive.content
            )
        }
    }

    @Test fun pinnedCorpusShapeMatchesItsPublishedCounts() {
        assertEquals(172, examples.size)
        assertEquals(78, examples.count { it.bookable })
        assertEquals(94, examples.count { !it.bookable })
    }

    @Test fun labeledCompletedDebitsParseWithTheExactInrAmount() {
        val errors = mutableListOf<String>()
        examples.filter { it.bookable }.forEach { example ->
            val parsed = PaymentMessageParser.parse(example.body)
            if (parsed == null) {
                errors += "${example.id} [${example.rail}/${example.kind}]: expected completed debit to parse"
                return@forEach
            }
            if (parsed.amountPaise != example.amountPaise) {
                errors += "${example.id}: expected ${example.amountPaise} paise, got ${parsed.amountPaise}"
            }
            val outcome = applyDetection(
                BuddyState(autoDetect = true, onboardingDone = true),
                parsed,
                EVALUATION_TIME_MS
            )
            if (outcome == DetectionOutcome.Ignored) {
                errors += "${example.id}: labeled debit parsed but detection ignored it"
            }
            val requiresConfirmation = example.rail in setOf("atm", "transfer") ||
                parsed.kind == PaymentKind.CASH || parsed.kind == PaymentKind.UPI_PERSON ||
                parsed.merchant.isBlank() || parsed.amountPaise >= ASK_ABOVE_PAISE
            if (requiresConfirmation && outcome is DetectionOutcome.Added) {
                errors += "${example.id} [${example.rail}]: ambiguous/cash/person/large payment auto-added"
            }
        }
        assertTrue("Public corpus debit coverage failures (${errors.size}):\n${errors.joinToString("\n")}", errors.isEmpty())
    }

    @Test fun labeledNonBookableMessagesNeverAutoAdd() {
        val errors = mutableListOf<String>()
        examples.filterNot { it.bookable }.forEach { example ->
            val parsed = PaymentMessageParser.parse(example.body) ?: return@forEach
            val outcome = applyDetection(
                BuddyState(autoDetect = true, onboardingDone = true),
                parsed,
                EVALUATION_TIME_MS
            )
            if (outcome is DetectionOutcome.Added) {
                errors += "${example.id} [${example.rail}/${example.kind}]: ${parsed.amountPaise} paise auto-added as '${outcome.expense.merchant}'"
            }
        }
        assertTrue("Unsafe false auto-adds in public corpus (${errors.size}):\n${errors.joinToString("\n")}", errors.isEmpty())
    }

    private companion object {
        const val EVALUATION_TIME_MS = 1_800_000_000_000L
    }
}
