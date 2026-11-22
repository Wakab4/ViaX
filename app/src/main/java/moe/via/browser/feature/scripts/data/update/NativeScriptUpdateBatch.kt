package moe.via.browser.feature.scripts.data.update

import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.scripts.data.ScriptRepository
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** sb.q runs eligible entries concurrently and checkpoints only a normally completed stream. */
internal class NativeScriptUpdateBatch(private val repository: ScriptRepository,
    private val worker: NativeScriptUpdateWorker, private val preferences: NativeScriptUpdatePreferences) {
    fun start() {
        BrowserExecutors.io.execute {
            try {
                val cutoff = preferences.cutoff(System.currentTimeMillis()) ?: return@execute
                val targets = repository.updateTargets().filter { it.enabled && !it.url.isNullOrEmpty() && it.updated < cutoff }
                if (targets.isEmpty()) { preferences.completedAt = System.currentTimeMillis(); return@execute }
                val remaining = AtomicInteger(targets.size)
                val failed = AtomicBoolean(false)
                targets.forEach { target -> BrowserExecutors.io.execute {
                    try {
                        val success = worker.update(target, automatic = true)
                        moe.via.browser.core.logging.NativeLog.debug("ScriptUpdater", "Update ${target.name}: $success")
                    } catch (error: Exception) {
                        failed.set(true)
                        moe.via.browser.core.logging.NativeLog.warn("ScriptUpdater", "Automatic update: ${error.javaClass.simpleName}")
                    }
                    if (remaining.decrementAndGet() == 0 && !failed.get()) preferences.completedAt = System.currentTimeMillis()
                } }
            } catch (error: Exception) { moe.via.browser.core.logging.NativeLog.warn("ScriptUpdater", "Read automatic targets: ${error.javaClass.simpleName}") }
        }
    }
}
