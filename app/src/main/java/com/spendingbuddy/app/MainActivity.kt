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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repo by lazy { BuddyRepository.get(applicationContext) }
    private var handledShareKey: String? = null
    private var uiReady = false
    private var pendingDestination: String? = null
    private var pendingScan: ((String) -> Unit)? = null
    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> if (uris.isNotEmpty()) importAndOpen(uris) }
    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { lifecycleScope.launch { contentResolver.openOutputStream(it)?.let { out -> repo.exportBackup(out) } } } }
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { lifecycleScope.launch { contentResolver.openInputStream(it)?.let { input -> repo.importBackup(input) } } } }
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SpendingBuddyApp() }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleIntent(intent) }

    fun onUiReady() { uiReady = true; handleIntent(intent) }
    fun consumeDestination(): String? = pendingDestination.also { pendingDestination = null }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        intent.getStringExtra(Destinations.EXTRA)?.let { pendingDestination = it }
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
        pendingDestination = Destinations.IMPORT
        lifecycleScope.launch { repo.importUris(uris) }
    }

    fun pickMoreScreenshots() { pickImages.launch("image/*") }
    fun exportBackup() { createDocument.launch("spending-buddy-backup.json") }
    fun restoreBackup() { openDocument.launch(arrayOf("application/json", "text/*")) }
    fun requestNotifications() { if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notifications.launch(Manifest.permission.POST_NOTIFICATIONS) }
    fun openBatterySettings() { runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }.getOrElse { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) } }
    fun shareInvite(code: String) { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "Join me on Spending Buddy. Open the app, go to Buddy, tap Join with a code, and paste this: $code") }, "Share invite")) }
    fun makeQr(text: String): Bitmap = qrBitmap(text, 720)
    fun scanInvite(onCode: (String) -> Unit) { pendingScan = onCode; GmsBarcodeScanning.getClient(this).startScan().addOnSuccessListener { it.rawValue?.let(onCode) }.addOnFailureListener { pendingScan = null } }

    private fun qrBitmap(value: String, size: Int): Bitmap {
        val matrix: BitMatrix = MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, size, size)
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap -> for (x in 0 until size) for (y in 0 until size) bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE) }
    }
}
