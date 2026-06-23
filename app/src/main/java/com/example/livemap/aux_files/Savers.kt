package com.example.livemap.aux_files

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import java.time.LocalDateTime

/**
 * Reusable Savers for rememberSaveable, so form state survives configuration
 * changes and process death. Shared by NewScreen and ProfileScreen instead of
 * each redefining its own.
 */

/** Persists a List<String> (the default autoSaver can't handle arbitrary lists). */
val stringListSaver = listSaver<List<String>, String>(
    save = { it.toList() },
    restore = { it }
)

/** Persists a nullable LocalDateTime as ISO-8601 text ("" represents null). */
val localDateTimeSaver = Saver<LocalDateTime?, String>(
    save = { it?.toString() ?: "" },
    restore = { if (it.isBlank()) null else LocalDateTime.parse(it) }
)
