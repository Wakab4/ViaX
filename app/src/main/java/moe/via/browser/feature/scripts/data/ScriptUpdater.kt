package moe.via.browser.feature.scripts.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.scripts.data.update.NativeScriptUpdateBatch
import moe.via.browser.feature.scripts.data.update.NativeScriptUpdatePreferences
import moe.via.browser.feature.scripts.data.update.NativeScriptUpdateWorker
import moe.via.browser.feature.scripts.model.ScriptUpdateTarget
import moe.via.browser.feature.scripts.model.UserScript
import moe.via.browser.feature.scripts.runtime.UserScriptEngine

/** Manual callbacks and the original automatic batch share an ID-only update worker. */
class ScriptUpdater private constructor(context: Context) {
    private val repository = UserScriptEngine.get(context).repository
    private val preferences = NativeScriptUpdatePreferences(context)
    private val worker = NativeScriptUpdateWorker(repository)
    private val automatic = NativeScriptUpdateBatch(repository, worker, preferences)
    private val main = Handler(Looper.getMainLooper())

    fun update(script: UserScript, completed: (Result<Boolean>) -> Unit = {}) {
        val target = ScriptUpdateTarget.from(script)
        BrowserExecutors.io.execute {
            val result = try {
                Result.success(worker.update(target, automatic = false))
            } catch (error: Exception) { Result.failure(error) }
            main.post { completed(result) }
        }
    }
    fun maybeUpdate() = automatic.start()
    companion object {
        @Volatile private var instance: ScriptUpdater? = null
        fun get(context: Context) = instance ?: synchronized(this) { instance ?: ScriptUpdater(context.applicationContext).also { instance = it } }
    }
}
