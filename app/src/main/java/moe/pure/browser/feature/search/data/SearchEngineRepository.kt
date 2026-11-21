package moe.pure.browser.feature.search.data

import android.content.Context
import moe.pure.browser.R
import moe.pure.browser.data.BrowserStore
import moe.pure.browser.data.settings.NativeSettingsRows
import moe.pure.browser.data.settings.NativeSettingsRow
import moe.pure.browser.feature.search.model.SearchEngine
import java.io.Closeable

/** Synchronous parameter-bound queries; callers dispatch database work off the UI thread. */
class SearchEngineRepository(private val context: Context) : Closeable {
    private val app = context.applicationContext
    private val store = BrowserStore(app)
    private val preferences = SearchPreferences(app)
    private val locale = context.resources.configuration.locales[0] ?: java.util.Locale.getDefault()

    fun all(): List<SearchEngine> = SearchBuiltins.visible(locale) + custom()
    fun get(id: Int): SearchEngine? = SearchBuiltins.all(locale).firstOrNull { it.id == id }
        ?: NativeSettingsRows(store.readableDatabase).get(id)?.let(::engine)
        ?: if (id <= -999 && preferences.customUrl.isNotEmpty()) SearchEngine(id, context.getString(R.string.ref_startup_custom), preferences.customUrl) else null
    fun selected(): SearchEngine = get(preferences.engineId) ?: get(SearchBuiltins.defaultId()) ?: all().first()
    fun select(id: Int): Boolean {
        val engine = get(id) ?: return false
        preferences.engineId = engine.id
        if (engine.custom) preferences.customUrl = engine.mobileTemplate
        return true
    }
    fun toolbar(): List<SearchEngine> = preferences.ordered(all())
    internal fun insertNative(name: String?, value: String?): Int = NativeSettingsRows(store.writableDatabase)
        .insert(NativeSettingsRow.create(2, name, value))
    internal fun editorRecord(id: Int): NativeSettingsRow? = when {
        id > 0 -> NativeSettingsRows(store.readableDatabase).get(id)
        id < 0 -> SearchBuiltins.visible(locale).firstOrNull { it.id == id }?.let {
            NativeSettingsRow(id = it.id, title = it.name, type = 2)
        }
        else -> null
    }
    internal fun saveEditor(record: NativeSettingsRow): NativeSettingsRow {
        val now = System.currentTimeMillis()
        val rows = NativeSettingsRows(store.writableDatabase)
        val updated = record.copy(updatedAt = now, createdAt = if (record.id > 0) record.createdAt else now,
            type = if (record.id > 0) record.type else 2)
        if (record.id > 0) rows.update(record.id, updated) else updated.id = rows.insert(updated)
        return updated
    }
    fun delete(id: Int): Boolean {
        if (id <= 0) return false
        return NativeSettingsRows(store.writableDatabase).delete(id)
    }
    private fun custom(): List<SearchEngine> = NativeSettingsRows(store.readableDatabase).all(2).map(::engine)
    private fun engine(row: NativeSettingsRow) = SearchEngine(row.id, row.title.orEmpty(), row.content.orEmpty(),
        note = row.note, flags = row.flag, createdAt = row.createdAt, updatedAt = row.updatedAt)
    override fun close() = store.close()
}
