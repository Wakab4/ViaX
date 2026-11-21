package moe.pure.browser.feature.download

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors
import moe.pure.browser.feature.download.service.DownloadServiceCommands

/** One application-scoped queue owns transfer jobs; activities only observe snapshots. */
class DownloadCoordinator private constructor(private val context: Context) {
    interface Events {
        fun taskChanged(task: DownloadTask, previous: DownloadTask?)
        fun activityChanged(busy: Boolean)
    }
    private val repository = DownloadRepository(context)
    private val files = DownloadFiles(context)
    private val network = DownloadNetwork(context)
    private val payloads by lazy { DownloadPayloads.get(context) }
    private val commands = Executors.newSingleThreadExecutor()
    private val transfers = Executors.newFixedThreadPool(MAX_ACTIVE)
    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    private val events = CopyOnWriteArraySet<Events>()
    private val rows = linkedMapOf<Long, DownloadTask>()
    private val jobs = mutableMapOf<Long, DownloadControl>()
    private val restarts = mutableSetOf<Long>()
    private val pendingDelete = mutableMapOf<Long, Boolean>()
    private val measurements = mutableMapOf<Long, Pair<Long, Long>>()
    @Volatile var snapshot: List<DownloadTask> = emptyList(); private set
    @Volatile var busy = false; private set
    private val deliver = Runnable { listeners.forEach { it() } }
    init { command {
        repository.recoverInterrupted()
        repository.all().filter { it.state == DownloadState.PUBLISHING }.forEach { task ->
            runCatching { files.publish(task); repository.complete(task.id) }.onFailure { repository.state(task.id, DownloadState.FAILED, it.message) }
        }
        repository.all().forEach { rows[it.id] = it }; publish()
    } }
    fun observe(listener: () -> Unit) { listeners.add(listener); main.post(listener) }
    fun removeObserver(listener: () -> Unit) { listeners.remove(listener) }
    fun observeEvents(listener: Events) { events.add(listener) }
    fun removeEvents(listener: Events) { events.remove(listener) }
    fun enqueue(url: String, name: String, mime: String, headers: Map<String, String>, payload: String? = null, length: Long = -1, completed: (Result<Long>) -> Unit) = command {
        val result = runCatching {
            val created = files.create(name, mime)
            val id = try { repository.add(DownloadTask(url = url, name = created.name, target = created.uri.toString(), mime = mime, headers = headers, payload = payload, total = length, connections = if (payload == null) 8 else 1)) }
                catch (error: Exception) { runCatching { files.delete(DownloadTask(url = url, name = created.name, target = created.uri.toString())) }; throw error }
            refresh(id); publish(); id
        }
        main.post { completed(result) }
    }
    fun requestStart(id: Long, restart: Boolean = false) {
        DownloadServiceCommands.send(context, if (restart) DownloadServiceCommands.REDOWNLOAD else DownloadServiceCommands.RESUME, id)
    }
    fun requestPause(id: Long) = DownloadServiceCommands.send(context, DownloadServiceCommands.PAUSE, id)
    fun requestNetworkResume() = command {
        if (rows.values.any { it.state == DownloadState.QUEUED || it.state == DownloadState.WAITING_NETWORK })
            main.post { DownloadServiceCommands.send(context, DownloadServiceCommands.PAUSE_ALL) }
    }
    fun resumePending(completed: (Boolean) -> Unit) = command { drain(); publish(); main.post { completed(busy) } }
    fun start(id: Long, restart: Boolean, completed: (Boolean) -> Unit = {}) = command {
        var accepted = false
        try {
            val task = repository.get(id)
            accepted = id in jobs || task != null
            if (id !in jobs && task != null) {
                if (restart) {
                    files.delete(task)
                    createDestination(task)
                    repository.resetTransfer(id)
                    restarts.add(id)
                }
                if (restart || task.state !in setOf(DownloadState.QUEUED, DownloadState.WAITING_NETWORK)) {
                    repository.state(id, DownloadState.QUEUED); refresh(id)
                }
            }
            if (accepted && id !in jobs) drain()
        } catch (error: Exception) {
            repository.state(id, DownloadState.FAILED, error.message); refresh(id)
        } finally {
            publish(); main.post { completed(accepted) }
        }
    }
    fun resumeAfterDirectory(id: Long, restart: Boolean) = command {
        try {
            check(id !in jobs) { "请先暂停下载" }
            val task = repository.get(id) ?: return@command
            val accessible = DownloadDirectories(context).hasAccess(android.net.Uri.parse(task.target)) && files.exists(task)
            if (!accessible) {
                createDestination(task)
                repository.state(id, DownloadState.PAUSED); refresh(id); publish()
            }
            main.post { requestStart(id, restart && accessible) }
        } catch (error: Exception) { repository.state(id, DownloadState.FAILED, error.message); refresh(id); publish() }
    }
    private fun createDestination(task: DownloadTask) {
        val created = files.create(task.name, task.mime)
        try { repository.retarget(task.id, created.name, created.uri.toString()) }
        catch (error: Exception) { runCatching { files.delete(task.copy(target = created.uri.toString())) }; throw error }
    }
    fun pause(id: Long, completed: () -> Unit = {}) = command {
        val control = jobs[id]
        if (control != null) control.pause()
        else repository.get(id)?.takeIf { it.state != DownloadState.COMPLETE && it.state != DownloadState.FAILED }?.let {
            repository.state(id, DownloadState.PAUSED); refresh(id); drain()
        }
        publish(); main.post(completed)
    }
    fun cancel(id: Long, completed: () -> Unit = {}) = command {
        repository.state(id, DownloadState.CANCELED); jobs[id]?.cancel(); refresh(id); publish(); main.post(completed)
    }
    fun pauseAll(completed: () -> Unit = {}) = command {
        rows.values.filter { it.state in setOf(DownloadState.QUEUED, DownloadState.RUNNING, DownloadState.WAITING_NETWORK) }
            .forEach {
                val control = jobs[it.id]
                if (control != null) control.pause() else { repository.state(it.id, DownloadState.PAUSED); refresh(it.id) }
            }
        publish(); main.post(completed)
    }
    fun remove(ids: Collection<Long>, deleteFiles: Boolean, completed: (Result<Unit>) -> Unit = {}) = removeByPolicy(ids, { deleteFiles }, completed)
    fun removeSelection(ids: Collection<Long>, deleteCompletedFiles: Boolean, completed: (Result<Unit>) -> Unit = {}) =
        removeByPolicy(ids, { it?.state != DownloadState.COMPLETE || deleteCompletedFiles }, completed)
    private fun removeByPolicy(ids: Collection<Long>, deleteFile: (DownloadTask?) -> Boolean, completed: (Result<Unit>) -> Unit) = command {
        val result = runCatching {
            ids.forEach { id ->
                val control = jobs[id]
                val file = deleteFile(repository.get(id))
                if (control == null) delete(id, file)
                else { pendingDelete[id] = file; control.cancel() }
            }
            publish()
        }
        main.post { completed(result) }
    }
    fun rename(id: Long, name: String, completed: (Result<Unit>) -> Unit) = command {
        val result = runCatching {
            check(id !in jobs) { "请先暂停下载" }
            val task = repository.get(id) ?: error("任务不存在")
            if (task.name.equals(name, true)) return@runCatching
            val target = files.rename(task, name)
            val dot = name.lastIndexOf('.')
            val mime = if (dot > 0) DownloadNames.mime(name) else "application/octet-stream"
            repository.retarget(id, files.name(target) ?: DownloadFiles.safeName(name), target, mime); refresh(id); publish()
        }
        main.post { completed(result) }
    }
    private fun drain() {
        // m5.i.o: queued90 precedes waiting95, newest first within each state; at most three.
        while (jobs.size < MAX_ACTIVE) {
            val next = rows.values.filter { it.state in setOf(DownloadState.QUEUED, DownloadState.WAITING_NETWORK) && it.id !in jobs }
                .minWithOrNull(compareBy<DownloadTask> { if (it.state == DownloadState.QUEUED) 0 else 1 }
                    .thenByDescending { it.created }) ?: break
            val control = DownloadControl(); jobs[next.id] = control
            val restart = restarts.remove(next.id)
            transfers.execute {
                try {
                    files.prepare(next)
                    val state = if (next.payload == null) DownloadWorker(repository, files.sink(next.target), control, network::available) { id -> command { refresh(id); publish() } }.run(next.id, restart)
                        else PayloadDownloadWorker(repository, payloads, files.sink(next.target), control) { id -> command { refresh(id); publish() } }.run(next.id, restart)
                    if (state == DownloadState.COMPLETE) {
                        repository.state(next.id, DownloadState.PUBLISHING)
                        repository.get(next.id)?.let(files::publish)
                        repository.complete(next.id)
                    }
                } catch (error: Exception) { repository.state(next.id, DownloadState.FAILED, "无法保存下载文件：${error.message}") }
                command {
                    jobs.remove(next.id)
                    pendingDelete.remove(next.id)?.let { runCatching { delete(next.id, it) } }
                    refresh(next.id)
                    // Native95 releases its slot without immediately resubmitting the disconnected queue.
                    if (rows[next.id]?.state != DownloadState.WAITING_NETWORK) drain()
                    publish()
                }
            }
        }
    }
    private fun delete(id: Long, removeFile: Boolean) {
        val task = repository.get(id)
        repository.remove(listOf(id)); rows.remove(id)
        task?.let { if (removeFile) runCatching { files.delete(it) }.onFailure { error -> moe.pure.browser.core.logging.NativeLog.warn("DownloadDelete", "Cannot remove target", error) } }
        task?.payload?.let(payloads::removeUnused)
        main.postDelayed({ DownloadNotifications.get(context).remove(id) }, 500)
    }
    private fun refresh(id: Long) {
        val task = repository.get(id)
        if (task == null) { rows.remove(id); measurements.remove(id); return }
        var speed = 0L
        if (task.state == DownloadState.RUNNING) {
            val now = android.os.SystemClock.elapsedRealtime()
            val before = measurements[id]
            if (before == null || now - before.first >= 100) {
                if (before != null) speed = (task.received - before.second).coerceAtLeast(0) * 1000 / (now - before.first)
                measurements[id] = now to task.received
            } else speed = rows[id]?.speed ?: 0
        } else measurements.remove(id)
        val previous = rows[id]
        val current = task.copy(state = if (id in jobs && task.state == DownloadState.COMPLETE) DownloadState.PUBLISHING else task.state, speed = speed)
        rows[id] = current
        if (current != previous) main.post { events.forEach { it.taskChanged(current, previous) } }
    }
    private fun publish() {
        snapshot = rows.values.sortedWith(compareByDescending<DownloadTask> { it.created }.thenByDescending { it.id })
        val active = jobs.isNotEmpty()
        val different = active != busy
        busy = active
        if (different) main.post { events.forEach { it.activityChanged(active) } }
        main.removeCallbacks(deliver); main.post(deliver)
    }
    private fun command(body: () -> Unit) { commands.execute { try { body() } catch (error: Exception) { moe.pure.browser.core.logging.NativeLog.error("DownloadQueue", "Queue command failed", error) } } }
    companion object {
        private const val MAX_ACTIVE = 3
        @Volatile private var current: DownloadCoordinator? = null
        fun get(context: Context): DownloadCoordinator = current ?: synchronized(this) { current ?: DownloadCoordinator(context.applicationContext).also { current = it } }
    }
}
