package moe.via.browser.feature.scripts.runtime

import android.content.Context
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.scripts.data.ScriptExecutionCache
import moe.via.browser.feature.scripts.data.ScriptProvider
import moe.via.browser.feature.scripts.model.ScriptExecution
import moe.via.browser.storage.preferences.BrowserPreferences

/** Owns the script execution cache before a WebView starts loading frames. */
class UserScriptEngine private constructor(context: Context) {
    private val app = context.applicationContext
    private val preferences = BrowserPreferences(app.getSharedPreferences("browser", Context.MODE_PRIVATE))
    val repository = ScriptProvider.get(app).repository
    val secret: String = java.util.UUID.randomUUID().toString()
    private val api by lazy { GmApiBuilder(app) }
    val enabled get() = preferences.scriptsEnabled

    init {
        try {
            ScriptExecutionCache.replace(repository.executionRecords())
        } catch (error: Exception) { moe.via.browser.core.logging.NativeLog.warn("Scripts", "Initialize execution cache", error) }
        BrowserExecutors.io.execute {
            try {
                NativeFulltextScript.initialize(app, repository)
            } catch (error: Exception) { moe.via.browser.core.logging.NativeLog.warn("Scripts", "Initialize fulltext script", error) }
        }
    }
    fun matching(url: String, phase: Int? = null): List<ScriptExecution> {
        if (!enabled || url.isEmpty() || url.startsWith("file://")) return emptyList()
        return ScriptExecutionCache.all().filter { it.enabled && it.applies(url) && (phase == null || it.runAt and phase != 0) }
    }
    internal fun execution(identifier: String): ScriptExecution? = ScriptExecutionCache.get(identifier)
    internal fun scripts(): List<ScriptExecution> = ScriptExecutionCache.all()
    internal fun observeScripts(listener: () -> Unit): AutoCloseable = ScriptExecutionCache.observe(listener)
    fun assemble(script: ScriptExecution, beforeContent: String? = null): String {
        val dependencies = script.requires.map { repository.resourceText(it) ?: "null" }
        return buildString {
            if (!script.unwrap) append("(function(){\n")
            append(api.build(script, secret)); append('\n')
            if (beforeContent != null) { append(beforeContent); append('\n') }
            dependencies.forEach { append(it); append("\n\n") }
            append(script.content)
            if (!script.unwrap) append("\n})();")
        }
    }
    companion object {
        @Volatile private var instance: UserScriptEngine? = null
        fun get(context: Context) = instance ?: synchronized(this) { instance ?: UserScriptEngine(context.applicationContext).also { instance = it } }
    }
}
