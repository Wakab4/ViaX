package moe.pure.browser.feature.sync.network

import moe.pure.browser.feature.sync.model.SyncCredentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

/** Old account protocol, invoked only by an explicit manager operation. Does not validate services at construction. */
internal class ViaCloudClient(serverIndex: Int, private val operation: SyncOperation) {
    private val base = if (serverIndex == 1) "https://app.viayoo.com/api/" else "https://us.app.viayoo.com/api/"
    fun login(credentials: SyncCredentials): Int = when (get("user", credentials)) {
        "0" -> 0; "1" -> throw SyncNetworkException("密码错误。")
        "2" -> 2; else -> throw SyncNetworkException("服务器异常。")
    }
    fun download(credentials: SyncCredentials): String = get("sync", credentials).also {
        if (it.isEmpty()) throw SyncNetworkException("服务器异常。")
    }
    fun upload(credentials: SyncCredentials, fields: Map<String, String?>) {
        val values = linkedMapOf<String, String?>("name" to credentials.username, "psw" to credentials.password)
        fields.forEach { (key, value) -> values[key] = value?.let { URLEncoder.encode(it, "UTF-8") } }
        post(values)
    }
    fun delete(credentials: SyncCredentials) = post(linkedMapOf("name" to credentials.username, "psw" to credentials.password, "op" to "delete"))
    private fun get(path: String, credentials: SyncCredentials): String {
        // Native concatenates account fields; data values alone are form-encoded on the upload path.
        val url = base + path + "?name=" + credentials.username + "&psw=" + credentials.password
        val request = Request.Builder().url(url).get().build()
        return SyncHttp.execute(SyncHttp.cloudGet, request, operation).use {
            if (it.code != 200) throw SyncNetworkException("HTTP ${it.code}", it.code)
            val text = it.body?.bytes()?.toString(Charsets.UTF_8).orEmpty()
            META.replace(text, "").trim()
        }.also { operation.check() }
    }
    private fun post(fields: Map<String, String?>) {
        val body = fields.entries.joinToString("&") { it.key + "=" + (it.value ?: "null") }.toByteArray(Charsets.UTF_8)
        val request = Request.Builder().url(base + "update").post(body.toRequestBody("application/x-www-form-urlencoded".toMediaType())).build()
        SyncHttp.execute(SyncHttp.cloudPost, request, operation).use {
            if (it.code != 200) throw SyncNetworkException("HTTP ${it.code}", it.code)
            // Native defines success by HTTP status and does not inspect the business response body.
        }
        operation.check()
    }
    private companion object { val META = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE) }
}
