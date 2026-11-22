package moe.via.browser.feature.report

import moe.via.browser.core.logging.NativeLog as Log
import moe.via.browser.core.concurrent.BrowserExecutors
import java.net.HttpURLConnection
import java.net.URL

/** Explicit form submissions only. It never starts a request while a page or menu is opened. */
internal object ReportClient {
    private const val ENDPOINT = "https://ra.viayoo.com/report_abuse"
    // Public application protocol value bundled in the supplied reference APK, not a user credential.
    private const val AUTHORIZATION = "Bearer tk_o4s4ghe1tqu60mj0ljy5im762ebrr"

    fun submit(payload: ReportPayload) {
        BrowserExecutors.network.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(ENDPOINT).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 20_000
                connection.setRequestProperty("Authorization", AUTHORIZATION)
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.outputStream.use { output ->
                    output.write(payload.toJson().toByteArray(Charsets.UTF_8))
                    output.flush()
                }
                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) Log.warn("Report", "Report server returned HTTP $status")
            } catch (error: Exception) {
                Log.warn("Report", "Report request failed", error)
            } finally {
                connection?.disconnect()
            }
        }
    }
}
