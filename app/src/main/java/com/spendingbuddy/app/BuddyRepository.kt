package com.spendingbuddy.app

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Local-first state for this phone. There is no hosted backend: the friend's data arrives over
 * the iroh peer link (see p2p/PeerSyncManager) and is written back through [mutate].
 *
 * All instances in one process share a single state and serialization lock.
 */
class BuddyRepository private constructor(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val file = AtomicFile(File(app.filesDir, "buddy-state.json"))
    private val sharedListeners = CopyOnWriteArrayList<() -> Unit>()
    private val state0: MutableStateFlow<BuddyState>

    init {
        val restored = try {
            file.openRead().bufferedReader().use { StateCodec.decode(it.readText()) }
        } catch (_: java.io.FileNotFoundException) { null } catch (_: Exception) { null }
        state0 = MutableStateFlow((restored ?: BuddyState()).let { s ->
            s.copy(importImages = s.importImages.map {
                if (it.status == "processing") it.copy(status = "error", error = "Import was interrupted. Retry or remove this screenshot.") else it
            })
        })
    }

    val state: StateFlow<BuddyState> get() = state0

    /** Called after any change to data the friend can see (name, limits, expenses, check-ins). */
    fun addSharedChangeListener(listener: () -> Unit) { sharedListeners += listener }

    /**
     * Atomically transforms and persists state. Used by this class and by the peer sync manager.
     * Bumps [BuddyState.revision] when shared fields change so the friend can detect fresh data.
     */
    fun mutate(transform: (BuddyState) -> BuddyState) {
        var sharedChanged = false
        synchronized(lock) {
            val before = state0.value
            var next = transform(before)
            sharedChanged = sharedFields(before) != sharedFields(next)
            if (sharedChanged) next = next.copy(revision = maxOf(before.revision, next.revision) + 1)
            if (next == before) return
            val out = file.startWrite()
            try {
                out.write(StateCodec.encode(next).toByteArray(Charsets.UTF_8))
                file.finishWrite(out)
                state0.value = next
            } catch (e: Exception) { file.failWrite(out); throw e }
        }
        if (sharedChanged) sharedListeners.forEach { runCatching { it() } }
    }

    private fun sharedFields(s: BuddyState) = listOf(s.name, s.dailyLimitPaise, s.weeklyLimitPaise, s.expenses, s.checkIns)

    // ---- Profile, limits and preferences ----
    fun completeOnboarding(name: String, dailyPaise: Long, weeklyPaise: Long) {
        require(dailyPaise >= 0 && weeklyPaise >= 0)
        mutate { it.copy(name = name.trim().take(40), dailyLimitPaise = dailyPaise, weeklyLimitPaise = weeklyPaise, onboardingDone = true) }
    }
    fun updateName(name: String) = mutate { it.copy(name = name.trim().take(40)) }
    fun setBudgets(dailyPaise: Long, weeklyPaise: Long) {
        require(dailyPaise >= 0 && weeklyPaise >= 0)
        mutate { it.copy(dailyLimitPaise = dailyPaise, weeklyLimitPaise = weeklyPaise) }
    }
    fun setHaptics(enabled: Boolean) = mutate { it.copy(haptics = enabled) }
    fun setReminders(enabled: Boolean) {
        mutate { it.copy(reminders = enabled) }
        BuddyNotifications.schedule(app, enabled)
    }
    /** "keypad" or "dial". */
    fun setAmountInput(mode: String) { require(mode == "keypad" || mode == "dial"); mutate { it.copy(amountInput = mode) } }
    fun setFriendAlerts(enabled: Boolean) = mutate { it.copy(friendAlerts = enabled) }

    // ---- Presets (Hick's law: short lists; custom entries can be saved as presets) ----
    fun addPreset(value: String, isReason: Boolean) {
        val v = value.trim().take(24)
        if (v.isBlank()) return
        mutate { s ->
            if (isReason) s.copy(reasons = (s.reasons + v).distinctBy { it.lowercase() })
            else s.copy(categories = (s.categories + v).distinctBy { it.lowercase() })
        }
    }
    /** Built-in presets (including Impulse) cannot be removed; custom ones can. */
    fun removePreset(value: String, isReason: Boolean) {
        val builtIn = if (isReason) DEFAULT_REASONS else DEFAULT_CATEGORIES
        if (builtIn.any { it.equals(value, true) }) return
        mutate { s -> if (isReason) s.copy(reasons = s.reasons.filterNot { it == value }) else s.copy(categories = s.categories.filterNot { it == value }) }
    }

    // ---- Purchases ----
    /** Adds or replaces (same id) a purchase. Re-opens the check-in of any affected day. */
    fun saveExpense(expense: Expense) {
        require(expense.amountPaise > 0) { "Enter an amount above ₹0" }
        require(expense.category.isNotBlank() && expense.reason.isNotBlank()) { "Choose a category and a reason" }
        require(LocalDate.parse(expense.date) <= todayIndia()) { "Choose today or an earlier date" }
        require(expense.kind in listOf("expense", "refund", "transfer"))
        val clean = expense.copy(merchant = expense.merchant.trim().take(60), note = expense.note.trim().take(200), updatedAt = System.currentTimeMillis())
        mutate { current ->
            val previousDate = current.expenses.find { it.id == clean.id }?.date
            current.copy(
                expenses = current.expenses.filterNot { it.id == clean.id } + clean,
                checkIns = current.checkIns.filterNot { c -> c.date == clean.date || c.date == previousDate },
                lastCategory = clean.category
            )
        }
    }
    @Deprecated("Use saveExpense", ReplaceWith("saveExpense(expense)"))
    fun addExpense(expense: Expense) = saveExpense(expense)

    /** Returns the removed purchase so the UI can offer Undo. */
    fun deleteExpense(id: String): Expense? {
        val removed = state0.value.expenses.find { it.id == id } ?: return null
        mutate { s -> s.copy(expenses = s.expenses.filterNot { it.id == id }, checkIns = s.checkIns.filterNot { it.date == removed.date }) }
        return removed
    }
    fun restoreExpense(expense: Expense) = mutate { s -> s.copy(expenses = s.expenses.filterNot { it.id == expense.id } + expense, checkIns = s.checkIns.filterNot { it.date == expense.date }) }

    /** Recent merchants for one-tap suggestions, most recent first. */
    fun recentMerchants(limit: Int = 6): List<String> = state0.value.expenses.sortedByDescending { it.updatedAt }
        .map { it.merchant }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.take(limit)

    // ---- Daily check-in ----
    /** Returns an error message or null on success. */
    fun completeDay(date: String): String? {
        val d = try { LocalDate.parse(date) } catch (_: Exception) { return "Choose a valid date" }
        if (d > todayIndia()) return "You can't finish a day that hasn't happened yet"
        if (state0.value.checkIns.any { it.date == date }) return "This day is already finished"
        val now = java.time.ZonedDateTime.now(INDIA_ZONE)
        val withinDeadline = eligibleCheckIn(d, now)
        val record = CheckIn(date, now.toInstant().toEpochMilli(), d < now.toLocalDate() && withinDeadline, withinDeadline,
            state0.value.expenses.none { it.date == date && it.kind == "expense" })
        mutate { it.copy(checkIns = it.checkIns.filterNot { c -> c.date == date } + record) }
        return null
    }

    // ---- Screenshot import (multi-image draft) ----
    suspend fun importUris(uris: List<Uri>) = importMutex.withLock {
        val importer = ScreenshotImporter(app)
        for (uri in uris.take(20)) {
            if (state0.value.importImages.size >= 30) break
            val placeholder = ImportImage(path = "", status = "processing")
            mutate { it.copy(importImages = it.importImages + placeholder) }
            try {
                val (image, candidates) = withContext(Dispatchers.IO) { importer.importUri(uri) }
                synchronized(lock) {
                    if (state0.value.importImages.none { it.id == placeholder.id }) { importer.delete(image); continue }
                    mutate { s -> s.copy(importImages = s.importImages.map { if (it.id == placeholder.id) image else it }, importCandidates = markDuplicates(s.importCandidates + candidates, s.expenses)) }
                }
            } catch (e: Exception) {
                mutate { it.copy(importImages = it.importImages.map { image -> if (image.id == placeholder.id) image.copy(status = "error", error = e.message ?: "Couldn't read this screenshot") else image }) }
            }
        }
    }
    fun updateCandidate(candidate: ImportCandidate) = mutate {
        it.copy(importCandidates = markDuplicates(it.importCandidates.map { c -> if (c.id == candidate.id) candidate else c }, it.expenses))
    }
    fun deleteImportImage(id: String) {
        state0.value.importImages.find { it.id == id }?.let { ScreenshotImporter(app).delete(it) }
        mutate { it.copy(importImages = it.importImages.filterNot { image -> image.id == id }, importCandidates = markDuplicates(it.importCandidates.filterNot { c -> c.imageId == id }, it.expenses)) }
    }
    fun retryImportImage(id: String) {
        scope.launch {
            importMutex.withLock {
                val old = state0.value.importImages.find { it.id == id } ?: return@withLock
                if (old.path.isBlank()) return@withLock
                mutate { it.copy(importImages = it.importImages.map { i -> if (i.id == id) i.copy(status = "processing", error = "") else i }) }
                try {
                    val (image, candidates) = ScreenshotImporter(app).retry(old)
                    mutate { s ->
                        val existing = s.importCandidates.filter { it.imageId == id }
                        s.copy(importImages = s.importImages.map { if (it.id == id) image else it }, importCandidates = markDuplicates(s.importCandidates.filterNot { it.imageId == id } + if (existing.isNotEmpty()) existing else candidates, s.expenses))
                    }
                } catch (e: Exception) { mutate { it.copy(importImages = it.importImages.map { i -> if (i.id == id) i.copy(status = "error", error = e.message ?: "Retry failed") else i }) } }
            }
        }
    }
    fun discardImport() {
        val old = state0.value.importImages
        mutate { it.copy(importImages = emptyList(), importCandidates = emptyList()) }
        old.forEach { ScreenshotImporter(app).delete(it) }
    }
    /** Returns an error message, or null after saving every selected candidate. */
    fun saveImport(): String? {
        val s = state0.value
        if (s.importImages.any { it.status == "processing" }) return "Wait until every screenshot has been read"
        val selected = s.importCandidates.filter { it.selected }
        if (selected.isEmpty()) return "Select at least one purchase, or add it by hand"
        val expenses = mutableListOf<Expense>()
        for (c in selected) {
            val amount = amountToPaise(c.amountText) ?: return "Fix the amount for ${c.merchant.ifBlank { "a purchase" }}"
            if (amount <= 0) return "Amounts must be above ₹0"
            val date = try { LocalDate.parse(c.date) } catch (_: Exception) { return "Confirm the date for ${c.merchant.ifBlank { "each purchase" }}" }
            if (date > todayIndia()) return "Purchase dates can't be in the future"
            if (c.merchant.isBlank() || c.category.isBlank() || c.reason.isBlank()) return "Add a merchant, category and reason for each purchase"
            expenses += Expense(date = date.toString(), amountPaise = amount, merchant = c.merchant.trim(), category = c.category, reason = c.reason)
        }
        val changed = expenses.map { it.date }.toSet()
        mutate { it.copy(expenses = it.expenses + expenses, checkIns = it.checkIns.filterNot { c -> c.date in changed }, importImages = emptyList(), importCandidates = emptyList()) }
        s.importImages.forEach { ScreenshotImporter(app).delete(it) }
        return null
    }
    private fun markDuplicates(candidates: List<ImportCandidate>, expenses: List<Expense>): List<ImportCandidate> = candidates.map { c ->
        val amount = amountToPaise(c.amountText)
        val duplicate = amount != null && c.merchant.isNotBlank() && (
            candidates.any { other -> other.id != c.id && amountToPaise(other.amountText) == amount && other.merchant.equals(c.merchant, true) && other.date == c.date } ||
                expenses.any { it.amountPaise == amount && it.merchant.equals(c.merchant, true) && it.date == c.date }
            )
        c.copy(possibleDuplicate = duplicate)
    }

    // ---- Backup (no server: the user owns their data) ----
    suspend fun exportBackup(out: OutputStream) = withContext(Dispatchers.IO) {
        out.use { it.write(StateCodec.encodeBackup(state0.value).toByteArray(Charsets.UTF_8)) }
    }
    /** Replaces purchases, check-ins, limits and presets with the backup. Keeps the current peer link. Returns an error or null. */
    suspend fun importBackup(input: InputStream): String? = withContext(Dispatchers.IO) {
        val restored = try { input.use { StateCodec.decode(it.readBytes().toString(Charsets.UTF_8)) } } catch (_: Exception) { return@withContext "That file isn't a Spending Buddy backup" }
        mutate { s -> s.copy(name = restored.name.ifBlank { s.name }, expenses = restored.expenses, checkIns = restored.checkIns,
            dailyLimitPaise = restored.dailyLimitPaise, weeklyLimitPaise = restored.weeklyLimitPaise,
            categories = restored.categories, reasons = restored.reasons, onboardingDone = true) }
        null
    }

    companion object {
        private val lock = Any()
        private val importMutex = Mutex()
        @Volatile private var instance: BuddyRepository? = null
        fun get(context: Context): BuddyRepository = instance ?: synchronized(lock) { instance ?: BuddyRepository(context).also { instance = it } }
    }
}

/** Android Keystore–protected small secrets (the iroh endpoint secret key lives here). */
class SecretVault(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("buddy_secure", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun putBytes(name: String, value: ByteArray?) {
        if (value == null || value.isEmpty()) { prefs.edit().remove(name).apply(); return }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value)
        prefs.edit().putString(name, Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)).apply()
    }
    fun getBytes(name: String): ByteArray? = try {
        prefs.getString(name, null)?.let { stored ->
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.doFinal(bytes.copyOfRange(12, bytes.size))
        }
    } catch (_: Exception) { null }
    private companion object { const val ALIAS = "buddy-session" }
}
