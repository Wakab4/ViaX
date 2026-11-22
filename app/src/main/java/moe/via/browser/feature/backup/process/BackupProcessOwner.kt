package moe.via.browser.feature.backup.process

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import moe.via.browser.core.files.NativeStreams
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.backup.archive.BackupArchive
import moe.via.browser.feature.backup.archive.PreparedBackup
import moe.via.browser.feature.backup.model.BackupData
import moe.via.browser.feature.backup.model.BackupItemResult
import moe.via.browser.feature.backup.model.BackupResult
import moe.via.browser.feature.backup.ui.BackupDocumentState
import moe.via.browser.feature.passwords.data.PasswordRepository
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

/** Owns real IO and prepared files. The only window reference is a detachable weak observer. */
internal class BackupProcessOwner(context: Context, private val legacyImport: ((String) -> Int)?) {
    fun interface Observer { fun changed(snapshot: BackupSnapshot) }
    private sealed class ImportSource {
        class Archive(val prepared: PreparedBackup) : ImportSource()
        class Legacy(val count: Int) : ImportSource()
    }
    private val app = context.applicationContext
    private val archive = BackupArchive(app)
    private val main = Handler(Looper.getMainLooper())
    val token = UUID.randomUUID().toString()
    val documents = BackupDocumentState(app)
    var selection: Set<BackupData> = BackupData.DEFAULT.toSet()
        private set
    var phase = BackupPhase.IDLE
        private set
    var closed = false
        private set
    private var revision = 0L
    private var observer: WeakReference<Observer>? = null
    private var draft: CharArray? = null
    private var password: CharArray? = null
    private var prepared: PreparedBackup? = null
    private var work: FutureTask<*>? = null
    private var outcome: BackupOutcome? = null
    private var importNotified = false
    private var documentLaunched = false

