package moe.pure.browser.feature.library.history

import android.os.Handler
import android.os.Looper
import moe.pure.browser.core.logging.NativeLog as Log
import moe.pure.browser.feature.library.LibraryEntry
import moe.pure.browser.feature.library.LibraryRepository
import java.util.concurrent.Executor

/** bb.v's model. Loading belongs to the view; no query generation, keyset, deduplication or retry is added. */
internal class HistoryPager(private val repository: LibraryRepository, private val io: Executor) : AutoCloseable {
    interface Observer {
        fun rows(values: List<LibraryEntry>)
        fun removed(values: List<LibraryEntry>, positions: List<Int>)
        fun backing(values: List<LibraryEntry>)
        fun hasMore(value: Boolean)
    }
    private val main = Handler(Looper.getMainLooper())
    var records: List<LibraryEntry>? = null
        private set
    var more: Boolean? = null
        private set
    private var offset = 0
    private var observer: Observer? = null
    private var closed = false
    fun attach(value: Observer) { observer = value; records?.let(value::rows); more?.let(value::hasMore) }
    fun detach(value: Observer) { if (observer === value) observer = null }
    fun open(position: Int?, query: String) {
        if (query.isNotEmpty()) search(query) else load(if (position == null) 120 else maxOf(120, position + 20))
    }
    fun load(count: Int = 120) = submit({ repository.historyPage(offset, count).rows }) { page ->
        if (page.isEmpty()) {
            if (records == null) emitRows(emptyList())
            emitMore(false)
        } else {
            val next = records.orEmpty() + page
            offset = next.size; emitRows(next); emitMore(page.size >= count)
        }
    }
    fun search(raw: String) {
        if (raw.isNotEmpty()) submit({ repository.historySearch(raw, 1024).rows }) { values -> emitMore(false); emitRows(values) }
        else { offset = 0; emitRows(emptyList()); load() }
    }
    fun removeOne(position: Int) {
        val next = records?.toMutableList() ?: return
        if (position !in next.indices) return
        val removed = next.removeAt(position); offset--; records = next
        submit({ repository.deleteHistory(longArrayOf(removed.numericId)) }) {}
        if (next.isEmpty()) emitRows(next) else observer?.removed(next, listOf(position))
    }
    fun removeSelected(positions: List<Int>, searching: Boolean) {
        val next = records?.toMutableList() ?: return
        if (positions.isEmpty() || positions.size > next.size) return
        val indices = positions.sortedDescending()
        val removed = indices.map { next.removeAt(it) }
        offset -= indices.size; records = next
        val reload = !searching && next.isEmpty()
        if (reload) {
            observer?.backing(next)
            emitMore(false)
            submit({ repository.deleteHistory(removed.map { it.numericId }.toLongArray()) }) { offset = 0; emitRows(emptyList()); emitMore(false); load() }
        } else {
            submit({ repository.deleteHistory(removed.map { it.numericId }.toLongArray()) }) {}
            if (next.isEmpty()) emitRows(next) else observer?.removed(next, indices)
        }
    }
    fun periods(callback: (List<HistoryPeriod>) -> Unit) = submit(repository::historyPeriodCounts, callback)
    fun clearSince(sinceSeconds: Long) = submit({ repository.clearHistory(sinceSeconds) }) {
        offset = 0; emitRows(emptyList()); emitMore(false); load()
    }
    private fun emitRows(value: List<LibraryEntry>) { records = value; observer?.rows(value) }
    private fun emitMore(value: Boolean) { more = value; observer?.hasMore(value) }
    private fun <T> submit(query: () -> T, completed: (T) -> Unit) {
        if (closed) return
        io.execute {
            try { val value = query(); main.post { if (!closed) completed(value) } }
            catch (error: Exception) { Log.warn("History", "History operation", error) }
        }
    }
    override fun close() { closed = true; observer = null; main.removeCallbacksAndMessages(null) }
}
