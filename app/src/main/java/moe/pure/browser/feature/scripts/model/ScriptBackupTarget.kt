package moe.pure.browser.feature.scripts.model

/** Export traverses metadata first and obtains execution source only for an accepted cache key. */
internal data class ScriptBackupTarget(val id: Long, val name: String?, val url: String?, val enabled: Boolean, val updated: Long)
