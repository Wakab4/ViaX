package moe.pure.browser.feature.scripts.runtime.http

import java.io.IOException
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/** o5.b owns one request; its specific error exits deliberately preserve the original ready-state. */
internal class NativeGmHttpRequest(private val options: NativeGmHttpOptions, dispatch: (String) -> Unit) {
    private val callbacks = NativeGmHttpCallbacks(dispatch, options)

    fun start() { if (options.synchronous) execute() else Thread { execute() }.start() }

    private fun execute() {
        val response = NativeGmHttpResponse(options.context)
        try {
            val url = URL(options.url)
            val body = options.data.takeIf(String::isNotEmpty)?.toByteArray(Charsets.UTF_8)
            try {
                response.readyState = 1
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = options.method
                callbacks.loadStart(response)
                NativeGmHttpConnection.configure(connection, options, body)
                connection.connect()
                if (body != null && !upload(connection, body, response)) return
                response.status = connection.responseCode
                response.statusText = connection.responseMessage
                response.finalUrl = connection.url.toString()
                response.readyState = 2; callbacks.ready(response)
                if (response.status !in 200..299) {
                    connection.disconnect(); callbacks.error(response); return
                }
                response.headers = NativeGmHttpConnection.responseHeaders(connection)
                val length = connection.contentLength
                response.lengthComputable = length > 0
                response.total = if (length > 0) length.toLong() else -1
                response.readyState = 3; callbacks.ready(response)
                if (!options.method.equals("HEAD", true)) {
                    val result = NativeGmHttpBody.read(connection, options.responseType, response, length) { callbacks.progress(response) }
                    if (result.tooLarge) { callbacks.error(response); return }
                    callbacks.progress(response)
                    response.responseType = options.responseType
                    if (result.binary) response.dataUrl = result.value else response.text = result.value
                }
                connection.disconnect()
                response.readyState = 4; callbacks.loaded(response)
            } catch (_: UnsupportedEncodingException) {
                moe.pure.browser.core.logging.NativeLog.warn("UserScriptHttp", "Unsupported response encoding")
            } catch (_: SocketTimeoutException) {
                callbacks.timeout()
            } catch (_: IOException) {
                response.readyState = 4; callbacks.error(response)
            }
        } catch (_: Exception) {
            callbacks.error(response)
        }
    }

    private fun upload(connection: HttpURLConnection, body: ByteArray, response: NativeGmHttpResponse): Boolean {
        try {
            val output = connection.outputStream
            output.write(body); output.flush(); output.close()
            response.lengthComputable = true; response.total = body.size.toLong(); response.loaded = body.size.toLong()
            response.upload = true; callbacks.uploaded(response); response.upload = false
            return true
        } catch (error: SocketTimeoutException) { throw error }
        catch (error: UnsupportedEncodingException) { throw error }
        catch (_: IOException) {
            moe.pure.browser.core.logging.NativeLog.warn("UserScriptHttp", "Unable to write request body")
            return false
        }
    }
}
