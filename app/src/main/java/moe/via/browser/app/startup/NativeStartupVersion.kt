package moe.via.browser.app.startup

import android.content.Context
import moe.via.browser.app.lifecycle.NativeVersionMarker
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import moe.via.browser.feature.search.data.SearchBuiltins
import moe.via.browser.feature.search.data.SearchEngineRepository
import moe.via.browser.feature.useragent.data.UserAgentRepository
import moe.via.browser.ui.tasks.NativeViewJobs

/** ua.b0/e2/d2 run after incoming-entry parsing and before default/startup model completion. */
internal class NativeStartupVersion(context: Context) : AutoCloseable {
    private val app = context.applicationContext
    private val values = app.getSharedPreferences("browser", Context.MODE_PRIVATE)
    private val version = NativeVersionMarker(app)
    private val jobs = NativeViewJobs()

    fun apply(customTitle: String, completed: () -> Unit) {
        jobs.launch({
            val previous = version.previous
            if (version.installed > previous) {
                GeneratedPageDirtyState.markAll()
                if (previous < 20201231) values.edit().putBoolean("login", false).apply()
                if (values.getInt("uachoice", 0) == -999) {
                    val id = UserAgentRepository(app).use { it.insertNative(customTitle, values.getString("uastring", null)) }
                    values.edit().putInt("uachoice", id).apply()
                }
                if (values.getInt("search2", SearchBuiltins.defaultId()) == -999) {
                    val id = SearchEngineRepository(app).use {
                        it.insertNative(customTitle, values.getString("searchurl", "https://www.bing.com/search?q="))
                    }
                    values.edit().putInt("search2", id).apply()
                }
                version.mark()
            }
        }) { completed() }
    }

    override fun close() = jobs.close()
}
