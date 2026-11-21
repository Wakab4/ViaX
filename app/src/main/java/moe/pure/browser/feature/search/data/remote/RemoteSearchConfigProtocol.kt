package moe.pure.browser.feature.search.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** GP r9.f/mark.via.Shell request constants; never log request headers or identity. */
internal object RemoteSearchConfigProtocol {
    private const val ENDPOINT = "https://us-c.viayoo.com/api/frontend"
    private const val AUTHORIZATION = "default:production.5ae8018a613c7b4ef6297b23aaf7d5d18d16cac9a6636ae20d378aa2"
    private const val AGENT = "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36 Via/7.3.3"

    fun request(userId: String): Request {
        val json = JSONObject().put("userId", userId).toString()
        val bytes = ByteArray(json.length) { json[it].code.toByte() }
        return Request.Builder().url(ENDPOINT).header("Authorization", AUTHORIZATION).header("User-Agent", AGENT)
            .post(bytes.toRequestBody("application/json".toMediaType())).build()
    }
}
