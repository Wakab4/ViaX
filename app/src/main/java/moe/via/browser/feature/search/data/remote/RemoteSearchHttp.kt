package moe.via.browser.feature.search.data.remote

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/** r9.j has no explicit timeouts, retry loop, error-body parsing or response charset override. */
internal object RemoteSearchHttp {
    private val client by lazy {
        OkHttpClient.Builder().connectTimeout(0, TimeUnit.MILLISECONDS).readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(0, TimeUnit.MILLISECONDS).callTimeout(0, TimeUnit.MILLISECONDS).build()
    }

    data class Result(val status: Int, val body: String?)

    fun call(userId: String): Call = client.newCall(RemoteSearchConfigProtocol.request(userId))

    fun read(response: Response): Result = try {
        response.use {
            // HttpURLConnection.getInputStream throws on these statuses; Native reports code 10.
            if (it.code >= 400) throw IOException()
            val body = it.body?.byteStream()?.reader(Charset.defaultCharset())?.buffered()?.use { reader ->
                buildString { while (true) append(reader.readLine() ?: break) }
            }.orEmpty()
            Result(it.code, body)
        }
    } catch (_: IOException) { Result(10, null) }
}
