package com.spendingbuddy.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private const val STROKE_WIDTH = 1.75f
private const val DOT_STROKE_WIDTH = 3f

private fun penny(name: String, vararg paths: String, dotPaths: Set<String> = emptySet()): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    for (d in paths) {
        builder.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = if (d in dotPaths) DOT_STROKE_WIDTH else STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null
        )
    }
    return builder.build()
}

object PennyIcons {
    val Food: ImageVector by lazy {
        penny(
            "PennyFood",
            "M6 3v6",
            "M10 3v6",
            "M6 9c0 2 1 3 2 3s2-1 2-3",
            "M8 12v9",
            "M17 3c-2.2 1-3.5 3.6-3.5 7 0 2 1.3 3 3.5 3",
            "M17 3v18"
        )
    }

    val Transport: ImageVector by lazy {
        penny(
            "PennyTransport",
            "M5 4h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
            "M3 11h18",
            "M7 18v2",
            "M17 18v2",
            "M7.5 15h.01",
            "M16.5 15h.01",
            dotPaths = setOf("M7.5 15h.01", "M16.5 15h.01")
        )
    }

    val Shopping: ImageVector by lazy {
        penny(
            "PennyShopping",
            "M6 8h12l1 12H5L6 8z",
            "M9 8V6a3 3 0 0 1 6 0v2"
        )
    }

    val Bills: ImageVector by lazy {
        penny(
            "PennyBills",
            "M6 3h12v18l-3-2-3 2-3-2-3 2V3z",
            "M9 8h6",
            "M9 12h6"
        )
    }

    val Other: ImageVector by lazy {
        penny(
            "PennyOther",
            "M5 12h.01",
            "M12 12h.01",
            "M19 12h.01",
            dotPaths = setOf("M5 12h.01", "M12 12h.01", "M19 12h.01")
        )
    }

    val Today: ImageVector by lazy {
        penny(
            "PennyToday",
            "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z",
            "M3 10h18",
            "M8 3v4",
            "M16 3v4"
        )
    }

    val Activity: ImageVector by lazy {
        penny(
            "PennyActivity",
            "M6 20v-9",
            "M12 20V5",
            "M18 20v-7"
        )
    }

    val Circle: ImageVector by lazy {
        penny(
            "PennyCircle",
            "M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6z",
            "M3 20v-1a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v1",
            "M17 5.2a3 3 0 0 1 0 5.6",
            "M21 20v-1a4 4 0 0 0-3-3.9"
        )
    }

    val Add: ImageVector by lazy {
        penny(
            "PennyAdd",
            "M12 5v14",
            "M5 12h14"
        )
    }

    val ArrowRight: ImageVector by lazy {
        penny(
            "PennyArrowRight",
            "M5 12h14",
            "M13 6l6 6-6 6"
        )
    }
}

fun categoryIconVector(name: String): ImageVector =
    when (name.lowercase()) {
        "food" -> PennyIcons.Food
        "transport" -> PennyIcons.Transport
        "shopping" -> PennyIcons.Shopping
        "bills" -> PennyIcons.Bills
        else -> PennyIcons.Other
    }
