package moe.pure.browser.feature.sync.network

import android.net.Uri
import android.util.Xml
import moe.pure.browser.feature.sync.model.SyncCredentials
import moe.pure.browser.feature.sync.model.WebDavProfile
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale
import java.util.TimeZone

/** Real extension methods through OkHttp's public Request.Builder.method API. No test/validation request in construction. */
internal class WebDavTransport(
    profile: WebDavProfile, credentials: SyncCredentials, private val operation: SyncOperation
) {
    private val base = profile.baseUrl.toHttpUrl()
    private val auth = HttpAuthentication(credentials.username, credentials.password, profile.digestAuth)

    fun exists(path: String, directory: Boolean = false): Boolean = request("HEAD", path).use {
        when {
            it.isSuccessful -> true
            it.code == 404 -> false
            directory && it.code == 405 -> true
            else -> throw SyncNetworkException("HTTP ${it.code}", it.code)
        }
    }
    fun download(path: String): ByteArray {
        if (!exists(path)) throw SyncNetworkException("远端文件不存在。", 404)
        return request("GET", path).use {
            requireSuccess(it)
            it.body?.bytes() ?: ByteArray(0)
        }.also { operation.check() }
    }
    fun upload(path: String, bytes: ByteArray, mime: String) {
        ensureDirectories(path)
        request("PUT", path, bytes, mime).use(::requireSuccess)
        operation.check()
    }
    fun modified(path: String): Long {
        val bytes = request("PROPFIND", path, ALL_PROPERTIES, "application/xml; charset=utf-8", mapOf("Depth" to "1")).use {
            if (it.code == 404) return 0
            requireSuccess(it); it.body?.bytes() ?: return 0
        }
        operation.check()
        return parseProperties(bytes, target(path))
    }
    private fun ensureDirectories(path: String) {
        var current = ""
        for (piece in path.substringBeforeLast('/', "").split('/').filter(String::isNotEmpty)) {
            current += "$piece/"
            if (!exists(current, directory = true)) request("MKCOL", current).use {
                if (!it.isSuccessful && it.code != 405) throw SyncNetworkException("HTTP ${it.code}", it.code)
            }
        }
    }
    private fun request(
        method: String, path: String, bytes: ByteArray? = null, mime: String = "application/octet-stream", headers: Map<String, String> = emptyMap()
    ): Response {
        var url = target(path)
        repeat(3) { attempt ->
            operation.check()
            val requestTarget = url.encodedPath + url.encodedQuery?.let { "?$it" }.orEmpty()
            val builder = Request.Builder().url(url).method(method, bytes?.toRequestBody(mime.toMediaType()))
            for ((name, value) in headers) builder.header(name, value)
            auth.authorization(method, requestTarget, bytes ?: ByteArray(0))?.let { builder.header("Authorization", it) }
            val response = SyncHttp.execute(SyncHttp.dav, builder.build(), operation)
            if (response.code != 401 || attempt == 2) return response
            val finalUrl = response.request.url
            val sameOrigin = base.scheme == finalUrl.scheme && base.host == finalUrl.host && base.port == finalUrl.port
            val retry = sameOrigin && auth.challenge(response.headers.values("WWW-Authenticate"))
            if (!retry) return response
            response.close(); url = finalUrl
        }
        throw SyncNetworkException("身份验证失败。", 401)
    }
    private fun target(path: String): HttpUrl = base.newBuilder().addPathSegments(path.trimStart('/')).build()
    private fun requireSuccess(response: Response) {
        if (!response.isSuccessful) throw SyncNetworkException("HTTP ${response.code}", response.code)
    }

    private fun parseProperties(bytes: ByteArray, target: HttpUrl): Long {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        runCatching { parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false) }
        parser.setInput(ByteArrayInputStream(bytes), null)
        var href = ""; var modified = ""; var created = ""
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            operation.check()
            if (parser.eventType == XmlPullParser.START_TAG) when (parser.name) {
                "response" -> { href = ""; modified = ""; created = "" }
                "href" -> href = parser.nextText()
                "getlastmodified" -> modified = parser.nextText()
                "creationdate" -> created = parser.nextText()
            }
            else if (parser.eventType == XmlPullParser.END_TAG && parser.name == "response") {
                val resource = target.resolve(href)
                if (resource != null && Uri.decode(resource.encodedPath).trimEnd('/') == Uri.decode(target.encodedPath).trimEnd('/'))
                    return date(modified).takeIf { it > 0 } ?: date(created)
            }
            parser.next()
        }
        return 0
    }
    private fun date(text: String): Long {
        if (text.isEmpty()) return 0
        runCatching { return Instant.parse(text).toEpochMilli() }
        runCatching { return OffsetDateTime.parse(text).toInstant().toEpochMilli() }
        for (pattern in listOf("EEE, dd MMM yyyy HH:mm:ss zzz", "EEEE, dd-MMM-yy HH:mm:ss zzz", "EEE MMM d HH:mm:ss yyyy")) {
            runCatching { return SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("GMT"); isLenient = false }.parse(text)?.time ?: 0 }
        }
        return 0
    }
    private companion object {
        val ALL_PROPERTIES = "<?xml version=\"1.0\" encoding=\"utf-8\"?><d:propfind xmlns:d=\"DAV:\"><d:allprop/></d:propfind>".toByteArray(Charsets.UTF_8)
    }
}
