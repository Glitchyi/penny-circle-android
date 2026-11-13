package com.spendingbuddy.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import com.spendingbuddy.app.ui.AppTheme
import com.spendingbuddy.app.ui.tokens
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.spendingbuddy.app.ui.SpendingBuddyApp
import com.spendingbuddy.app.ui.SpendingBuddyTheme
import com.spendingbuddy.app.ui.StartupSurface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart

class MainActivity : ComponentActivity() {
    private val repo by lazy { BuddyRepository.get(applicationContext) }
    private var handledShareKey: String? = null
    private var uiReady = false
    private val restoreJobLock = Any()
    private var backupRestoreJob: Job? = null
    /** Destination requested by an intent (notification tap, share). The UI navigates to it and then clears it. */
    val destination = MutableStateFlow<String?>(null)
    /** One-off user-facing results (backup saved/restored, failures) shown as snackbars by the UI. */
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    private var pendingScan: ((String) -> Unit)? = null
    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> if (uris.isNotEmpty()) importAndOpen(uris) }
    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val saved = runCatching { contentResolver.openOutputStream(it)?.let { out -> repo.exportBackup(out) } != null }.getOrDefault(false)
                messages.tryEmit(getString(if (saved) R.string.backup_saved else R.string.backup_save_error))
            }
        }
    }
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val operation = repo.beginBackupRestore() ?: return@let
            val job = lifecycleScope.launch(start = CoroutineStart.LAZY) {
                val problem = repo.importBackup(operation) { contentResolver.openInputStream(it) }
                messages.tryEmit(problem ?: getString(R.string.backup_restored))
            }
            synchronized(restoreJobLock) { backupRestoreJob = job }
            // Also releases a reservation if lifecycle cancellation happens before the coroutine starts.
            job.invokeOnCompletion {
                synchronized(restoreJobLock) { if (backupRestoreJob === job) backupRestoreJob = null }
                repo.finishRecovery(operation)
            }
            job.start()
        }
    }
    private var notificationsResult: (() -> Unit)? = null
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsResult?.invoke(); notificationsResult = null
    }
    private var smsResult: ((Boolean) -> Unit)? = null
    private val sms = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> smsResult?.invoke(granted); smsResult = null }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handledShareKey = savedInstanceState?.getString("handled_share_key")
        // Apply the saved theme before the first frame: window background and system bar icon style.
        val savedTheme = AppTheme.fromId(ThemeStore.load(this))
        val bg = savedTheme.tokens().bg.toArgb()
        val bars = if (savedTheme.isDark) SystemBarStyle.dark(bg) else SystemBarStyle.light(bg, bg)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bg))
        setContent {
            val app = application as BuddyApplication
            val startup by app.startupState.collectAsState()
            if (startup == StartupPhase.READY) SpendingBuddyApp()
            else SpendingBuddyTheme(savedTheme) {
                StartupSurface(failed = startup == StartupPhase.FAILED, onRetry = app::retryStartup)
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleIntent(intent) }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("handled_share_key", handledShareKey)
        super.onSaveInstanceState(outState)
    }

    fun onUiReady() { uiReady = true; handleIntent(intent) }
    fun clearDestination() {
        destination.value = null
        // A consumed notification must not reopen its route after a theme/font recreation.
        intent?.removeExtra(Destinations.EXTRA)
    }

    /** Makes the next cold start's system splash screen use the colours of the chosen theme (API 33+). */
    fun applySplashTheme(themeId: String) {
        val style = when (AppTheme.fromId(themeId)) {
            AppTheme.LightHorizon -> R.style.Theme_Splash_LightHorizon
            AppTheme.VioletNight -> R.style.Theme_Splash_VioletNight
            AppTheme.MintNight -> R.style.Theme_Splash_MintNight
            AppTheme.SignalBlue -> R.style.Theme_Splash_SignalBlue
        }
        runCatching { splashScreen.setSplashScreenTheme(style) }
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        intent.getStringExtra(Destinations.EXTRA)?.let { destination.value = it }
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
            else -> emptyList()
        }
        if (uris.isEmpty()) return
        val key = intent.action + uris.joinToString("|")
        if (key == handledShareKey) return
        handledShareKey = key
        importAndOpen(uris)
    }

    private fun importAndOpen(uris: List<Uri>) {
        destination.value = Destinations.IMPORT
        lifecycleScope.launch {
            if ((application as BuddyApplication).awaitReady()) repo.importUris(uris)
            else handledShareKey = null // A retry can handle the original share once startup succeeds.
        }
    }

    fun pickMoreScreenshots() { pickImages.launch("image/*") }
    fun exportBackup() { createDocument.launch("Penny-Circle-backup.json") }
    fun restoreBackup() { if (repo.recoveryOperation.value == null) openDocument.launch(arrayOf("application/json", "text/*")) }
    fun closeForRecovery() {
        synchronized(restoreJobLock) { backupRestoreJob }?.cancel()
        finish()
    }
    fun requestNotifications(onResult: () -> Unit = {}) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationsResult = onResult
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else onResult()
    }
    /** Asks for permission to receive SMS as they arrive (used only to parse bank alerts on this phone). */
    fun requestSms(onResult: (Boolean) -> Unit) { smsResult = onResult; sms.launch(Manifest.permission.RECEIVE_SMS) }
    /** Opens the system page where the user can let this app read notifications. */
    fun openNotificationAccess() { runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }.getOrElse { openAppSettings() } }
    /** Opens the system page for this app's Live Updates switch (Android 16), or the app's notification settings. */
    fun openLiveUpdateSettings() {
        val pkg = Uri.parse("package:$packageName")
        runCatching { startActivity(Intent("android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS").putExtra(Settings.EXTRA_APP_PACKAGE, packageName)) }
            .recoverCatching { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)) }
            .getOrElse { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)) }
    }
    fun openAppSettings() { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
    fun openBatterySettings() { runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }.getOrElse { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) } }
    fun shareInvite(code: String) { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, getString(R.string.share_circle_invite, code)) }, getString(R.string.share_circle_title))) }
    fun makeQr(text: String): Bitmap = qrBitmap(text, 720)
    fun scanInvite(onCode: (String) -> Unit) { pendingScan = onCode; GmsBarcodeScanning.getClient(this).startScan().addOnSuccessListener { it.rawValue?.let(onCode) }.addOnFailureListener { pendingScan = null; messages.tryEmit(getString(R.string.share_scan_error)) } }

    private fun qrBitmap(value: String, size: Int): Bitmap {
        val matrix: BitMatrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size)
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap -> for (x in 0 until size) for (y in 0 until size) bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE) }
    }
}
