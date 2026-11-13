package com.spendingbuddy.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.spendingbuddy.app.MAX_PRESETS
import com.spendingbuddy.app.MAX_PRESET_NAME
import com.spendingbuddy.app.R

/** Legacy domain validation stays compatible; app-owned feedback uses the Android catalog. */
internal fun Context.presetValidationMessage(message: String?): String? = when {
    message == null -> null
    message == "Give the preset a name" -> getString(R.string.validation_preset_name_required)
    message.startsWith("Keep the name to ") -> getString(R.string.validation_preset_name_length, MAX_PRESET_NAME)
    message == "Choose a category" -> getString(R.string.validation_category_required)
    message == "Choose a reason" -> getString(R.string.validation_reason_required)
    message.startsWith("You already have a preset called \"") -> getString(R.string.validation_preset_duplicate, message.substringAfter('"').substringBeforeLast('"'))
    message.startsWith("You can keep up to ") -> getString(R.string.validation_preset_capacity, MAX_PRESETS)
    message == "That preset no longer exists" -> getString(R.string.validation_preset_missing)
    else -> getString(R.string.validation_preset_save_failed)
}

@Composable
internal fun presetValidationMessage(message: String?): String? = LocalContext.current.presetValidationMessage(message)

internal fun Context.budgetValidationMessage(message: String?): String? = when (message) {
    null -> null
    "Enter a valid daily amount, with up to two decimal places." -> getString(R.string.validation_daily_amount)
    "Enter a valid weekly amount, with up to two decimal places." -> getString(R.string.validation_weekly_amount)
    else -> message
}
