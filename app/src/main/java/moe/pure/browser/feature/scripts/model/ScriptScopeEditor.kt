package moe.pure.browser.feature.scripts.model

data class ScriptScope(val pattern: String, val match: Boolean, val editable: Boolean)

/** Overrides cancel equal source entries without rewriting the installed script. */
object ScriptScopeEditor {
    fun entries(script: UserScript): List<ScriptScope> {
        val added = script.overrides.matches + script.overrides.includes
        val removed = script.overrides.excludeMatches + script.overrides.excludes
        val blocked = ((script.excludeMatches + script.excludes).filterNot(added::contains).map { ScriptScope(it, false, false) } +
            removed.map { ScriptScope(it, false, true) }).distinctBy { it.pattern }
        val allowed = ((script.matches + script.includes).filterNot { item -> blocked.any { it.pattern == item } }.map { ScriptScope(it, true, false) } +
            added.map { ScriptScope(it, true, true) }).distinctBy { it.pattern }
        return allowed + blocked
    }
    fun add(script: UserScript, text: String, match: Boolean): ScriptOverrides {
        val pattern = text.trim()
        if (pattern.isEmpty()) return script.overrides
        val added = (script.overrides.matches + script.overrides.includes).toMutableList()
        val removed = (script.overrides.excludeMatches + script.overrides.excludes).toMutableList()
        if (match) {
            removed.removeAll { it == pattern }
            if (pattern !in script.matches + script.includes && pattern !in added) added += pattern
        } else {
            added.removeAll { it == pattern }
            if (pattern !in script.excludeMatches + script.excludes && pattern !in removed) removed += pattern
        }
        return ScriptOverrides(script.overrides.runAt, includes = added, excludes = removed)
    }
    fun remove(script: UserScript, entry: ScriptScope): ScriptOverrides = if (!entry.editable) add(script, entry.pattern, !entry.match)
    else script.overrides.copy(matches = script.overrides.matches - entry.pattern, includes = script.overrides.includes - entry.pattern,
        excludeMatches = script.overrides.excludeMatches - entry.pattern, excludes = script.overrides.excludes - entry.pattern)
    fun replace(script: UserScript, entry: ScriptScope, text: String): ScriptOverrides {
        if (!entry.editable || text.isBlank() || text.trim() == entry.pattern) return script.overrides
        return add(script.copy(overrides = remove(script, entry)), text, entry.match)
    }
}
