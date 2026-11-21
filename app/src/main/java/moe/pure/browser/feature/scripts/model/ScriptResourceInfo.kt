package moe.pure.browser.feature.scripts.model

import java.io.File

sealed interface ScriptResourceInfo {
    val url: String
    /** Native path=null resources are inline, without a claimed file, length or successful decoding. */
    data class Inline(override val url: String) : ScriptResourceInfo
    data class Missing(override val url: String) : ScriptResourceInfo
    data class Cached(override val url: String, val file: File, val length: Long,
        val modified: Long) : ScriptResourceInfo
}
