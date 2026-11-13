package com.spendingbuddy.app

import com.spendingbuddy.app.p2p.MAX_TOMBSTONES
import com.spendingbuddy.app.p2p.MAX_SEEN_EVENT_IDS
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.UUID
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
class UnsupportedSchemaException(val version: Int) : Exception("Unsupported schema version $version")

/** Stable validation outcomes; Android owns the displayed recovery copy. */
enum class BackupRestoreIssue { UnsupportedVersion, UnsupportedTimers, InvalidLimits, InvalidPurchases, TooManyMembers, EmptyBackup, NoPurchases }

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

    /** Keys whose shape changed in schema 3 (single peer -> group); they are rebuilt from the v2 values. */
    private val v2PeerKeys = setOf(
        "peer", "friend", "outbox", "seenEventIds", "incomingNudge", "outgoingNudge", "suggestions", "sentSuggestion"
    )

    const val MIGRATED_GROUP_NAME = "My circle"

    fun encode(state: BuddyState): String = json.encodeToString(BuddyState.serializer(), state)

    fun decode(text: String): BuddyState {
        val root = json.parseToJsonElement(text).jsonObject
        val version = (root["schemaVersion"] as? JsonPrimitive)?.intOrNull ?: 1
        if (version < 1) throw IllegalArgumentException("Invalid schema version $version")
        if (version > STATE_SCHEMA_VERSION) throw UnsupportedSchemaException(version)
        if (version == STATE_SCHEMA_VERSION) return json.decodeFromJsonElement(BuddyState.serializer(), root)
        var obj: JsonObject = root
        if (version == 1) {
            // v0.1 -> v0.2. Backend-era friend data was never peer-verified, so drop it.
            val existingUser = ((root["name"] as? JsonPrimitive)?.content?.isNotBlank() == true) ||
                ((root["expenses"]?.jsonArray?.size ?: 0) > 0) || ((root["checkIns"]?.jsonArray?.size ?: 0) > 0)
            obj = JsonObject(root.filterKeys { it !in legacyKeys } + mapOf(
                "schemaVersion" to JsonPrimitive(2),
                "onboardingDone" to JsonPrimitive(existingUser)
            ))
        }
        return migrateV2ToV3(obj, version == 1)
    }

    private inline fun <reified T> JsonObject.opt(key: String): T? =
        this[key]?.takeIf { it !is JsonNull }?.let { runCatching { json.decodeFromJsonElement<T>(it) }.getOrNull() }

    /**
     * v2 -> v3: a single [peer] becomes a two-member group named [MIGRATED_GROUP_NAME] (visibility FULL_DETAILS to preserve existing behaviour; admin chosen by ensureSelf). Expenses, check-ins,
     * limits, the peer's snapshot, queued events and seen event ids are preserved (re-keyed by the peer's id).
     * This phone's own endpoint id is unknown here; the app calls `ensureSelf` once its iroh key is loaded,
     * which adds the self member and fills in the target of migrated incoming suggestions.
     */
    private fun migrateV2ToV3(root: JsonObject, fromV1: Boolean): BuddyState {
        val peerObj = root["peer"] as? JsonObject
        val peerId = (peerObj?.get("endpointId") as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
        val base = json.decodeFromJsonElement(
            BuddyState.serializer(),
            JsonObject(root.filterKeys { it !in v2PeerKeys } + ("schemaVersion" to JsonPrimitive(STATE_SCHEMA_VERSION)))
        )
        var migrated = base
        if (peerId != null) {
            val peer = root.opt<LegacyPeerLink>("peer")!!
            val friend = root.opt<FriendSnapshot>("friend")
            val now = System.currentTimeMillis()
            val peerName = peer.name.ifBlank { friend?.name.orEmpty() }.trim().take(MAX_DISPLAY_NAME)
            val outbox = (root.opt<List<PeerEvent>>("outbox") ?: emptyList())
                .map { it.copy(type = if (it.type == "unlink") "leave" else it.type, target = peerId) }
            val seen = (root.opt<List<String>>("seenEventIds") ?: emptyList()).takeLast(MAX_SEEN_EVENT_IDS)
            migrated = base.copy(
                // id, secret and admin are derived identically on both phones by ensureSelf (admin = smaller endpoint id).
                group = Group("", MIGRATED_GROUP_NAME, "", if (peer.pairedAt > 0) peer.pairedAt else now),
                // Existing behaviour: the two friends already saw each other's purchases, so keep sharing full details.
                visibility = Visibility.FULL_DETAILS,
                members = listOf(Member(peerId, peerName, peer.relayUrl, peer.directAddresses, peer.pairedAt)),
                friends = friend?.let { mapOf(peerId to it) } ?: emptyMap(),
                outbox = outbox,
                seenEventIds = if (seen.isEmpty()) emptyMap() else mapOf(peerId to seen),
                incomingNudge = root.opt<Nudge>("incomingNudge")?.copy(senderId = peerId),
                outgoingNudges = listOfNotNull(root.opt<OutgoingNudge>("outgoingNudge")?.copy(targetId = peerId)),
                // targetId is "" until ensureSelf supplies this phone's id.
                suggestions = (root.opt<List<BudgetSuggestion>>("suggestions") ?: emptyList()).map { it.copy(senderId = peerId) },
                sentSuggestions = listOfNotNull(root.opt<BudgetSuggestion>("sentSuggestion")?.copy(senderId = "", targetId = peerId))
            )
        }
        val last = migrated.expenses.maxByOrNull { it.updatedAt }?.category ?: migrated.lastCategory
        return migrated.copy(lastCategory = last, revision = maxOf(migrated.revision, 1))
    }

    @Serializable
    private data class LegacyPeerLink(
        val endpointId: String,
        val name: String = "",
        val pairedAt: Long = 0,
        val relayUrl: String? = null,
        val directAddresses: List<String> = emptyList()
    )

    /** Result of reading the state file; never silently substitutes an empty state for unreadable data. */
    sealed class LoadResult {
        data class Loaded(val state: BuddyState) : LoadResult()
        /** No file yet: a fresh install. */
        object Missing : LoadResult()
        /** The file exists but cannot be used (corrupt, or written by a newer app). Keep the original untouched. */
        data class Unreadable(val reason: String) : LoadResult()
    }

    fun load(text: String?): LoadResult {
        if (text == null) return LoadResult.Missing
        return try {
            LoadResult.Loaded(decode(text).normalizedLocalTimerPreferences())
        } catch (e: UnsupportedSchemaException) {
            LoadResult.Unreadable("This data was saved by a newer version of Penny Circle (schema ${e.version}). Update the app to open it.")
        } catch (e: Exception) {
            LoadResult.Unreadable("The saved data could not be read.")
        }
    }

    fun hasUserData(s: BuddyState): Boolean =
        s.name.isNotBlank() || s.expenses.isNotEmpty() || s.checkIns.isNotEmpty() || s.onboardingDone

    /**
     * Validates a decoded backup before it replaces [current]. Returns an issue or null.
     * Rejects an empty backup that would wipe existing data.
     */
    fun validateRestore(current: BuddyState, restored: BuddyState): BackupRestoreIssue? {
        if (restored.schemaVersion != STATE_SCHEMA_VERSION) return BackupRestoreIssue.UnsupportedVersion
        if (restored.paymentUpdateMinutes !in PAYMENT_UPDATE_MINUTES || restored.finishCountdownLeadMinutes !in FINISH_COUNTDOWN_LEAD_MINUTES)
            return BackupRestoreIssue.UnsupportedTimers
        if (restored.dailyLimitPaise < 0 || restored.weeklyLimitPaise < 0) return BackupRestoreIssue.InvalidLimits
        if (restored.expenses.any { it.amountPaise <= 0 }) return BackupRestoreIssue.InvalidPurchases
        if (restored.members.size > MAX_MEMBERS + MAX_TOMBSTONES) return BackupRestoreIssue.TooManyMembers
        if (hasUserData(current) && !hasUserData(restored)) return BackupRestoreIssue.EmptyBackup
        if (current.expenses.isNotEmpty() && restored.expenses.isEmpty() && restored.checkIns.isEmpty())
            return BackupRestoreIssue.NoPurchases
        return null
    }

    /** Backup files exclude in-progress screenshot drafts and the circle (keys stay on the phone). */
    fun encodeBackup(state: BuddyState): String = encode(
        state.copy(importImages = emptyList(), importCandidates = emptyList(), selfId = "", group = null, members = emptyList(),
            friends = emptyMap(), outbox = emptyList(), seenEventIds = emptyMap(), incomingNudge = null, outgoingNudges = emptyList(),
            suggestions = emptyList(), sentSuggestions = emptyList(), stayReachable = false, pendingJoins = emptyList(), declinedJoinIds = emptyList(),
            joinNonce = "", joinNonceExpiresAt = 0, lastNudgeFrom = emptyMap(), outSeq = 0, lastSeqSeen = emptyMap(),
            // Detection working data stays on the phone. Merchant rules are the user's own and are kept.
            pendingDetections = emptyList(), recentDetections = emptyList(), heldCheckIns = emptyMap(), autoDetect = false,
            expenses = state.expenses.map { if (it.heldUntil == 0L) it else it.copy(heldUntil = 0) })
    )

    @Suppress("unused")
    private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

    @Suppress("unused")
    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.content ?: this[key]?.jsonPrimitive?.content
}

/** Persisted local preferences are repaired to safe defaults; explicit backup restores are validated above. */
private fun BuddyState.normalizedLocalTimerPreferences(): BuddyState = copy(
    paymentUpdateMinutes = paymentUpdateMinutes.takeIf { it in PAYMENT_UPDATE_MINUTES } ?: DEFAULT_PAYMENT_UPDATE_MINUTES,
    finishCountdownLeadMinutes = finishCountdownLeadMinutes.takeIf { it in FINISH_COUNTDOWN_LEAD_MINUTES } ?: DEFAULT_FINISH_COUNTDOWN_LEAD_MINUTES
)
