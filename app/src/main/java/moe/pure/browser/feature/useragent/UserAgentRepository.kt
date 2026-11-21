package moe.pure.browser.feature.useragent

import android.content.Context
import android.webkit.WebSettings
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.data.BrowserStore
import moe.pure.browser.data.settings.NativeSettingsRows
import moe.pure.browser.data.settings.NativeSettingsRow
import org.json.JSONArray

data class UserAgent(val id: Int, val name: String, val value: String?, val createdAt: Long = 0,
    val updatedAt: Long = 0, val type: Int = 1, val note: String? = null, val flag: Int = 0)

class UserAgentRepository(private val context: Context) : AutoCloseable {
    private val store = BrowserStore(context)
    val presets: List<UserAgent> by lazy {
        val json = JSONArray(context.assets.open("user_agents.json").bufferedReader().use { it.readText() })
        List(json.length()) { index -> json.getJSONObject(index).let {
            UserAgent(it.getInt("id"), it.getString("name"), if (it.isNull("value")) null else it.getString("value"))
        } }
    }

    fun all(): List<UserAgent> = presets + NativeSettingsRows(store.readableDatabase).all(1).map(::agent)

    fun get(id: Int): UserAgent? = if (id <= 0) presets.firstOrNull { it.id == id }
        else NativeSettingsRows(store.readableDatabase).get(id)?.let(::agent)

    private fun agent(row: NativeSettingsRow) = UserAgent(row.id, row.title.orEmpty(), row.content,
        row.createdAt, row.updatedAt, row.type, row.note, row.flag)

    internal fun insertNative(name: String?, value: String?): Int = NativeSettingsRows(store.writableDatabase)
        .insert(NativeSettingsRow.create(1, name, value))

    fun save(id: Int?, name: String, value: String): Int {
        val now = System.currentTimeMillis()
        val rows = NativeSettingsRows(store.writableDatabase)
        val previous = if (id != null && id > 0) rows.get(id) else null
        val row = (previous ?: NativeSettingsRow(type = 1, createdAt = now)).copy(
            title = UserAgentText.cleanName(name), content = UserAgentText.cleanValue(value), updatedAt = now)
        if (id != null && id > 0) {
            rows.update(id, row)
            changed()
            return id
        }
        val savedId = rows.insert(row)
        if (savedId > 0) changed()
        return savedId
    }

    fun delete(id: Int) { if (id > 0) { NativeSettingsRows(store.writableDatabase).delete(id); changed() } }
    private fun changed() { context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().putLong("ua.revision", System.nanoTime()).apply() }

    fun resolve(settings: BrowserSettings, desktop: Boolean, overrideId: Int? = null, custom: String? = null): String? {
        val default = runCatching { WebSettings.getDefaultUserAgent(context) }.getOrNull()?.let(UserAgentText::webViewDefault)
        val globalId = settings.userAgentId
        val global = choose(settings, globalId, settings.values.getString("uastring", default), default,
            settings.preferences.desktop)?.takeIf(String::isNotEmpty)
        if (overrideId == null) return global
        val id = if (overrideId == -1000) globalId else overrideId
        val source = if (id > 0) get(id)?.value else custom?.takeIf(String::isNotEmpty) ?: global
        return choose(settings, id, source, default, desktop)
    }

    private fun choose(settings: BrowserSettings, choice: Int, source: String?, default: String?, desktop: Boolean): String? {
        val windows = presets.firstOrNull { it.id == -3 }?.value
        val id: Int
        val fallback: String?
        if (desktop) {
            id = settings.preferences.desktopUserAgentId
            // b4.c returns this branch before reduction, including when a site supplied another ID.
            if (id == 0) return windows
            fallback = settings.values.getString("duastring", null)?.takeIf(String::isNotEmpty) ?: windows
        } else { id = choice; fallback = source }
        val value = if (id == 0) default else if (id in -8..-1) presets.firstOrNull { it.id == id }?.value else fallback
        return if (settings.preferences.simplifyUserAgent && value != null) UserAgentText.reduce(value) else value
    }

    override fun close() = store.close()
}
