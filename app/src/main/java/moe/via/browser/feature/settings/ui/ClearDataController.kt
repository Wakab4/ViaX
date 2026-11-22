package moe.via.browser.feature.settings.ui

import moe.via.browser.R
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import moe.via.browser.browser.session.data.TabRepository
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.privacy.BrowserCaches
import moe.via.browser.feature.library.data.BookmarkFaviconStore
import moe.via.browser.feature.search.suggestions.SearchHistory
import moe.via.browser.feature.library.history.HistoryDirtyState
import moe.via.browser.feature.library.history.data.HistoryRepository
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.theme.ThemedActivity

/** z8.h0 masks differ from the visible row indices: closed tabs is bit 64. */
class ClearDataController(
    private val activity: ThemedActivity,
    private val settings: BrowserSettings,
    private val views: () -> List<WebView> = { emptyList() },
    private val clearClosedTabs: (() -> Unit)? = null
) {
    private val preferences get() = settings.preferences
    fun show() = choose(activity.getString(R.string.ref_menu_clear_data), preferences.clearDataMask) { mask ->
        preferences.clearDataMask = mask; clear(mask, notify = true)
    }
    fun showExit() = choose(activity.getString(R.string.settings_clear_exit), preferences.exitClearMask) { preferences.exitClearMask = it }
    private fun choose(title: String, mask: Int, apply: (Int) -> Unit) {
        activity.dialogScope.show(BrowserDialog(activity, title)
            .multipleChoice(LABELS.map(activity::getString), BITS.indices.filterTo(mutableSetOf()) { mask and BITS[it] != 0 })
            .positive(activity.getString(android.R.string.ok)) { dialog -> apply(dialog.selectedIndices.fold(0) { value, index -> value or BITS[index] }); true }
            .negative())
    }
    fun clear(mask: Int, notify: Boolean = false, exiting: Boolean = false) {
        val app = activity.applicationContext
        val now = System.currentTimeMillis()
        val cache = mask and 1 != 0 || exiting && now - settings.values.getLong("last_cache_clear", 0) >= 432_000_000L
        if (cache) {
            val active = views()
            if (active.isEmpty()) WebView(activity).apply { clearCache(true); clearSslPreferences(); destroy() }
            else active.forEach { it.clearCache(true); it.clearSslPreferences() }
            BrowserExecutors.io.execute {
                BrowserCaches.clearWebEngineExtras(app)
                moe.via.browser.feature.scripts.data.ScriptRepository(app).use { it.pruneResourceFiles() }
            }
            settings.values.edit().putLong("last_cache_clear", now).apply()
        }
        @Suppress("DEPRECATION")
        if (mask and 2 != 0) WebViewDatabase.getInstance(activity).clearFormData()
        if (mask and 4 != 0) {
            SearchHistory.clear()
            BrowserExecutors.io.execute {
                HistoryRepository(app).use { it.clear() }
                BookmarkFaviconStore.clearHistoryAssets(app)
            }
            // z8.h0.a marks after scheduling; the exit-only h0.c path deliberately does not.
            if (!exiting) HistoryDirtyState.markDirty()
        }
        if (mask and 64 != 0) {
            clearClosedTabs?.invoke() ?: BrowserExecutors.io.execute {
                TabRepository(app).use { it.clearClosed() }
            }
        }
        if (mask and 8 != 0) WebStorage.getInstance().deleteAllData()
        if (mask and 16 != 0) {
            @Suppress("DEPRECATION")
            WebViewDatabase.getInstance(activity).apply { clearFormData(); clearHttpAuthUsernamePassword() }
            CookieManager.getInstance().removeAllCookies { CookieManager.getInstance().flush() }
        }
        if (mask and 32 != 0) BrowserExecutors.io.execute {
            try { BrowserCaches.clearApp(app) } finally { BookmarkFaviconStore.evictMemory() }
        }
        if (notify && mask and 127 != 0) BrowserMessage(activity).show(activity.getString(R.string.settings_cleared))
    }
    companion object {
        private val LABELS = listOf(R.string.settings_clear_cache, R.string.settings_clear_forms, R.string.ref_menu_history,
            R.string.ref_closed_tabs_title, R.string.settings_clear_storage, R.string.settings_clear_cookies, R.string.settings_clear_app_cache)
        val BITS = intArrayOf(1, 2, 4, 64, 8, 16, 32)
    }
}
