package moe.pure.browser.feature.settings.ui

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import moe.pure.browser.browser.TabRepository
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.data.BrowserStore
import moe.pure.browser.feature.privacy.BrowserCaches
import moe.pure.browser.feature.library.data.BookmarkFaviconStore
import moe.pure.browser.feature.search.suggestions.SearchHistory
import moe.pure.browser.feature.history.HistoryDirtyState
import moe.pure.browser.ui.components.BrowserMessage
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity

/** z8.h0 masks differ from the visible row indices: closed tabs is bit 64. */
class ClearDataController(
    private val activity: ThemedActivity,
    private val settings: BrowserSettings,
    private val views: () -> List<WebView> = { emptyList() },
    private val clearClosedTabs: (() -> Unit)? = null
) {
    private val preferences get() = settings.preferences
    fun show() = choose("清除数据", preferences.clearDataMask) { mask ->
        preferences.clearDataMask = mask; clear(mask, notify = true)
    }
    fun showExit() = choose("退出时清除数据", preferences.exitClearMask) { preferences.exitClearMask = it }
    private fun choose(title: String, mask: Int, apply: (Int) -> Unit) {
        activity.dialogScope.show(BrowserDialog(activity, title)
            .multipleChoice(LABELS, BITS.indices.filterTo(mutableSetOf()) { mask and BITS[it] != 0 })
            .positive("确定") { dialog -> apply(dialog.selectedIndices.fold(0) { value, index -> value or BITS[index] }); true }
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
                moe.pure.browser.feature.scripts.data.ScriptRepository(app).use { it.pruneResourceFiles() }
            }
            settings.values.edit().putLong("last_cache_clear", now).apply()
        }
        @Suppress("DEPRECATION")
        if (mask and 2 != 0) WebViewDatabase.getInstance(activity).clearFormData()
        if (mask and 4 != 0) {
            SearchHistory.clear()
            BrowserExecutors.io.execute {
                BrowserStore(app).use { it.clearHistory() }
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
        if (notify && mask and 127 != 0) BrowserMessage(activity).show("数据已清除")
    }
    companion object {
        val LABELS = listOf("缓存", "表单数据", "历史", "关闭的标签页", "网页存储", "Cookies (登录状态)", "应用缓存")
        val BITS = intArrayOf(1, 2, 4, 64, 8, 16, 32)
    }
}
