package com.spendingbuddy.app

import org.junit.Assert.*
import org.junit.Test

class PresetsTest {
    private fun ok(o: PresetOutcome) = (o as PresetOutcome.Ok)
    private fun add(s: BuddyState, n: String, d: Boolean = false) = ok(addPresetTo(s, n, "Food", "Essential", d)).state

    @Test fun addTrimsAndRejectsBlankLongAndDuplicates() {
        val s = add(BuddyState(), "  Chai Point ")
        assertEquals("Chai Point", s.presets.single().name)
        assertTrue(addPresetTo(s, "   ", "Food", "Essential") is PresetOutcome.Invalid)
        assertTrue(addPresetTo(s, "chai POINT", "Food", "Essential") is PresetOutcome.Invalid)
        assertTrue(addPresetTo(s, "x".repeat(MAX_PRESET_NAME + 1), "Food", "Essential") is PresetOutcome.Invalid)
        assertTrue(addPresetTo(s, "Metro", "", "Essential") is PresetOutcome.Invalid)
        assertTrue(addPresetTo(s, "Metro", "Transport", " ") is PresetOutcome.Invalid)
    }

    @Test fun capIsEnforced() {
        var s = BuddyState()
        repeat(MAX_PRESETS) { s = add(s, "p$it") }
        assertTrue(addPresetTo(s, "one more", "Food", "Essential") is PresetOutcome.Invalid)
    }

    @Test fun atMostOneDefault() {
        var s = add(BuddyState(), "A", true)
        s = add(s, "B", true)
        assertEquals(listOf("B"), s.presets.filter { it.isDefault }.map { it.name })
        s = setDefaultPresetIn(s, s.presets.first { it.name == "A" }.id)
        assertEquals(listOf("A"), s.presets.filter { it.isDefault }.map { it.name })
        assertTrue(setDefaultPresetIn(s, null).presets.none { it.isDefault })
        assertEquals(s, setDefaultPresetIn(s, "missing"))
    }

    @Test fun updateAllowsOwnNameButNotAnothers() {
        var s = add(add(BuddyState(), "A"), "B")
        val a = s.presets[0]
        val renamedCase = ok(updatePresetIn(s, a.id, "a", "Bills", "Social"))
        assertEquals("Bills", renamedCase.preset.category)
        assertTrue(updatePresetIn(s, a.id, "b", "Food", "Essential") is PresetOutcome.Invalid)
        assertTrue(updatePresetIn(s, "nope", "Z", "Food", "Essential") is PresetOutcome.Invalid)
    }

    @Test fun deleteReturnsRemovedAndUndoRestores() {
        val s = add(BuddyState(), "A", true)
        val (after, removed) = deletePresetFrom(s, s.presets.single().id)
        assertTrue(after.presets.isEmpty()); assertEquals("A", removed!!.name)
        assertNull(deletePresetFrom(after, "x").second)
        assertEquals(s, restorePresetTo(after, removed))
        // restoring while another default exists keeps a single default
        val other = add(after, "B", true)
        assertEquals(1, restorePresetTo(other, removed).presets.count { it.isDefault })
        // restoring twice does not duplicate
        assertEquals(1, restorePresetTo(s, removed).presets.size)
    }

    @Test fun olderStateLoadsWithNoPresetsAndRoundTrips() {
        val old = StateCodec.decode("""{"schemaVersion":3,"name":"R"}""")
        assertTrue(old.presets.isEmpty())
        val s = add(old, "Chai", true)
        assertEquals(s.presets, StateCodec.decode(StateCodec.encode(s)).presets)
    }
}
