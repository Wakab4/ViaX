package moe.pure.browser.feature.contextmenu.image

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.download.DataUriInputStream
import moe.pure.browser.feature.download.DownloadNames
import moe.pure.browser.feature.download.http.NativeDownloadFilename
import moe.pure.browser.core.mime.NativeMimeTypes
import moe.pure.browser.web.bridge.NativePageBridge
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal data class ContextImage(val file: File, val mime: String, val name: String)

/** Local files pass through; page XHR uses the common Native via.download callback. */
internal class ContextImageLoader(context: Context, private val completed: (Result<ContextImageResult>) -> Unit) {
    private val app = context.applicationContext
    private val directory = File(app.externalCacheDir ?: app.cacheDir, "download")
    private val main = Handler(Looper.getMainLooper())
    private val jobs = mutableSetOf<Job>()
    private val cached = mutableMapOf<String, ContextImage>()
    private val operations = ContextImageOperations()
    private var disposed = false

    fun load(view: WebView, url: String, requested: Int, fetching: () -> Unit) {
        if (disposed || requested == 0) return
        if (url.startsWith("file://", true)) {
            val file = File(Uri.parse(url).path.orEmpty())
            completed(Result.success(ContextImageResult(url, ContextImage(file, mimeForName(file.name), file.name), requested)))
            return
        }
        if (url.startsWith("data:", true)) { Job(url, emptyMap(), requested).decode(url); return }
        cached[url]?.takeIf { it.file.isFile && it.file.lastModified() >= System.currentTimeMillis() - MAX_AGE }?.let {
            completed(Result.success(ContextImageResult(url, it, requested))); return
        }
        if (view.settings.javaScriptEnabled) {
            operations.add(url, requested)
            NativePageBridge.image(view, url)
        } else Job(url, headers(view, url), requested).direct()
        fetching()
    }
    fun downloaded(view: WebView, url: String?, data: String?): Boolean {
        if (disposed) return false
        val target = url.orEmpty()
        val snapshot = operations.get(target)
        if (snapshot == 0) return false
        val job = Job(target, headers(view, target), snapshot)
        if (data?.startsWith("data:", true) == true) job.decode(data) else job.direct()
        return true
    }
    fun consume(url: String, operation: Int) = operations.remove(url, operation)
    fun dispose() {
        disposed = true
        jobs.toList().forEach { it.cancel() }; jobs.clear(); cached.clear(); operations.clear()
    }
    private fun headers(view: WebView, url: String) = buildMap {
        put("User-Agent", view.settings.userAgentString.orEmpty())
        view.url?.takeIf(URLUtil::isNetworkUrl)?.let { put("Referer", it) }
        if (URLUtil.isNetworkUrl(url)) CookieManager.getInstance().getCookie(url)?.takeIf(String::isNotEmpty)?.let { put("Cookie", it) }
    }
    private inner class Job(private val url: String, private val headers: Map<String, String>, private val snapshot: Int) {
        val cancelled = AtomicBoolean()
        @Volatile var call: Call? = null
        fun decode(data: String) = work {
            DataUriInputStream.open(data).use { copy(it, DataUriInputStream.header(data).mime, decoded = true) }
        }
        fun direct() = work {
            check(URLUtil.isNetworkUrl(url)) { "无法读取此图片" }
            val request = Request.Builder().url(url).apply { headers.forEach { (name, value) -> if (value.isNotEmpty()) header(name, value) } }.build()
            val active = client.newCall(request); call = active
            active.execute().use { response ->
                check(response.isSuccessful) { "图片下载失败" }
                val body = response.body
                val type = body.contentType()?.toString()?.substringBefore(';') ?: mimeForName(Uri.parse(url).lastPathSegment.orEmpty())
                body.byteStream().use { copy(it, type) }
            }
        }
        private fun work(block: () -> ContextImage) {
            jobs.add(this)
            BrowserExecutors.network.execute {
                val result = runCatching {
                    check(!cancelled.get())
                    check(directory.mkdirs() || directory.isDirectory)
                    block()
                }
                main.post {
                    jobs.remove(this)
                    if (!disposed && !cancelled.get()) {
                        if (!url.startsWith("data:", true)) result.onSuccess { cached[url] = it }
                        completed(result.map { ContextImageResult(url, it, snapshot) })
                    }
                }
            }
        }
        private fun copy(input: InputStream, rawMime: String, decoded: Boolean = false): ContextImage {
            val mime = rawMime.ifBlank { "image/*" }
            val extension = if (decoded) NativeMimeTypes.extension(mime) ?: "png"
                else NativeMimeTypes.extension(addressMime(url))
            val file = File(directory, rawDigest(url) + (extension?.let { ".$it" } ?: ""))
            try {
                file.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (true) {
                        check(!cancelled.get()) { "读取已取消" }
                        val count = input.read(buffer); if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.flush()
                }
                val storedMime = mimeForName(file.name)
                return ContextImage(file, storedMime, NativeDownloadFilename.guess(url, null, storedMime))
            } catch (error: Throwable) { file.delete(); throw error }
        }
        fun cancel() { cancelled.set(true); call?.cancel() }
    }
    private fun mimeForName(name: String) = DownloadNames.mime(name, "image/*")
    private fun addressMime(value: String): String? {
        val decoded = Uri.decode(value).substringBefore('?')
        val slash = decoded.lastIndexOf('/')
        val dot = decoded.indexOf('.', slash + 1)
        return if (slash >= 0 && dot > 0 && !decoded.endsWith('/')) NativeMimeTypes.mime(decoded.substring(dot + 1), "image/*") else "image/*"
    }
    private fun rawDigest(value: String): String = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    companion object {
        private const val MAX_AGE = 24 * 60 * 60 * 1000L
        private val client by lazy { OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS).callTimeout(60, TimeUnit.SECONDS).build() }
    }
}
