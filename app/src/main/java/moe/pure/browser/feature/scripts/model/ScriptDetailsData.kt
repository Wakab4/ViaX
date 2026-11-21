package moe.pure.browser.feature.scripts.model

/** r5.c metadata has no script body or scope fields; nullable text stays nullable. */
internal data class ScriptDetailsMetadata(val id: Int, val name: String?, val version: String?, val enabled: Boolean,
    val iconUrl: String?, val homepageUrl: String?, val downloadUrl: String?, val updated: Long, val created: Long,
    val supportUrl: String? = null)

/** r5.e retains raw JSON-array strings and distinguishes a missing object from an empty one. */
internal data class ScriptDetailsScope(val runAt: Int, val matches: String?, val excludeMatches: String?,
    val includes: String?, val excludes: String?)

/** ta.b0.n reads cached source first, then metadata and the two independent scope queries. */
internal data class ScriptDetailsData(val metadata: ScriptDetailsMetadata?, val source: ScriptExecution?,
    val defaults: ScriptDetailsScope?, val overrides: ScriptDetailsScope?)
