package com.spendingbuddy.app.ui

import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalView

/**
 * Material 3 1.3.2 owns a native dialog-window Back callback, separate from the Activity dispatcher.
 * A multi-step modal handles that same window's Back without asking Material to hide it first.
 * Default priority lets the IME consume Back before the form changes steps. Android 13 is our minimum.
 */
@Composable
internal fun ModalStepBackHandler(onBack: () -> Unit) {
    val view = LocalView.current
    val currentBack by rememberUpdatedState(onBack)
    DisposableEffect(view) {
        val dispatcher = view.findOnBackInvokedDispatcher()
        val callback = OnBackInvokedCallback { currentBack() }
        dispatcher?.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
        onDispose { dispatcher?.unregisterOnBackInvokedCallback(callback) }
    }
}
