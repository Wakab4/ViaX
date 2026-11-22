package moe.via.browser.feature.scripts.runtime.http

import android.util.Base64
import org.json.JSONException
import java.net.HttpURLConnection
import java.util.Locale

/** Native request headers use only caller-supplied cookies and the platform default user agent. */
internal object NativeGmHttpConnection {
    fun configure(connection: HttpURLConnection, options: NativeGmHttpOptions, body: ByteArray?) {
        if (body != null) {
            connection.doOutput = true; connection.doInput = true
            connection.setRequestProperty("Content-Length", body.size.toString())
            connection.setRequestProperty("Content-Type", if (options.data.indexOf('=') > 0)
                "application/x-www-form-urlencoded;charset=UTF-8" else "text/plain;charset=UTF-8")
        }
        if (options.user.isNotEmpty() && options.password.isNotEmpty()) {
            val bytes = (options.user + ":" + options.password).toByteArray(Charsets.UTF_8)
            connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(bytes, Base64.NO_WRAP))
        }
        val headers = try {
            options.headers?.let { source -> HashMap<String, String>().apply {
                source.keys().forEach { key -> put(key, source.getString(key)) }
            } }
        } catch (_: JSONException) { null }
        var userAgent: String? = null
        var cookie: String? = null
        headers?.forEach { (name, value) ->
            when {
                name.equals("cookie", true) -> cookie = value
                name.equals("user-agent", true) -> userAgent = value
                else -> connection.setRequestProperty(name, value)
            }
        }
        if (userAgent != null || !options.userAgent.isNullOrEmpty())
            connection.setRequestProperty("User-Agent", userAgent ?: options.userAgent)
        if (!options.anonymous) {
            val cookies = listOfNotNull(cookie, options.cookie).filter(String::isNotEmpty).joinToString(";")
            if (cookies.isNotEmpty()) connection.setRequestProperty("Cookie", cookies)
        }
        if (options.overrideMimeType.isNotEmpty()) connection.setRequestProperty("Content-Type", options.overrideMimeType)
        connection.connectTimeout = options.timeout; connection.readTimeout = options.timeout
    }

    fun responseHeaders(connection: HttpURLConnection): String = buildString {
        connection.headerFields.forEach { (name, values) ->
            if (!name.isNullOrEmpty()) {
                append(name.lowercase(Locale.ROOT)); append(": ")
                if (!values.isNullOrEmpty()) append(values.joinToString("; "))
                append("\r\n")
            }
        }
    }
}
