package com.spendingbuddy.app.ui

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PennyIconsTest {

    @Test
    fun categoryIconVector_mapsKnownCategoriesCaseInsensitively() {
        assertSame(PennyIcons.Food, categoryIconVector("Food"))
        assertSame(PennyIcons.Transport, categoryIconVector("transport"))
        assertSame(PennyIcons.Bills, categoryIconVector("Bills"))
        assertSame(PennyIcons.Shopping, categoryIconVector("Shopping"))
    }

    @Test
    fun categoryIconVector_fallsBackToOther() {
        assertSame(PennyIcons.Other, categoryIconVector("Essential"))
        assertSame(PennyIcons.Other, categoryIconVector(""))
        assertSame(PennyIcons.Other, categoryIconVector("xyz"))
    }

    @Test
    fun allIconsHave24dpSizeAndContent() {
        val icons: List<ImageVector> = listOf(
            PennyIcons.Food,
            PennyIcons.Transport,
            PennyIcons.Shopping,
            PennyIcons.Bills,
            PennyIcons.Other,
            PennyIcons.Today,
            PennyIcons.Activity,
            PennyIcons.Circle,
            PennyIcons.Add,
            PennyIcons.ArrowRight
        )
        assertEquals(10, icons.size)
        for (icon in icons) {
            assertEquals(24.dp, icon.defaultWidth)
            assertEquals(24.dp, icon.defaultHeight)
            assertTrue(icon.root.size > 0)
        }
    }
}
