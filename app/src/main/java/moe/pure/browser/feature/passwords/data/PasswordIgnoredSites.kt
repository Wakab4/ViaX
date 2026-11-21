package moe.pure.browser.feature.passwords.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.passwords.model.PasswordSites
import java.io.Closeable
import java.io.File

/** Exact authority set. All public file operations are synchronous and belong on the persistence worker. */
class PasswordIgnoredSites(context: Context) : Closeable {
    private val app = context.applicationContext
    private val state by lazy {
        synchronized(STATES) { STATES.getOrPut(app.filesDir.absolutePath) {
            State(File(app.getExternalFilesDir("pass") ?: File(app.filesDir, "pass"), "ignored_sites.txt"))
        } }
    }
    private var accessed = false
    private var closed = false

    fun all(): List<String> = access { sites.sortedWith(PasswordSites.siteOrder) }
    fun contains(site: String): Boolean = access { key(site) in sites }
    fun add(site: String): Boolean = change {
        val key = key(site)
        if (key.isEmpty()) false else { if (sites.add(key)) dirty(); true }
    }
    fun remove(site: String): Boolean = change { if (sites.remove(key(site))) dirty(); true }
    fun replace(old: String, new: String): Boolean = change {
        val target = key(new)
        if (target.isEmpty()) false else {
            val removed = sites.remove(key(old)); val added = sites.add(target)
            if (removed || added) dirty()
            true
        }
    }
    fun flush() = access { write(); Unit }
    private fun key(value: String) = if (value.contains("://")) PasswordSites.authority(value) else value
    private fun <T> change(block: State.() -> T) = access(block)
    private fun <T> access(block: State.() -> T): T {
        check(!closed) { "Password ignored sites are closed" }
        val selected = state; accessed = true
        return synchronized(selected) { selected.load(); selected.block() }
    }
    override fun close() {
        if (closed) return
        if (accessed) runCatching { flush() }
        closed = true
    }

    private class State(private val file: File) {
        val sites = hashSetOf<String>()
        private var loaded = false
        private var changed = false
        private var generation = 0L
        private var pending: Runnable? = null
        fun load() {
            if (loaded) return
            val atomic = AtomicFile(file)
            if (file.exists() || File(file.path + ".bak").exists()) {
                atomic.openRead().bufferedReader(Charsets.UTF_8).use { reader ->
                    val entries = reader.readLines()
                    sites.addAll(entries)
                }
            }
            loaded = true
        }
        fun dirty() {
            changed = true
            val expected = ++generation
            pending?.let(MAIN::removeCallbacks)
            val task = Runnable {
                BrowserExecutors.persistence.execute {
                    synchronized(this) { if (expected == generation) { pending = null; runCatching { write() } } }
                }
            }
            pending = task; MAIN.postDelayed(task, 200)
        }
        fun write() {
            if (!changed) return
            check(file.parentFile?.let { it.mkdirs() || it.isDirectory } == true)
            val atomic = AtomicFile(file)
            val output = atomic.startWrite()
            try {
                output.write(sites.joinToString("\n", postfix = if (sites.isEmpty()) "" else "\n").toByteArray(Charsets.UTF_8))
                atomic.finishWrite(output); changed = false
            } catch (error: Exception) { atomic.failWrite(output); throw error }
        }
    }
    private companion object {
        val MAIN = Handler(Looper.getMainLooper())
        val STATES = hashMapOf<String, State>()
    }
}
