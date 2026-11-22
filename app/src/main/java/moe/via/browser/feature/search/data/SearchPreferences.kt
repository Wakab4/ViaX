package moe.via.browser.feature.search.data

import android.content.Context
import moe.via.browser.feature.search.model.SearchEngine
import moe.via.browser.storage.preferences.BrowserPreferences
import org.json.JSONObject

class SearchPreferences(context: Context) {
    val values = context.applicationContext.getSharedPreferences("browser", Context.MODE_PRIVATE)
    var engineId: Int
        get() {
            val id = values.getInt("search2", SearchBuiltins.defaultId())
            return if (id > 0 || id <= -999 || SearchBuiltins.all().any { it.id == id }) id else SearchBuiltins.defaultId()
        }
        set(value) { values.edit().putInt("search2", value).apply() }
    var customUrl: String
        get() = values.getString("searchurl", "https://www.bing.com/search?q=").orEmpty()
        set(value) { values.edit().putString("searchurl", value).apply() }
    var suggestionMask: Int
        get() = values.getInt("searchsuggestion", 31)
        set(value) { values.edit().putInt("searchsuggestion", value).apply() }
    var toolbarEnabled: Boolean
        get() = values.getBoolean(BrowserPreferences.SEARCH_TOOLBAR_KEY, false)
        set(value) { values.edit().putBoolean(BrowserPreferences.SEARCH_TOOLBAR_KEY, value).apply() }
    var toolbarDisabledIds: List<Int>
        get() = csv("searchtoolbardisabled")
        set(value) { values.edit().putString("searchtoolbardisabled", value.joinToString(",")).apply() }
    var toolbarOrder: List<Int>
        get() = csv("searchtoolbarorder")
        set(value) { values.edit().putString("searchtoolbarorder", value.joinToString(",")).apply() }

    fun shortcuts(): Map<Int, String> = DEFAULT_SHORTCUTS.toMutableMap().apply {
        runCatching {
            val json = JSONObject(this@SearchPreferences.values.getString("searchshortcuts", "").orEmpty())
            json.keys().forEach { key -> key.toIntOrNull()?.let { put(it, json.optString(key, "")) } }
        }
    }
    fun shortcut(id: Int): String = shortcuts()[id].orEmpty()
    internal fun writeShortcuts(map: Map<Int, String>) {
        val json = JSONObject()
        map.forEach { (id, value) -> json.put(id.toString(), value) }
        values.edit().putString("searchshortcuts", json.toString()).apply()
    }
    fun setToolbarVisible(id: Int, visible: Boolean) {
        if (id == engineId && !visible) return
        toolbarDisabledIds = toolbarDisabledIds.toMutableList().apply { if (visible) remove(id) else if (id !in this) add(id) }
    }
    fun ordered(engines: List<SearchEngine>): List<SearchEngine> {
        val order = toolbarOrder
        return engines.sortedWith { left, right ->
            val first = order.indexOf(left.id); val second = order.indexOf(right.id)
            when { first >= 0 && second >= 0 -> first.compareTo(second); first >= 0 -> -1; second >= 0 -> 1; else -> 0 }
        }
    }
    fun visible(engines: List<SearchEngine>): List<SearchEngine> = ordered(engines).filter { it.id == engineId || it.id !in toolbarDisabledIds }
    fun moveToolbar(from: Int, to: Int, engines: List<SearchEngine>) {
        val list = ordered(engines).toMutableList()
        if (from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        toolbarOrder = list.map(SearchEngine::id)
    }
    private fun csv(key: String) = values.getString(key, "").orEmpty().split(',').mapNotNull { it.trim().toIntOrNull() }

    companion object {
        val DEFAULT_SHORTCUTS: Map<Int, String> = linkedMapOf(-1 to "gg", -3 to "bn", -7 to "yd", -8 to "yh", -9 to "sp", -10 to "dd")
    }
}
