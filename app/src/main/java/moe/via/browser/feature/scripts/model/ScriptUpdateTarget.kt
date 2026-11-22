package moe.via.browser.feature.scripts.model

/** r5.c update selection needs metadata, without reading each script body. */
internal class ScriptUpdateTarget(val id: Long, val name: String, val url: String?, val enabled: Boolean, val updated: Long) {
    companion object {
        fun from(script: UserScript) = ScriptUpdateTarget(script.id, script.name, script.downloadUrl, script.enabled, script.updated)
    }
}
