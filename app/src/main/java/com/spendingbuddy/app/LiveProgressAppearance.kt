package com.spendingbuddy.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import androidx.core.graphics.PathParser
import androidx.compose.ui.graphics.toArgb
import com.spendingbuddy.app.ui.AppTheme
import com.spendingbuddy.app.ui.tokens

/** Use the same saved theme tokens as Compose, including readable text on each accent. */
internal data class LiveProgressAppearance(val accent: Int, val onAccent: Int, val danger: Int)

internal fun liveProgressAppearance(theme: String): LiveProgressAppearance =
    AppTheme.fromId(theme).tokens().let { LiveProgressAppearance(it.accent.toArgb(), it.onAccent.toArgb(), it.danger.toArgb()) }

internal fun rupeeProgressTrackerIcon(appearance: LiveProgressAppearance): Icon =
    Icon.createWithBitmap(rupeeTrackerBitmap(appearance.accent, appearance.onAccent))

/** Original vector geometry, rasterized locally for Android's full-color tracker and its UI preview. */
internal fun rupeeTrackerBitmap(accent: Int, onAccent: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(3f, 3f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = accent
    canvas.drawCircle(16f, 16f, 15f, paint)
    paint.color = onAccent
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    canvas.drawCircle(16f, 16f, 14f, paint)
    paint.style = Paint.Style.FILL
    val rupee = requireNotNull(PathParser.createPathFromPathData(
        "M10,7.5H23V9.8H19.1Q20.8,11.1 21.2,13H23V15.2H21.1Q20.1,19.2 15.6,19.8L22.2,25H18.5L10.8,18.8V17.4H13.4Q17.6,17.4 18.4,15.2H10V13H18.4Q17.7,9.8 13.4,9.8H10Z"
    ))
    canvas.drawPath(rupee, paint)
    return bitmap
}
