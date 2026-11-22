package moe.via.browser.feature.adblock.update

import android.content.Context
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.adblock.AdblockEngine
import moe.via.browser.feature.adblock.FilterSubscriptions
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** sb.g publishes the current nonempty list on normal completion before advancing its checkpoint. */
internal class NativeFilterUpdateBatch(private val context: Context, private val repository: FilterSubscriptions,
    private val preferences: NativeFilterUpdatePreferences) {
    fun start() {
        BrowserExecutors.io.execute {
            try {
                val cutoff = preferences.cutoff(System.currentTimeMillis()) ?: return@execute
                val targets = repository.all().filter { it.enabled && it.updated < cutoff }
                if (targets.isEmpty()) { complete(); return@execute }
                val remaining = AtomicInteger(targets.size)
                val failed = AtomicBoolean(false)
                targets.forEach { item -> BrowserExecutors.io.execute {
                    try { moe.via.browser.core.logging.NativeLog.debug("FilterUpdater", "Update ${item.url}: ${NativeFilterUpdateWorker.update(item)}") }
                    catch (error: Exception) { failed.set(true); moe.via.browser.core.logging.NativeLog.warn("FilterUpdater", "Automatic update: ${error.javaClass.simpleName}") }
                    if (remaining.decrementAndGet() == 0 && !failed.get()) complete()
                } }
            } catch (error: Exception) { moe.via.browser.core.logging.NativeLog.warn("FilterUpdater", "Read subscriptions: ${error.javaClass.simpleName}") }
        }
    }
    private fun complete() {
        if (repository.all().isEmpty()) return
        if (repository.publish()) AdblockEngine.changed(context)
        preferences.completedAt = System.currentTimeMillis()
    }
}
