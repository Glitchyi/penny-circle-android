package com.spendingbuddy.app

import com.spendingbuddy.app.ui.PennyPalette as P
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class PennyPaletteContrastTest {
    private fun lin(v: Int): Double { val s = v / 255.0; return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4) }
    private fun lum(rgb: Int) = 0.2126 * lin(rgb shr 16 and 255) + 0.7152 * lin(rgb shr 8 and 255) + 0.0722 * lin(rgb and 255)
    private fun ratio(a: Int, b: Int): Double { val x = lum(a); val y = lum(b); return (maxOf(x, y) + .05) / (minOf(x, y) + .05) }
    private fun text(name: String, fg: Int, bg: Int) = assertTrue("$name text ${"%.2f".format(ratio(fg, bg))}:1 < 4.5", ratio(fg, bg) >= 4.5)
    private fun ui(name: String, fg: Int, bg: Int) = assertTrue("$name ui ${"%.2f".format(ratio(fg, bg))}:1 < 3", ratio(fg, bg) >= 3.0)

    @Test fun bodyTextOnSurfaces() {
        for (bg in listOf(P.Background, P.Surface, P.SurfaceVariant)) {
            text("text/$bg", P.Text, bg); text("muted/$bg", P.TextMuted, bg)
            text("primary/$bg", P.Primary, bg); text("secondary/$bg", P.Secondary, bg)
            text("tertiary/$bg", P.Tertiary, bg); text("error/$bg", P.Error, bg)
        }
    }

    @Test fun rolePairs() {
        text("onPrimary", P.OnPrimary, P.Primary); text("onSecondary", P.OnSecondary, P.Secondary)
        text("onTertiary", P.OnTertiary, P.Tertiary); text("onError", P.OnError, P.Error)
        text("onPrimaryContainer", P.OnPrimaryContainer, P.PrimaryContainer)
        text("onSecondaryContainer", P.OnSecondaryContainer, P.SecondaryContainer)
        text("onTertiaryContainer", P.OnTertiaryContainer, P.TertiaryContainer)
        text("onErrorContainer", P.OnErrorContainer, P.ErrorContainer)
        text("muted on primaryContainer", P.TextMuted, P.PrimaryContainer)
    }

    @Test fun controlOutlines() {
        ui("outline/surface", P.Outline, P.Surface); ui("outline/background", P.Outline, P.Background)
        ui("primary/surface", P.Primary, P.Surface)
    }

    @Test fun categoryPairs() {
        assertTrue(P.categories.keys.containsAll(listOf("Food", "Transport", "Shopping", "Bills", "Essential", "Convenience", "Social", "Impulse")))
        for ((name, t) in P.categories) {
            text("$name content/container", t.second, t.first)
            ui("$name accent/surface", t.third, P.Surface)
            ui("$name accent/background", t.third, P.Background)
        }
    }
}
