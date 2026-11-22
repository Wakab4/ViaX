package moe.via.browser.feature.sync.sync

import android.util.AtomicFile
import moe.via.browser.feature.backup.model.ImportMode
import moe.via.browser.feature.sync.data.SyncRepository
import moe.via.browser.feature.sync.model.*
import moe.via.browser.feature.sync.network.SyncNetworkException
import moe.via.browser.feature.sync.network.SyncOperation
import moe.via.browser.feature.sync.network.WebDavTransport
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/** Native sequential file strategies, with profile isolation and cancellation between committed datasets. */
internal class SyncCoordinator(private val repository: SyncRepository, private val adapter: SyncDataAdapter) {
    fun run(profile: WebDavProfile, mode: SyncMode, force: Boolean, operation: SyncOperation, progress: (SyncProgress) -> Unit): SyncReport {
        val key = profile.id + ':' + profile.baseUrl + ':' + profile.username + ':' + profile.remotePath
        val lock = LOCKS.getOrPut(key) { ReentrantLock() }
        while (!lock.tryLock(200, TimeUnit.MILLISECONDS)) operation.check()
        try {
            operation.check()
            if (!force && System.currentTimeMillis() - (SUCCEEDED[key] ?: 0) < 180_000) return SyncReport(skipped = true)
            val credentials = syncLocal(operation) { repository.webDavCredentials(profile) }
            val transport = WebDavTransport(profile, credentials, operation)
            val states = syncLocal(operation) { repository.index(profile) }
            val items = mutableListOf<SyncItemResult>()
            for ((index, data) in profile.data.withIndex()) {
                operation.check()
                var errorCode = if (mode == SyncMode.PUSH_LOCAL) 201 else if (mode == SyncMode.PULL_REMOTE) 301 else 101
                fun emit(phase: SyncPhase) = progress(SyncProgress(data, phase, index, profile.data.size))
                val path = profile.remotePath.trim('/') .let { if (it.isEmpty()) data.fileName else "$it/${data.fileName}" }
                fun state() = states.getOrPut(path) { SyncFileState(profile.id, path) }
                fun pull(importMode: ImportMode) {
                    emit(SyncPhase.DOWNLOADING)
                    val bytes = transport.download(path)
                    val baseline = repository.baseline(profile, path, remote = true)
                    syncLocal(operation) { write(baseline, bytes) }
                    emit(SyncPhase.IMPORTING)
                    syncLocal(operation) { adapter.importData(data, bytes.toString(Charsets.UTF_8), importMode, state().lastSynced) }
                }
                fun remoteModified(): Long = try { transport.modified(path) }
                    catch (_: IOException) { operation.check(); 0 }
                fun push(forced: Boolean) {
                    errorCode = 201; emit(SyncPhase.EXPORTING)
                    val text = syncLocal(operation) { adapter.export(data) } ?: return
                    val bytes = text.toByteArray(Charsets.UTF_8)
                    val outgoing = repository.baseline(profile, path, remote = false)
                    val baseline = repository.baseline(profile, path, remote = true)
                    val unchanged = syncLocal(operation) {
                        write(outgoing, bytes)
                        !forced && baseline.isFile && baseline.readBytes().contentEquals(bytes)
                    }
                    if (unchanged) return
                    emit(SyncPhase.UPLOADING); transport.upload(path, bytes, data.mime)
                    syncLocal(operation) { write(baseline, bytes) }
                    val modified = remoteModified()
                    states[path] = state().copy(remoteModified = modified, lastSynced = modified)
                }
                try {
                    when (mode) {
                        SyncMode.PULL_REMOTE -> pull(ImportMode.REPLACE)
                        // Native force applies only to bookmark push in this manual mode.
                        SyncMode.PUSH_LOCAL -> push(data == SyncData.BOOKMARKS)
                        SyncMode.MERGE -> {
                            emit(SyncPhase.CHECKING)
                            val modified = remoteModified()
                            states[path] = state().copy(remoteModified = modified)
                            if (modified > state().lastSynced && modified != 0L)
                                pull(if (data == SyncData.SETTINGS || state().lastSynced == 0L) ImportMode.MERGE else ImportMode.RECONCILE)
                            if (modified == 0L && data != SyncData.BOOKMARKS) syncLocal(operation) {
                                AtomicFile(repository.baseline(profile, path, remote = true)).delete()
                            }
                            push(data == SyncData.BOOKMARKS && modified == 0L)
                        }
                    }
                    items += SyncItemResult(data, true)
                } catch (error: Exception) {
                    operation.check()
                    val message = when (error) {
                        is SyncNetworkException -> error.message ?: "连接失败。"
                        else -> "无法处理${data.fileName}。"
                    }
                    items += SyncItemResult(data, false, errorCode,
                        (if (errorCode == 201) "Cannot push: " else if (errorCode == 301) "Cannot pull: " else "Cannot merge: ") + message)
                }
                progress(SyncProgress(data, SyncPhase.COMPLETE, index + 1, profile.data.size))
            }
            operation.check()
            if (items.all { it.success }) {
                syncLocal(operation) { repository.commitIndex(profile, states) }
                SUCCEEDED[key] = System.currentTimeMillis()
            }
            return SyncReport(items)
        } finally { lock.unlock() }
    }
    private fun write(file: File, bytes: ByteArray) {
        check(file.parentFile?.let { it.mkdirs() || it.isDirectory } == true)
        val atomic = AtomicFile(file); val output = atomic.startWrite()
        try { output.write(bytes); atomic.finishWrite(output) }
        catch (error: Exception) { atomic.failWrite(output); throw error }
    }
    private companion object {
        val LOCKS = ConcurrentHashMap<String, ReentrantLock>()
        val SUCCEEDED = ConcurrentHashMap<String, Long>()
    }
}
