package moe.pure.browser.feature.scripts.model

/** Backup comparison does not need the stored script body or its display fields. */
internal data class ScriptRestoreTarget(val id: Long, val updated: Long, val enabled: Boolean)
