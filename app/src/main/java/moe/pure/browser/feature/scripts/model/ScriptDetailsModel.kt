package moe.pure.browser.feature.scripts.model

import androidx.lifecycle.ViewModel
import moe.pure.browser.core.lifecycle.NativeLiveEvent
import moe.pure.browser.feature.scripts.data.ScriptRepository
import moe.pure.browser.feature.scripts.data.ScriptDetailsDataSource
import moe.pure.browser.ui.tasks.NativeViewJobs
import java.io.File

/** ta.b0 owns selection and scope edits; only its reads are bound to model clearing. */
internal class ScriptDetailsModel(private val repository: ScriptRepository) : ViewModel() {
    private val jobs = NativeViewJobs()
    val changed = NativeLiveEvent<Unit>()
    var id = 0
        private set
    var metadata: ScriptDetailsMetadata? = null
        private set
    var runAt = 0
        private set
    var defaultRunAt = 0
        private set
    var sourceLength = 0
        private set
    private val baseMatches = ArrayList<String>()
    private val baseExcludes = ArrayList<String>()
    private val added = ArrayList<String>()
    private val removed = ArrayList<String>()
    val dependencies = ArrayList<ScriptDetailsResource>()
    val resources = ArrayList<ScriptDetailsResource>()

    fun load(id: Int) {
        this.id = id
        jobs.launch({ repository.details(id) }) { value ->
            metadata = value.metadata
            val source = value.source
            if (source != null) {
                sourceLength = source.content.length
                dependencies.clear(); resources.clear()
                source.requires.forEach { dependencies.add(resource(it)) }
                source.resources.values.forEach { resources.add(resource(it)) }
                dependencies.sortBy { it.url }; resources.sortBy { it.url }
            }
            baseMatches.clear(); baseExcludes.clear(); added.clear(); removed.clear()
            value.defaults?.let { defaults ->
                defaultRunAt = defaults.runAt
                baseMatches.addAll(ScriptDetailsDataSource.scopes(defaults.matches, defaults.includes))
                baseExcludes.addAll(ScriptDetailsDataSource.scopes(defaults.excludeMatches, defaults.excludes))
            }
            value.overrides?.let { overrides ->
                runAt = overrides.runAt
                added.addAll(ScriptDetailsDataSource.scopes(overrides.matches, overrides.includes))
                removed.addAll(ScriptDetailsDataSource.scopes(overrides.excludeMatches, overrides.excludes))
            }
            notifyChanged()
        }
    }

    fun load(key: String) {
        if (key.isEmpty()) return
        jobs.launch({ repository.detailsMetadata(key) }) { value -> if (value != null) load(value.id) }
    }

    private fun resource(url: String): ScriptDetailsResource {
        val path = repository.resourcePath(url)
        return ScriptDetailsResource(url, path, path.isNullOrEmpty() || File(path).isFile)
    }
    fun downloadResource(url: String) = repository.downloadResource(url)

    fun scopes(): List<ScriptScope> {
        val result = ArrayList<ScriptScope>()
        val blocked = HashSet<String>()
        val matched = HashSet<String>()
        val exclusions = ArrayList<ScriptScope>()
        baseExcludes.forEach { pattern ->
            if (pattern !in added && blocked.add(pattern)) exclusions.add(ScriptScope(pattern, false, false))
        }
        removed.forEach { pattern -> if (blocked.add(pattern)) exclusions.add(ScriptScope(pattern, false, true)) }
        baseMatches.forEach { pattern ->
            if (pattern !in blocked && matched.add(pattern)) result.add(ScriptScope(pattern, true, false))
        }
        added.forEach { pattern -> if (matched.add(pattern)) result.add(ScriptScope(pattern, true, true)) }
        return result + exclusions
    }

    fun selectRunAt(value: Int) { runAt = value; persist(); notifyChanged() }

    fun reset() { added.clear(); removed.clear(); runAt = 0; persist(); notifyChanged() }

    fun add(text: String, match: Boolean) {
        val pattern = text.trim { it <= ' ' }
        if (pattern.isEmpty()) return
        if (match) {
            if (pattern in added) return
            removed.remove(pattern)
            if (pattern !in baseMatches) added.add(pattern)
        } else {
            if (pattern in removed) return
            added.remove(pattern)
            if (pattern !in baseExcludes) removed.add(pattern)
        }
        persist(); notifyChanged()
    }

    fun remove(entry: ScriptScope) {
        if (!entry.editable) { add(entry.pattern, !entry.match); return }
        val values = if (entry.match) added else removed
        if (!values.remove(entry.pattern)) return
        persist(); notifyChanged()
    }

    fun replace(entry: ScriptScope, text: String) {
        val pattern = text.trim { it <= ' ' }
        if (pattern.isEmpty() || pattern == entry.pattern) return
        (if (entry.match) added else removed).remove(entry.pattern)
        add(pattern, entry.match)
    }

    private fun persist() {
        repository.override(id.toLong(), ScriptOverrides(
            runAt = if (runAt == defaultRunAt || runAt == 0) 0 else runAt,
            matches = added.toList(), excludeMatches = removed.toList()))
    }
    private fun notifyChanged() { changed.value = Unit }
    override fun onCleared() { jobs.close(); super.onCleared() }
}
