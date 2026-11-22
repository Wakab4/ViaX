package moe.via.browser.feature.scripts.model

data class ScriptOverrides(val runAt: Int = 0, val matches: List<String> = emptyList(), val includes: List<String> = emptyList(),
    val excludeMatches: List<String> = emptyList(), val excludes: List<String> = emptyList())

/** Stored data is separate from the WebView, editor and privileged API implementation. */
data class UserScript(val id: Long = 0, val key: String, val name: String, val content: String, val version: String? = null,
    val matches: List<String> = emptyList(), val includes: List<String> = emptyList(), val excludeMatches: List<String> = emptyList(),
    val excludes: List<String> = emptyList(), val runAt: Int = 4, val requires: List<String> = emptyList(), val resources: Map<String, String> = emptyMap(),
    val enabled: Boolean = true, val flags: Int = 0, val grant: Int = 0, val iconUrl: String? = null, val homepageUrl: String? = null,
    val supportUrl: String? = null, val downloadUrl: String? = null, val updated: Long = System.currentTimeMillis(),
    val created: Long = System.currentTimeMillis(), val overrides: ScriptOverrides = ScriptOverrides()) {
    val effectiveRunAt get() = overrides.runAt.takeIf { it != 0 } ?: runAt
    val unwrap get() = flags and 1 != 0
    val noframes get() = flags and 2 != 0
    fun applies(url: String): Boolean {
        val added = overrides.matches + overrides.includes
        val removed = overrides.excludeMatches + overrides.excludes
        val allowed = added + (matches + includes).filterNot(removed::contains)
        val blocked = removed + (excludeMatches + excludes).filterNot(added::contains)
        return allowed.isNotEmpty() && blocked.none { ScriptUrlMatcher.matches(it, url) } && allowed.any { ScriptUrlMatcher.matches(it, url) }
    }
}