    init { BackupProcessRegistry.register(this) }
    fun attach(value: Observer) { if (!closed) { observer = WeakReference(value); value.changed(snapshot()) } }
    fun detach(value: Observer) { if (observer?.get() === value) observer = null }
    fun snapshot() = BackupSnapshot(phase, revision, selection, outcome)
    private fun publish(next: BackupPhase) {
        if (closed) return
        if (next != phase && next in setOf(BackupPhase.EXPORT_DOCUMENT, BackupPhase.IMPORT_DOCUMENT)) documentLaunched = false
        phase = next; revision++
        observer?.get()?.changed(snapshot())
    }
    private fun available() = !closed && work == null && phase in setOf(BackupPhase.IDLE, BackupPhase.RESULT)
    fun exportSelection() {
        if (!available()) return
        clearSecrets(); discardPrepared(); outcome = null
        selection = BackupData.DEFAULT.toSet(); publish(BackupPhase.SELECTION)
    }
    fun recordSelection(value: Set<BackupData>) { if (!closed) selection = value.toSet() }
    fun beginExport(value: Set<BackupData>) {
        if (closed || phase != BackupPhase.SELECTION) return
        recordSelection(value)
        if (selection.isEmpty()) { cancel(); return }
        if (BackupData.PASSWORDS !in selection) { publish(BackupPhase.EXPORT_DOCUMENT); return }
        worker(BackupPhase.COUNTING_PASSWORDS, { PasswordRepository(app).use { it.count() } }) { count ->
            if (count == 0) {
                selection = selection - BackupData.PASSWORDS
                publish(if (selection.isEmpty()) BackupPhase.IDLE else BackupPhase.EXPORT_DOCUMENT)
            } else publish(BackupPhase.AUTHENTICATION)
        }
    }
    fun authenticated(allowed: Boolean) {
        if (!closed && phase == BackupPhase.AUTHENTICATION) {
            if (allowed) publish(BackupPhase.EXPORT_PASSWORD) else cancel()
        }
    }
    fun importDocument() {
        if (!available()) return
        clearSecrets(); discardPrepared(); outcome = null; publish(BackupPhase.IMPORT_DOCUMENT)
    }
    fun recordPassword(value: CharArray) {
        if (!closed && phase in setOf(BackupPhase.EXPORT_PASSWORD, BackupPhase.IMPORT_PASSWORD)) {
            draft?.fill('\u0000'); draft = value.copyOf()
        }
    }
    fun passwordDraft(): CharArray = draft?.copyOf() ?: CharArray(0)
    fun launchDocument(): Boolean {
        if (closed || documentLaunched || phase !in setOf(BackupPhase.EXPORT_DOCUMENT, BackupPhase.IMPORT_DOCUMENT)) return false
        documentLaunched = true; return true
    }
    fun acceptPassword(value: CharArray) {
        if (closed || phase !in setOf(BackupPhase.EXPORT_PASSWORD, BackupPhase.IMPORT_PASSWORD)) return
        password?.fill('\u0000'); password = value.copyOf(); draft?.fill('\u0000'); draft = null
        if (phase == BackupPhase.EXPORT_PASSWORD) publish(BackupPhase.EXPORT_DOCUMENT) else importPrepared()
    }
    fun exportTarget(uri: Uri, path: String?) {
        if (closed || phase != BackupPhase.EXPORT_DOCUMENT || work != null) return
        val selected = selection.toSet()
        val secret = password; password = null
        if (BackupData.PASSWORDS in selected && secret == null) {
            BrowserExecutors.persistence.execute { documents.finish(uri, false) }
            fail("导出已中断，请重试"); return
        }
        worker(BackupPhase.EXPORTING, {
            try {
                val output = app.contentResolver.openOutputStream(uri, "wt") ?: error("无法创建备份文件")
                val result = try { archive.exportTo(output, selected, secret) } finally { NativeStreams.close(output) }
                documents.finish(uri, true); result
            } catch (error: Exception) { documents.finish(uri, false); throw error }
            finally { secret?.fill('\u0000') }
        }) { result -> finish(BackupOutcome(result, exporting = true, path = path)) }
    }
    fun importTarget(uri: Uri) {
        if (closed || phase != BackupPhase.IMPORT_DOCUMENT || work != null) return
        worker(BackupPhase.PREPARING_IMPORT, {
            val input = app.contentResolver.openInputStream(uri) ?: error("无法打开备份文件")
            input.buffered().use { stream ->
                stream.mark(512)
                val probe = ByteArray(256); val count = stream.read(probe); stream.reset()
                val plain = count > 0 && String(probe, 0, count, Charsets.UTF_8).trimStart('\uFEFF', ' ', '\r', '\n', '\t').startsWith('{')
                if (plain && legacyImport != null) {
                    val buffer = ByteArray(8192); val output = java.io.ByteArrayOutputStream()
                    while (true) {
                        val size = stream.read(buffer); if (size < 0) break
                        check(output.size() + size <= 64 * 1024 * 1024) { "备份文件过大" }; output.write(buffer, 0, size)
                    }
                    ImportSource.Legacy(legacyImport.invoke(output.toString("UTF-8").trimStart('\uFEFF')))
                } else ImportSource.Archive(archive.prepare(stream))
            }
        }, abandoned = { source -> if (source is ImportSource.Archive) discard(source.prepared) }) { source ->
            when (source) {
                is ImportSource.Archive -> {
                    prepared = source.prepared
                    if (source.prepared.encrypted) publish(BackupPhase.IMPORT_PASSWORD) else importPrepared()
                }
                is ImportSource.Legacy -> finish(BackupOutcome(BackupResult(listOf(BackupItemResult(BackupData.SETTINGS, source.count))), imported = true))
            }
        }
    }
    private fun importPrepared() {
        val source = prepared ?: return
        prepared = null
        val secret = password; password = null
        worker(BackupPhase.IMPORTING, {
            try { source.use { archive.importFrom(it, secret, legacyImport) } }
            finally { secret?.fill('\u0000') }
        }) { result -> finish(BackupOutcome(result, imported = true)) }
    }
    private fun finish(value: BackupOutcome) { clearSecrets(); outcome = value; importNotified = false; publish(BackupPhase.RESULT) }
    fun consumeImportNotification(): Boolean {
        if (outcome?.imported != true || importNotified || closed) return false
        importNotified = true; return true
    }
    fun acknowledge() { if (phase == BackupPhase.RESULT && !closed) { outcome = null; publish(BackupPhase.IDLE) } }
    fun fail(message: String = "操作失败") { if (!closed) { clearSecrets(); discardPrepared(); finish(BackupOutcome(failure = message)) } }
    fun cancel() {
        if (closed || work != null) return
        clearSecrets(); discardPrepared(); documents.pendingName = null; outcome = null; publish(BackupPhase.IDLE)
    }
    private fun <T> worker(next: BackupPhase, action: () -> T, abandoned: ((T) -> Unit)? = null, completed: (T) -> Unit) {
        if (closed || work != null) return
        val task = object : FutureTask<T>(Callable(action)) {
            override fun done() {
                val result = runCatching { get() }.recoverCatching { error -> throw (if (error is ExecutionException) error.cause ?: error else error) }
                main.post {
                    if (work === this) work = null
                    if (closed) {
                        result.onSuccess { abandoned?.invoke(it) }
                        BrowserExecutors.persistence.execute(documents::discard)
                    } else result.onSuccess(completed).onFailure { fail() }
                }
            }
        }
        work = task; publish(next); BrowserExecutors.persistence.execute(task)
    }
    private fun clearSecrets() { draft?.fill('\u0000'); draft = null; password?.fill('\u0000'); password = null }
    private fun discard(value: PreparedBackup) { BrowserExecutors.persistence.execute { value.close() } }
    private fun discardPrepared() { val value = prepared; prepared = null; if (value != null) discard(value) }
    fun close() {
        if (closed) return
        closed = true; observer = null; clearSecrets(); discardPrepared(); outcome = null
        BackupProcessRegistry.remove(this)
        // An already queued/running write completes its own stream/finally cleanup; no partial import is rolled back.
        if (work == null) BrowserExecutors.persistence.execute(documents::discard)
    }
}
