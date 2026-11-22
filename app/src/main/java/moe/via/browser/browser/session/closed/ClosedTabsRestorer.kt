package moe.via.browser.browser.session.closed

import android.content.Context
import android.os.Handler
import android.os.Looper
import moe.via.browser.browser.session.data.TabRepository
import moe.via.browser.browser.session.TabSnapshot
import moe.via.browser.browser.session.data.SessionTabPersistence
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.core.concurrent.BrowserExecutors
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

/** ua.F1 emits each restored tab before completion selects the originally calculated index. */
class ClosedTabsRestorer(context: Context) : AutoCloseable {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val tasks = HashSet<FutureTask<Unit>>()
    @Volatile private var closed = false
    fun restore(ids: List<String>, selected: String?, countBefore: Int, indexBefore: Int,
        opened: (TabSnapshot) -> Unit, select: (Int) -> Unit) {
        if (ids.isEmpty()) return
        val entries = TabRepository(app).use { repository -> ids.mapNotNull(repository::get) }
        if (entries.isEmpty()) return
        var chosen = -1
        entries.forEachIndexed { index, entry -> if (selected != null && selected == entry.id) chosen = index }
        val changeSelection = countBefore <= 0 || indexBefore < 0 || chosen >= 0
        if (chosen < 0) chosen = entries.lastIndex
        val target = countBefore + chosen.coerceIn(0, entries.lastIndex)
        val task = object : FutureTask<Unit>(Callable {
            try {
                TabRepository(app).use { repository ->
                    for (entry in entries) {
                        if (Thread.currentThread().isInterrupted) return@Callable Unit
                        val value = repository.materialize(entry)
                        val snapshot = SessionTabPersistence.snapshot(value.entry, value.state)
                        main.post { if (!closed) opened(snapshot) }
                    }
                }
                main.post { if (!closed && changeSelection) select(target) }
            } catch (error: Exception) { main.post { if (!closed) NativeLog.warn(null, error) } }
            Unit
        }) { override fun done() { synchronized(tasks) { tasks.remove(this) } } }
        synchronized(tasks) { if (closed) return; tasks.add(task) }
        BrowserExecutors.io.execute(task)
    }
    /** z8.w2.j keeps comma ordering, empty-item omission and the base command's no-op meaning. */
    fun restoreLink(url: String, countBefore: Int, indexBefore: Int, opened: (TabSnapshot) -> Unit, select: (Int) -> Unit) {
        val marker = url.indexOf("/restore?ids=")
        if (marker < 0) return
        val selectedAt = url.indexOf("&selected=")
        val raw = if (selectedAt < 0) url.substring(marker + 13) else url.substring(marker + 13, selectedAt)
        val ids = if (raw.isEmpty()) listOf("") else raw.split(',').dropLastWhile(String::isEmpty)
        restore(ids, if (selectedAt < 0) null else url.substring(selectedAt + 10), countBefore, indexBefore, opened, select)
    }
    override fun close() {
        val pending: List<FutureTask<Unit>>
        synchronized(tasks) { closed = true; pending = tasks.toList(); tasks.clear() }
        pending.forEach { it.cancel(true) }; main.removeCallbacksAndMessages(null)
    }
}
