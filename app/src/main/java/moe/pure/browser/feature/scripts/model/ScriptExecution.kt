package moe.pure.browser.feature.scripts.model

/** r5.d compiles execution fields and scope overrides once when a stored record enters the cache. */
class ScriptExecution(script: UserScript) {
    val id = script.id
    val key = script.key
    val content = script.content
    val flags = script.flags
    val grant = script.grant
    val runAt = script.effectiveRunAt and 7
    var enabled = script.enabled
        internal set
    val requires = script.requires.toList()
    val resources: Map<String, String> = HashMap(script.resources)
    private val included: List<String>
    private val excluded: List<String>
    init {
        val added = script.overrides.matches + script.overrides.includes
        excluded = script.overrides.excludeMatches + script.overrides.excludes +
            (script.excludeMatches + script.excludes).filterNot(added::contains)
        included = added + (script.matches + script.includes).filterNot(excluded::contains)
    }
    val unwrap get() = flags and 1 != 0
    val noframes get() = flags and 2 != 0
    fun applies(url: String): Boolean = url.isNotEmpty() && included.isNotEmpty() &&
        excluded.none { ScriptUrlMatcher.matches(it, url) } && included.any { ScriptUrlMatcher.matches(it, url) }
}
