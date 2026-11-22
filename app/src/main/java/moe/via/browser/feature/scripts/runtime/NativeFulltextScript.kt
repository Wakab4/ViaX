package moe.via.browser.feature.scripts.runtime

import android.content.Context
import moe.via.browser.feature.scripts.data.ScriptExecutionCache
import moe.via.browser.feature.scripts.data.ScriptRepository
import moe.via.browser.feature.scripts.model.BuiltinScripts
import moe.via.browser.feature.search.data.remote.RemoteSearchValues
import moe.via.browser.storage.preferences.BrowserPreferences

/** z8.j0 uses the installed official script when present, otherwise the process-only -99 entry. */
internal object NativeFulltextScript {
    fun initialize(context: Context, repository: ScriptRepository) {
        val preferences = BrowserPreferences(context.getSharedPreferences("browser", Context.MODE_PRIVATE))
        update(context, repository, RemoteSearchValues.fulltextAllowed && preferences.fulltextExpansion, true)
    }
    fun enabled(repository: ScriptRepository): Boolean = repository.find(BuiltinScripts.OFFICIAL_FULLTEXT_KEY)?.enabled
        ?: (ScriptExecutionCache.get(BuiltinScripts.FULLTEXT_ID)?.enabled == true)

    fun update(context: Context, repository: ScriptRepository, enabled: Boolean, startup: Boolean = false) {
        val installed = repository.find(BuiltinScripts.OFFICIAL_FULLTEXT_KEY)
        if (installed != null) {
            if (!startup) repository.enable(installed.id, enabled)
        } else if (!ScriptExecutionCache.enabled(BuiltinScripts.FULLTEXT_ID, enabled) && enabled) {
            ScriptExecutionCache.put(BuiltinScripts.fulltext(context))
        }
    }
}
