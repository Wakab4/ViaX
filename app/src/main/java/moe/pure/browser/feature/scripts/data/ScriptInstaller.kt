package moe.pure.browser.feature.scripts.data

import moe.pure.browser.ui.message.BrowserMessages

import android.content.Context
import android.os.Handler
import android.os.Looper
import moe.pure.browser.core.logging.NativeLog as Log
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.scripts.model.UserScript
import moe.pure.browser.feature.scripts.runtime.UserScriptEngine

/** A missing dependency leaves the installed script available for editing and retrying. */
object ScriptInstaller {
    fun save(context: Context, repository: ScriptRepository, script: UserScript, targetId: Long? = null): Long {
        val app = context.applicationContext
        UserScriptEngine.get(app)
        val id = repository.save(script, targetId)
        if (script.requires.isNotEmpty() || script.resources.isNotEmpty()) BrowserExecutors.io.execute {
            val result = runCatching { ScriptRepository(app).use { ScriptAssetLoader().prepare(script.key, it) } }
            if (!result.getOrDefault(false)) Handler(Looper.getMainLooper()).post {
                BrowserMessages.show(app, "下载 \"${script.name}\" 依赖失败！", BrowserMessages.LONG)
            }
        }
        return id
    }

    /** m9.l/l9.h restore and sa.d1.Q3 completion enqueue the same all-pattern dependency sweep. */
    fun prepareAllDependencies(context: Context) {
        val app = context.applicationContext
        UserScriptEngine.get(app)
        BrowserExecutors.io.execute {
            val result = runCatching {
                ScriptRepository(app).use { repository ->
                    ScriptAssetLoader().prepareRestored(ScriptExecutionCache.all(), repository)
                }
            }
            result.onSuccess { report ->
                if (report.failed > 0) Log.warn("Scripts", "Script dependencies incomplete: ${report.failed}/${report.references} references")
            }.onFailure { error -> Log.warn("Scripts", "Script dependency sweep failed: ${error.javaClass.simpleName}") }
        }
    }
}
