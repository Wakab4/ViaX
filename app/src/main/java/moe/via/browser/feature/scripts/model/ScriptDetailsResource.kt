package moe.via.browser.feature.scripts.model

/** ta.a keeps its mutable availability flag with the URL and nullable cache path. */
internal data class ScriptDetailsResource(val url: String, val path: String?, var cached: Boolean)
