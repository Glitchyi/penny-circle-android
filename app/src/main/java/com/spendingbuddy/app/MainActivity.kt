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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.spendingbuddy.app.ui.SpendingBuddyApp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repo by lazy { BuddyRepository.get(applicationContext) }
    private var handledShareKey: String? = null
    private var uiReady = false
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
                messages.tryEmit(if (saved) "Backup saved." else "Couldn't save the backup. Try another location.")
            }
        }
    }
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            lifecycleScope.launch {
                val input = runCatching { contentResolver.openInputStream(it) }.getOrNull()
                val problem = if (input == null) "Couldn't open that file." else repo.importBackup(input)
                messages.tryEmit(problem ?: "Backup restored.")
            }
        }
    }
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SpendingBuddyApp() }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleIntent(intent) }

    fun onUiReady() { uiReady = true; handleIntent(intent) }
    fun clearDestination() { destination.value = null }

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
        lifecycleScope.launch { repo.importUris(uris) }
    }

    fun pickMoreScreenshots() { pickImages.launch("image/*") }
    fun exportBackup() { createDocument.launch("spending-buddy-backup.json") }
    fun restoreBackup() { openDocument.launch(arrayOf("application/json", "text/*")) }
    fun requestNotifications() { if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notifications.launch(Manifest.permission.POST_NOTIFICATIONS) }
    fun openBatterySettings() { runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }.getOrElse { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) } }
    fun shareInvite(code: String) { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Join my Penny Circle. Open the app, go to the Circle tab, tap Join with a code, and paste this. The admin approves each request, and approval can take a little while: $code") }, "Share circle code")) }
    fun makeQr(text: String): Bitmap = qrBitmap(text, 720)
    fun scanInvite(onCode: (String) -> Unit) { pendingScan = onCode; GmsBarcodeScanning.getClient(this).startScan().addOnSuccessListener { it.rawValue?.let(onCode) }.addOnFailureListener { pendingScan = null; messages.tryEmit("Couldn't scan. Paste the code instead.") } }

    private fun qrBitmap(value: String, size: Int): Bitmap {
        val matrix: BitMatrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size)
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap -> for (x in 0 until size) for (y in 0 until size) bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE) }
    }
}
