package moe.pure.browser.feature.sync.sync

import android.content.Context
import android.os.Handler
import android.os.Looper
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.sync.data.SyncRepository
import moe.pure.browser.feature.sync.model.*
import moe.pure.browser.feature.sync.network.SyncNetworkException
import moe.pure.browser.feature.sync.network.SyncOperation
import moe.pure.browser.feature.sync.network.ViaCloudClient
import java.io.Closeable
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/** No constructor traffic. Only explicit calls and the browser-resume hook can start a request. */
class SyncManager(context: Context, private val adapter: SyncDataAdapter = BrowserSyncDataAdapter(context)) : Closeable {
    private val app = context.applicationContext
    private val repository = SyncRepository(app)
    private val main = Handler(Looper.getMainLooper())
    private val closed = AtomicBoolean()
    private val operations = ConcurrentHashMap.newKeySet<SyncOperation>()

    fun sync(
        mode: SyncMode = SyncMode.MERGE, force: Boolean = true,
        progress: (SyncProgress) -> Unit = {}, complete: (SyncReport) -> Unit
    ): SyncCall = execute<SyncReport>({ result -> complete(result.getOrElse { SyncReport(error = safeError(it).message) }) }) { operation ->
        webDav(mode, force, automatic = false, operation = operation, progress = progress)
    }

    /** Foreground lifecycle trigger only; no service, scheduled job or background polling is registered. */
    fun onBrowserResume(complete: (SyncReport) -> Unit = {}): SyncCall =
        execute<SyncReport>({ result -> complete(result.getOrElse { SyncReport(error = safeError(it).message) }) }) { operation ->
            webDav(SyncMode.MERGE, force = false, automatic = true, operation = operation) {}
        }

    private fun webDav(
        mode: SyncMode, force: Boolean, automatic: Boolean, operation: SyncOperation, progress: (SyncProgress) -> Unit
    ): SyncReport {
        val profile = syncLocal(operation) { repository.webDav() }
        if (profile == null || !profile.configured) return if (automatic) SyncReport(skipped = true) else SyncReport(error = "无配置")
        if (automatic && !profile.autoSync) return SyncReport(skipped = true)
        if (profile.data.isEmpty()) return if (automatic) SyncReport(skipped = true) else SyncReport(error = "请选择同步的数据。")
        return SyncCoordinator(repository, adapter).run(profile, mode, force, operation) { state ->
            main.post { if (!closed.get() && !operation.isCancelled) progress(state) }
        }
    }

    fun loginCloud(username: String, password: String, complete: (Result<CloudLoginResult>) -> Unit): SyncCall =
        cloud(complete) { operation ->
            require(username.isNotEmpty() && password.isNotEmpty()) { "用户名和密码不能为空。" }
            val hash = SyncRepository.md5(password)
            val server = syncLocal(operation) { repository.cloudServerIndex() }
            val generation = syncLocal(operation) { repository.rememberCloud(username, hash, false); repository.cloudGeneration() }
            val status = ViaCloudClient(server, operation).login(SyncCredentials(username, hash))
            val account = syncLocal(operation) { repository.checkCloudGeneration(generation); repository.rememberCloud(username, hash, true) }
            CloudLoginResult(account, status == 2)
        }

    fun uploadCloud(allowOversizeBookmarks: Boolean = false, complete: (Result<Unit>) -> Unit): SyncCall =
        cloud(complete) { operation ->
            val (server, credentials, generation) = syncLocal(operation) { Triple(repository.cloudServerIndex(), repository.cloudCredentials(), repository.cloudGeneration()) }
            val fields = syncLocal(operation) { ViaCloudDataCodec(app).use { it.exportFields().toMutableMap() } }
            val encodedLength = fields["bookmark"]?.let { URLEncoder.encode(it, "UTF-8").length } ?: 0
            if (encodedLength > 48_000) {
                if (!allowOversizeBookmarks) throw CloudBookmarkTooLargeException()
                fields["bookmark"] = null
            }
            ViaCloudClient(server, operation).upload(credentials, fields)
            syncLocal(operation) { repository.checkCloudGeneration(generation) }
        }

    fun downloadCloud(complete: (Result<Int>) -> Unit): SyncCall = cloud(complete) { operation ->
        val (server, credentials, generation) = syncLocal(operation) { Triple(repository.cloudServerIndex(), repository.cloudCredentials(), repository.cloudGeneration()) }
        val text = ViaCloudClient(server, operation).download(credentials)
        // Unlike Native's early success dialog, completion waits for actual parsing and local commits.
        syncLocal(operation) { repository.checkCloudGeneration(generation); ViaCloudDataCodec(app).use { it.importText(text) } }.also {
            if (it <= 0) throw SyncNetworkException("服务器返回的数据无效，未完成同步。")
        }
    }

    fun deleteCloudAccount(password: String, complete: (Result<Unit>) -> Unit): SyncCall = cloud(complete) { operation ->
        val (server, credentials) = syncLocal(operation) { repository.cloudServerIndex() to repository.cloudCredentials() }
        if (!MessageDigest.isEqual(SyncRepository.md5(password).toByteArray(Charsets.UTF_8), credentials.password.toByteArray(Charsets.UTF_8)))
            throw SyncNetworkException("密码错误。")
        // Native logs out at submission, including HTTP failures. This is a deletion request, not proof of deletion.
        syncLocal(operation) { repository.logoutCloud() }
        ViaCloudClient(server, operation).delete(credentials)
    }

    private fun <T> cloud(complete: (Result<T>) -> Unit, block: (SyncOperation) -> T): SyncCall = execute(complete) { operation ->
        while (!CLOUD_LOCK.tryLock(200, TimeUnit.MILLISECONDS)) operation.check()
        try { operation.check(); block(operation) } finally { CLOUD_LOCK.unlock() }
    }
    private fun <T> execute(complete: (Result<T>) -> Unit, block: (SyncOperation) -> T): SyncCall {
        val operation = SyncOperation()
        if (closed.get()) { operation.cancel(); return operation }
        operations.add(operation)
        operation.cancelledAction = { operations.remove(operation) }
        val future = FutureTask {
            val result = runCatching { operation.check(); block(operation) }.fold(
                onSuccess = { Result.success(it) }, onFailure = { Result.failure<T>(safeError(it)) })
            operation.finish(); operations.remove(operation)
            main.post { if (!closed.get() && !operation.isCancelled) complete(result) }
        }
        operation.attach(future)
        if (closed.get()) operation.cancel() else BrowserExecutors.network.execute(future)
        return operation
    }
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        operations.forEach(SyncOperation::cancel); operations.clear()
        BrowserExecutors.persistence.execute { adapter.close() }
    }
    private fun safeError(error: Throwable): Exception = when (error) {
        is SyncNetworkException -> error
        is CloudBookmarkTooLargeException -> error
        // URLs, account query fields and Authorization headers never enter a public error/log message.
        else -> Exception("同步失败，请检查配置或数据。")
    }
    private companion object { val CLOUD_LOCK = ReentrantLock() }
}
