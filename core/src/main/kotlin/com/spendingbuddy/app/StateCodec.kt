package com.spendingbuddy.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads and writes the on-device state file.
 *
 * kotlinx.serialization applies Kotlin default values for missing fields, unlike Gson (which
 * left new non-null fields as null and would crash after an update). v0.1 files are migrated:
 * hosted-backend fields are dropped and existing users skip onboarding.
 */
object StateCodec {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val legacyKeys = setOf(
        "friend", "incomingNudge", "suggestions", "inviteCode", "syncStatus",
        "lastSync", "backendUrl", "deviceToken"
    )

    fun encode(state: BuddyState): String = json.encodeToString(BuddyState.serializer(), state)

    fun decode(text: String): BuddyState {
        val root = json.parseToJsonElement(text).jsonObject
        val version = (root["schemaVersion"] as? JsonPrimitive)?.intOrNull ?: 1
        if (version >= STATE_SCHEMA_VERSION) return json.decodeFromJsonElement(BuddyState.serializer(), root)
        // v0.1 → v0.2 migration. Backend-era friend data was never peer-verified, so drop it.
        val existingUser = ((root["name"] as? JsonPrimitive)?.content?.isNotBlank() == true) ||
            ((root["expenses"]?.jsonArray?.size ?: 0) > 0) || ((root["checkIns"]?.jsonArray?.size ?: 0) > 0)
        val cleaned = JsonObject(root.filterKeys { it !in legacyKeys } + mapOf(
            "schemaVersion" to JsonPrimitive(STATE_SCHEMA_VERSION),
            "onboardingDone" to JsonPrimitive(existingUser)
        ))
        val migrated = json.decodeFromJsonElement(BuddyState.serializer(), cleaned)
        val last = migrated.expenses.maxByOrNull { it.updatedAt }?.category ?: migrated.lastCategory
        return migrated.copy(lastCategory = last, revision = maxOf(migrated.revision, 1))
    }

    /** Backup files exclude in-progress screenshot drafts and the peer link (keys stay on the phone). */
    fun encodeBackup(state: BuddyState): String = encode(
        state.copy(importImages = emptyList(), importCandidates = emptyList(), peer = null, friend = null,
            outbox = emptyList(), seenEventIds = emptyList(), incomingNudge = null, outgoingNudge = null,
            suggestions = emptyList(), sentSuggestion = null, stayReachable = false)
    )

    @Suppress("unused")
    private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

    @Suppress("unused")
    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.content ?: this[key]?.jsonPrimitive?.content
}
