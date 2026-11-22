package moe.via.browser.feature.scripts.data

import android.os.Handler
import android.os.Looper
import moe.via.browser.feature.scripts.model.ScriptExecution
import moe.via.browser.feature.scripts.model.UserScript
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/** q5.a keeps one process list: bulk DB order, in-place replacement by id, append for a new id. */
internal object ScriptExecutionCache {
    private val records = ArrayList<ScriptExecution>()
    private val lock = ReentrantReadWriteLock()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val main = Handler(Looper.getMainLooper())
    fun all(): List<ScriptExecution> = lock.readLock().withLock { records.toList() }
    fun get(id: Long): ScriptExecution? = lock.readLock().withLock { records.firstOrNull { it.id == id } }
    fun get(key: String): ScriptExecution? = lock.readLock().withLock { records.firstOrNull { it.key == key } }
    fun observe(listener: () -> Unit): AutoCloseable {
        listeners.add(listener)
        return AutoCloseable { listeners.remove(listener) }
    }
    private fun changed() { main.post { listeners.forEach { it() } } }
    fun replace(scripts: List<UserScript>) {
        lock.writeLock().withLock {
            records.clear()
            scripts.forEach { records.add(ScriptExecution(it)) }
        }
        changed()
    }
    fun put(script: UserScript): ScriptExecution {
        val execution = ScriptExecution(script)
        lock.writeLock().withLock {
            val index = records.indexOfFirst { it.id == script.id }
            if (index < 0) records.add(execution) else records[index] = execution
        }
        changed()
        return execution
    }
    fun remove(id: Long) {
        lock.writeLock().withLock { records.removeAll { it.id == id } }
        changed()
    }
    fun enabled(id: Long, value: Boolean): Boolean {
        val found = lock.writeLock().withLock {
            val entry = records.firstOrNull { it.id == id } ?: return@withLock false
            entry.enabled = value
            true
        }
        if (found) changed()
        return found
    }
}
