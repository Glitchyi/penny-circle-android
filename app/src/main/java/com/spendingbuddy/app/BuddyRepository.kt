package com.spendingbuddy.app

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
 * Local-first state for this phone. There is no hosted backend: other circle members' data arrives over
 * direct iroh connections (see p2p/PeerSyncManager) and is written back through [mutate].
 *
 * All instances in one process share a single state and serialization lock.
 */
/** Valid values for [BuddyState.theme]. Keep in sync with the app theme list in ui/Theme.kt. */
internal val THEME_IDS = listOf("light_horizon", "violet_night", "mint_night", "signal_blue")

class BuddyRepository private constructor(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val file = AtomicFile(File(app.filesDir, "buddy-state.json"))
    private val sharedListeners = CopyOnWriteArrayList<() -> Unit>()
    private val state0: MutableStateFlow<BuddyState>
    @Volatile private var writeBlocked = false
    private var recoveryBackupAvailable = false
    private val recoveryGate = RecoveryOperationGate()
    internal val recoveryOperation: StateFlow<RecoveryOperation?> get() = recoveryGate.operation
    private val loadProblem0 = MutableStateFlow<String?>(null)

    init {
        var originalBytes: ByteArray? = null
        val text = try {
            file.openRead().use { it.readBytes().also { bytes -> originalBytes = bytes }.toString(Charsets.UTF_8) }
        } catch (_: java.io.FileNotFoundException) {
            if (stateStorageIsMissing(file.baseFile)) null else ""
        } catch (_: Exception) { "" }
        val result = StateCodec.load(text)
        var initial = BuddyState()
        if (result is StateCodec.LoadResult.Loaded) initial = result.state
        if (result is StateCodec.LoadResult.Unreadable) {
            // Keep the original bytes recoverable and never overwrite them until the user acts.
            recoveryBackupAvailable = originalBytes?.let { bytes ->
                runCatching { File(app.filesDir, "buddy-state-${System.currentTimeMillis()}.corrupt").writeBytes(bytes) }.isSuccess
            } == true
            writeBlocked = true
            loadProblem0.value = app.getString(
                if (result.reason.startsWith("This data was saved by a newer version of Penny Circle")) R.string.backup_local_newer_version
                else R.string.backup_local_unreadable
            )
        }
        state0 = MutableStateFlow(initial.copy(importImages = recoverInterruptedImports(initial.importImages)))
    }

    /** Set when the saved file could not be loaded. Its bytes stay protected until explicit recovery succeeds. */
    val loadProblem: StateFlow<String?> get() = loadProblem0
    /** Explicitly confirmed fresh start. A protected copy is required before replacing the unreadable file. */
    internal fun beginBackupRestore(): RecoveryOperation? = recoveryGate.begin(RecoveryKind.Restore)
    internal fun beginFreshStart(): RecoveryOperation? = recoveryGate.begin(RecoveryKind.FreshStart)
    internal fun finishRecovery(operation: RecoveryOperation) = recoveryGate.finish(operation)

    internal suspend fun startFreshAfterLoadProblem(operation: RecoveryOperation) = try { withContext(Dispatchers.IO) {
            val operationContext = currentCoroutineContext()
            recoveryGate.requireCurrent(operation)
            check(operation.kind == RecoveryKind.FreshStart)
            mutateState(allowRecoveryWrite = true, beforeWrite = { operationContext.ensureActive() }) { current ->
                if (writeBlocked) BuddyState(theme = current.theme) else current
            }
    } } finally { finishRecovery(operation) }

    val state: StateFlow<BuddyState> get() = state0

    /** Called after any change to data the circle can see (name, limits, expenses, check-ins). */
    fun addSharedChangeListener(listener: () -> Unit) { sharedListeners += listener }

    /**
     * Atomically transforms and persists state. Used by this class and by the peer sync manager.
     * Bumps [BuddyState.revision] when shared fields change so members can detect fresh data.
     */
    fun mutate(transform: (BuddyState) -> BuddyState) = mutateState(allowRecoveryWrite = false, transform = transform)

    private fun ensureRecoveryCopy() {
        if (!writeBlocked || recoveryBackupAvailable) return
        // Preserve sidecars too when AtomicFile could not open an authoritative base file.
        stateStorageArtifacts(file.baseFile).filterNot { java.nio.file.Files.notExists(it.toPath()) }
            .forEachIndexed { index, artifact ->
                val original = artifact.readBytes()
                File(app.filesDir, "buddy-state-${System.currentTimeMillis()}-$index.corrupt").writeBytes(original)
            }
        recoveryBackupAvailable = true
    }

    private fun mutateState(allowRecoveryWrite: Boolean, beforeWrite: () -> Unit = {}, transform: (BuddyState) -> BuddyState) {
        var sharedChanged = false
        var finishCountdownRefreshNeeded = false
        var detectedBudgetRefreshNeeded = false
        synchronized(lock) {
            val before = state0.value
            var next = transform(before)
            sharedChanged = sharedFields(before) != sharedFields(next)
            if (sharedChanged) next = next.copy(revision = maxOf(before.revision, next.revision) + 1)
            if (next == before && !allowRecoveryWrite) return@synchronized
            if (writeBlocked && !allowRecoveryWrite) { state0.value = next; return@synchronized }
            finishCountdownRefreshNeeded = before.reminders != next.reminders ||
                before.finishCountdown != next.finishCountdown || before.checkIns != next.checkIns ||
                before.onboardingDone != next.onboardingDone || before.finishCountdownLeadMinutes != next.finishCountdownLeadMinutes || before.theme != next.theme
            detectedBudgetRefreshNeeded = before.expenses != next.expenses || before.dailyLimitPaise != next.dailyLimitPaise || before.theme != next.theme
            if (allowRecoveryWrite) ensureRecoveryCopy()
            beforeWrite()
            val out = file.startWrite()
            try {
                out.write(StateCodec.encode(next).toByteArray(Charsets.UTF_8))
                file.finishWrite(out)
                state0.value = next
                if (allowRecoveryWrite) { writeBlocked = false; loadProblem0.value = null }
            } catch (e: Exception) { file.failWrite(out); throw e }
        }
        if (finishCountdownRefreshNeeded) runCatching { FinishCountdownNotifications.refresh(app, state0.value) }
        if (detectedBudgetRefreshNeeded) runCatching { com.spendingbuddy.app.detect.DetectionNotifications.refreshAdded(app, state0.value) }
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
        BuddyNotifications.schedule(app, enabled, state0.value.finishCountdownLeadMinutes)
    }
    /** Local opt-in: show a countdown before the 10 PM reminder using the selected lead time. */
    fun setFinishCountdown(enabled: Boolean) = mutate { it.copy(finishCountdown = enabled) }
    fun setPaymentUpdateMinutes(minutes: Int) {
        require(minutes in PAYMENT_UPDATE_MINUTES)
        mutate { it.copy(paymentUpdateMinutes = minutes) }
    }
    fun setFinishCountdownLeadMinutes(minutes: Int) {
        require(minutes in FINISH_COUNTDOWN_LEAD_MINUTES)
        mutate { it.copy(finishCountdownLeadMinutes = minutes) }
        if (state0.value.reminders) BuddyNotifications.reschedulePreReminder(app, minutes)
    }
    /** "keypad" or "dial". */
    fun setAmountInput(mode: String) { require(mode == "keypad" || mode == "dial"); mutate { it.copy(amountInput = mode) } }
    fun setTheme(id: String) { require(id in THEME_IDS); mutate { it.copy(theme = id) } }
    fun setFriendAlerts(enabled: Boolean) = mutate { it.copy(friendAlerts = enabled) }

    // ---- Circle ----
    /** Changes what this phone shares from now on (see com.spendingbuddy.app.p2p.setVisibility); the circle sees it on the next sync. Prefer PeerSyncManager.setVisibility, which also syncs. */
    fun setVisibility(visibility: Visibility) = mutate { com.spendingbuddy.app.p2p.setVisibility(it, visibility) }
    /** Display name for a member id, falling back to the last shared name and then "A member". */
    fun memberName(id: String): String = state0.value.let { s -> s.members.firstOrNull { it.endpointId == id }?.displayName?.ifBlank { null } ?: s.friends[id]?.name?.ifBlank { null } ?: "A member" }

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

    // ---- Personal presets (never synced; see core Presets.kt for the pure rules) ----
    private fun presetMutate(op: (BuddyState) -> PresetOutcome): PresetOutcome {
        var outcome: PresetOutcome = PresetOutcome.Invalid("Couldn't save that preset")
        mutate { s -> op(s).also { outcome = it }.let { (it as? PresetOutcome.Ok)?.state ?: s } }
        return outcome
    }
    /** Returns an error message, or null when saved. */
    fun savePreset(name: String, category: String, reason: String, makeDefault: Boolean = false): String? =
        (presetMutate { addPresetTo(it, name, category, reason, makeDefault) } as? PresetOutcome.Invalid)?.message
    fun updatePreset(id: String, name: String, category: String, reason: String): String? =
        (presetMutate { updatePresetIn(it, id, name, category, reason) } as? PresetOutcome.Invalid)?.message
    fun renamePreset(id: String, name: String): String? {
        val p = state0.value.presets.firstOrNull { it.id == id } ?: return "That preset no longer exists"
        return updatePreset(id, name, p.category, p.reason)
    }
    fun setDefaultPreset(id: String?) = mutate { setDefaultPresetIn(it, id) }
    /** Returns the removed preset so the caller can offer Undo via [restorePreset]. */
    fun deletePreset(id: String): Preset? {
        var removed: Preset? = null
        mutate { s -> deletePresetFrom(s, id).also { removed = it.second }.first }
        return removed
    }
    fun restorePreset(preset: Preset) = mutate { restorePresetTo(it, preset) }

    // ---- Purchases ----
    /** Adds or replaces (same id) a purchase. Re-opens the check-in of any affected day. */
    fun saveExpense(expense: Expense) {
        require(expense.amountPaise > 0) { "Enter an amount above ₹0" }
        require(expense.category.isNotBlank() && expense.reason.isNotBlank()) { "Choose a category and a reason" }
        require(LocalDate.parse(expense.date) <= todayIndia()) { "Choose today or an earlier date" }
        require(expense.kind in listOf("expense", "refund", "transfer"))
        // Saving from the form means the user has looked at it: the category is settled.
        val clean = expense.copy(merchant = expense.merchant.trim().take(60), note = expense.note.trim().take(200), updatedAt = System.currentTimeMillis(), needsCategory = false)
        mutate { current ->
            val previous = current.expenses.find { it.id == clean.id }
            val next = current.copy(
                expenses = current.expenses.filterNot { it.id == clean.id } + clean,
                checkIns = current.checkIns.filterNot { c -> c.date == clean.date || c.date == previous?.date },
                heldCheckIns = current.heldCheckIns.filterValues { it.date != clean.date && it.date != previous?.date },
                lastCategory = clean.category
            )
            // Correcting an auto-added purchase teaches the app what that shop is.
            if (previous?.source == SOURCE_AUTO && (previous.category != clean.category || previous.reason != clean.reason || previous.needsCategory))
                upsertMerchantRule(next, clean.merchant, clean.category, clean.reason) else next
        }
    }

    // ---- Automatic tracking from payment messages (all on this phone; see core Detection.kt) ----
    fun setAutoDetect(enabled: Boolean) = mutate { it.copy(autoDetect = enabled, autoDetectOffered = true) }
    fun dismissAutoDetectOffer() = mutate { it.copy(autoDetectOffered = true) }
    /** Applies one parsed payment and returns what happened so the caller can show the right notification. */
    fun recordDetection(payment: ParsedPayment, now: Long = System.currentTimeMillis()): DetectionOutcome {
        var outcome: DetectionOutcome = DetectionOutcome.Ignored
        mutate { s ->
            when (val result = applyDetection(s, payment, now).also { outcome = it }) {
                is DetectionOutcome.Added -> result.state
                is DetectionOutcome.Asked -> result.state
                DetectionOutcome.Ignored -> s
            }
        }
        return outcome
    }
    fun undoAuto(id: String) = mutate { undoAutoExpense(it, id) }
    /** Ends the undo window now, so the circle can see the purchase. */
    fun keepAuto(id: String) = mutate { keepAutoExpense(it, id) }
    fun releaseHolds() = mutate { releaseExpiredHolds(it) }
    fun addPending(id: String): Expense? {
        var added: Expense? = null
        mutate { s -> addPendingDetection(s, id).also { added = it.second }.first }
        return added
    }
    fun skipPending(id: String) = mutate { skipPendingDetection(it, id) }
    fun saveMerchantRule(merchant: String, category: String, reason: String) = mutate { upsertMerchantRule(it, merchant, category, reason) }
    fun removeMerchantRule(key: String) = mutate { deleteMerchantRule(it, key) }
    /** Marks a purchase as impulse, or back to [otherReason]. */
    fun setImpulse(id: String, impulse: Boolean, otherReason: String = "Essential") = mutate { s ->
        s.copy(expenses = s.expenses.map { if (it.id == id) it.copy(reason = if (impulse) IMPULSE else otherReason, updatedAt = System.currentTimeMillis()) else it })
    }
    @Deprecated("Use saveExpense", ReplaceWith("saveExpense(expense)"))
    fun addExpense(expense: Expense) = saveExpense(expense)

    /** Returns the removed purchase so the UI can offer Undo. */
    fun deleteExpense(id: String): Expense? {
        val removed = state0.value.expenses.find { it.id == id } ?: return null
        mutate { s -> s.copy(expenses = s.expenses.filterNot { it.id == id }, checkIns = s.checkIns.filterNot { it.date == removed.date }) }
        return removed
    }
    /** Reverts a just-saved new purchase and puts back the check-ins that saving it re-opened. */
    fun undoSave(expenseId: String, previousCheckIns: List<CheckIn>) = mutate { s ->
        s.copy(
            expenses = s.expenses.filterNot { it.id == expenseId },
            checkIns = s.checkIns.filterNot { c -> previousCheckIns.any { it.date == c.date } } + previousCheckIns
        )
    }
    /** Puts back a just-deleted purchase together with the check-ins its deletion re-opened. */
    fun undoDelete(expense: Expense, previousCheckIns: List<CheckIn>) = mutate { s ->
        s.copy(
            expenses = s.expenses.filterNot { it.id == expense.id } + expense,
            checkIns = s.checkIns.filterNot { c -> previousCheckIns.any { it.date == c.date } } + previousCheckIns
        )
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
        mutate { it.copy(checkIns = it.checkIns.filterNot { c -> c.date == date } + record,
            heldCheckIns = it.heldCheckIns.filterValues { c -> c.date != date }) }
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                mutate { it.copy(importImages = it.importImages.map { image -> if (image.id == placeholder.id) image.copy(status = "failed", error = e.message ?: app.getString(R.string.import_read_error)) else image }) }
            }
        }
    }
    fun updateCandidate(candidate: ImportCandidate) = mutate {
        it.copy(importCandidates = markDuplicates(it.importCandidates.map { c -> if (c.id == candidate.id) candidate else c }, it.expenses))
    }
    fun deleteImportImage(id: String) {
        val removedImage = state0.value.importImages.find { it.id == id }
        mutate { it.copy(importImages = it.importImages.filterNot { image -> image.id == id }, importCandidates = markDuplicates(it.importCandidates.filterNot { c -> c.imageId == id }, it.expenses)) }
        removedImage?.let { ScreenshotImporter(app).delete(it) }
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
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (e: Exception) { mutate { it.copy(importImages = it.importImages.map { i -> if (i.id == id) i.copy(status = "failed", error = e.message ?: app.getString(R.string.import_retry_error)) else i }) } }
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
        if (s.importImages.any { it.status == "processing" }) return app.getString(R.string.import_wait_reading)
        val today = todayIndia()
        app.importReviewMessage(s.importCandidates, today)?.let { return it }
        val expenses = requireNotNull(reviewedImportExpenses(s.importCandidates, today))
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
    /** Replaces purchases, check-ins, limits and presets with the backup. Keeps the current circle membership. Returns an error or null. */
    internal suspend fun importBackup(operation: RecoveryOperation, openInput: () -> InputStream?): String? = try { withContext(Dispatchers.IO) {
        recoveryGate.requireCurrent(operation)
        check(operation.kind == RecoveryKind.Restore)
        val restored = try {
            // Providers may block on both open and read. Neither runs on the activity thread.
            val input = openInput() ?: return@withContext app.getString(R.string.backup_open_error)
            input.use {
                currentCoroutineContext().ensureActive()
                val contents = it.readBytes()
                currentCoroutineContext().ensureActive()
                StateCodec.decode(contents.toString(Charsets.UTF_8))
            }
        }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: UnsupportedSchemaException) { return@withContext app.getString(R.string.backup_newer_version) }
        catch (_: java.io.IOException) { return@withContext app.getString(R.string.backup_open_error) }
        catch (_: Exception) { return@withContext app.getString(R.string.backup_invalid_file) }
        fun validationMessage(issue: BackupRestoreIssue): String = app.getString(when (issue) {
                BackupRestoreIssue.UnsupportedVersion -> R.string.backup_unsupported_version
                BackupRestoreIssue.UnsupportedTimers -> R.string.backup_unsupported_timers
                BackupRestoreIssue.InvalidLimits -> R.string.backup_invalid_limits
                BackupRestoreIssue.InvalidPurchases -> R.string.backup_invalid_purchases
                BackupRestoreIssue.TooManyMembers -> R.string.backup_too_many_members
                BackupRestoreIssue.EmptyBackup -> R.string.backup_empty
                BackupRestoreIssue.NoPurchases -> R.string.backup_no_purchases
            })
        currentCoroutineContext().ensureActive()
        recoveryGate.requireCurrent(operation)
        val operationContext = currentCoroutineContext()
        try { mutateState(allowRecoveryWrite = true, beforeWrite = { operationContext.ensureActive() }) { s ->
            StateCodec.validateRestore(s, restored)?.let { throw BackupRestoreRejected(it) }
            s.copy(name = restored.name.ifBlank { s.name }, expenses = restored.expenses, checkIns = restored.checkIns,
            dailyLimitPaise = restored.dailyLimitPaise, weeklyLimitPaise = restored.weeklyLimitPaise,
            categories = restored.categories, reasons = restored.reasons, presets = restored.presets.take(MAX_PRESETS),
            merchantRules = restored.merchantRules.take(MAX_MERCHANT_RULES), heldCheckIns = emptyMap(), onboardingDone = true) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (rejected: BackupRestoreRejected) { return@withContext validationMessage(rejected.issue) }
        catch (_: Exception) { return@withContext app.getString(R.string.backup_restore_error) }
        null
    } } finally { finishRecovery(operation) }

    companion object {
        private val lock = Any()
        private val importMutex = Mutex()
        @Volatile private var instance: BuddyRepository? = null
        fun get(context: Context): BuddyRepository = instance ?: synchronized(lock) { instance ?: BuddyRepository(context).also { instance = it } }
    }
}

private class BackupRestoreRejected(val issue: BackupRestoreIssue) : Exception()

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
