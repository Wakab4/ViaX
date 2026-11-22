package moe.via.browser.feature.sync.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class SyncNetworkException(message: String, val status: Int = 0) : IOException(message)

internal object SyncHttp {
    val dav = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS).callTimeout(60, TimeUnit.SECONDS).build()
    val cloudGet = dav.newBuilder().connectTimeout(7, TimeUnit.SECONDS).readTimeout(7, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS).build()
    val cloudPost = dav.newBuilder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS).build()

    fun execute(client: OkHttpClient, request: Request, operation: SyncOperation): Response {
        operation.check()
        val call = client.newCall(request)
        operation.attach(call)
        try { return call.execute() }
        catch (_: IOException) { operation.check(); throw SyncNetworkException("连接失败，请检查网络和同步配置。") }
        // Keep the call registered through response-body reads, not merely until headers arrive.
        // finish()/cancel() releases the bounded per-operation set after all bodies have been closed.
    }
}
