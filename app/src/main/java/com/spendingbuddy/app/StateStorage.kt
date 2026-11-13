package com.spendingbuddy.app

import java.io.File
import java.nio.file.Files

internal fun stateStorageArtifacts(baseFile: File): List<File> =
    listOf(baseFile, File(baseFile.path + ".bak"), File(baseFile.path + ".new"))

/** FileNotFoundException also includes access failures. Unknown presence must never mean a fresh install. */
internal fun stateStorageIsMissing(baseFile: File): Boolean = try {
    stateStorageArtifacts(baseFile).all { Files.notExists(it.toPath()) }
} catch (_: Exception) { false }
