package com.spendingbuddy.app

const val MAX_PRESETS = 30
const val MAX_PRESET_NAME = 40

sealed interface PresetOutcome {
    data class Ok(val state: BuddyState, val preset: Preset) : PresetOutcome
    data class Invalid(val message: String) : PresetOutcome
}

/** True when a preset with this name (trimmed, case-insensitive) already exists. */
fun hasPresetNamed(presets: List<Preset>, name: String, exceptId: String? = null): Boolean =
    name.trim().let { n -> n.isNotEmpty() && presets.any { it.id != exceptId && it.name.trim().equals(n, ignoreCase = true) } }

/** Returns an error message or null. Category and reason must be non-blank; the name must be unique ignoring case. */
fun validatePreset(presets: List<Preset>, name: String, category: String, reason: String, exceptId: String? = null): String? {
    val n = name.trim()
    return when {
        n.isEmpty() -> "Give the preset a name"
        n.length > MAX_PRESET_NAME -> "Keep the name to $MAX_PRESET_NAME characters or fewer"
        category.isBlank() -> "Choose a category"
        reason.isBlank() -> "Choose a reason"
        hasPresetNamed(presets, n, exceptId) -> "You already have a preset called \"$n\""
        exceptId == null && presets.size >= MAX_PRESETS -> "You can keep up to $MAX_PRESETS presets"
        else -> null
    }
}

fun addPresetTo(s: BuddyState, name: String, category: String, reason: String, makeDefault: Boolean = false): PresetOutcome {
    validatePreset(s.presets, name, category, reason)?.let { return PresetOutcome.Invalid(it) }
    val p = Preset(name = name.trim(), category = category.trim(), reason = reason.trim(), isDefault = makeDefault)
    val base = if (makeDefault) s.presets.map { it.copy(isDefault = false) } else s.presets
    return PresetOutcome.Ok(s.copy(presets = base + p), p)
}

fun updatePresetIn(s: BuddyState, id: String, name: String, category: String, reason: String): PresetOutcome {
    val old = s.presets.firstOrNull { it.id == id } ?: return PresetOutcome.Invalid("That preset no longer exists")
    validatePreset(s.presets, name, category, reason, exceptId = id)?.let { return PresetOutcome.Invalid(it) }
    val p = old.copy(name = name.trim(), category = category.trim(), reason = reason.trim())
    return PresetOutcome.Ok(s.copy(presets = s.presets.map { if (it.id == id) p else it }), p)
}

/** Makes [id] the single default, or clears the default when [id] is null. */
fun setDefaultPresetIn(s: BuddyState, id: String?): BuddyState =
    if (id != null && s.presets.none { it.id == id }) s
    else s.copy(presets = s.presets.map { it.copy(isDefault = it.id == id) })

fun deletePresetFrom(s: BuddyState, id: String): Pair<BuddyState, Preset?> {
    val removed = s.presets.firstOrNull { it.id == id } ?: return s to null
    return s.copy(presets = s.presets.filterNot { it.id == id }) to removed
}

/** Re-inserts a deleted preset (Undo). Keeps at most one default and skips it if its name is now taken. */
fun restorePresetTo(s: BuddyState, preset: Preset): BuddyState {
    if (s.presets.any { it.id == preset.id } || hasPresetNamed(s.presets, preset.name)) return s
    val keepDefault = preset.isDefault && s.presets.none { it.isDefault }
    return s.copy(presets = s.presets + preset.copy(isDefault = keepDefault))
}
