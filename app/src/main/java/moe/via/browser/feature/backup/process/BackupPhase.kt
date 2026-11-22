package moe.via.browser.feature.backup.process

import moe.via.browser.feature.backup.model.BackupData
import moe.via.browser.feature.backup.model.BackupResult

internal enum class BackupPhase {
    IDLE, SELECTION, COUNTING_PASSWORDS, AUTHENTICATION, EXPORT_PASSWORD, EXPORT_DOCUMENT,
    IMPORT_DOCUMENT, PREPARING_IMPORT, IMPORT_PASSWORD, IMPORTING, EXPORTING, RESULT
}

internal data class BackupOutcome(val result: BackupResult? = null, val exporting: Boolean = false,
    val path: String? = null, val failure: String? = null, val imported: Boolean = false)

internal data class BackupSnapshot(val phase: BackupPhase, val revision: Long,
    val selection: Set<BackupData>, val outcome: BackupOutcome? = null)
