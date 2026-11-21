package moe.pure.browser.feature.download.http

import android.webkit.CookieManager
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import moe.pure.browser.feature.download.DownloadControl
import moe.pure.browser.feature.download.transfer.DownloadFailure

/** i5.b's four-request redirect loop; caller headers are applied anew after each URL's Cookie. */
internal class NativeDownloadConnection(private val control: DownloadControl) {
    fun open(address: String, configure: (HttpURLConnection) -> Unit,
        redirected: (String, Boolean) -> Unit = { _, _ -> }): HttpURLConnection {
        var url = URL(address)
        repeat(4) { request ->
            control.check()
            if (url.protocol != "http" && url.protocol != "https") throw DownloadFailure(2)
            val connection = url.openConnection() as HttpURLConnection
            control.attach(connection)
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 20_000; connection.readTimeout = 20_000
                connection.setRequestProperty("Accept-Encoding", "identity")
                runCatching { CookieManager.getInstance().getCookie(url.toString()) }.getOrNull()?.let {
                    connection.setRequestProperty("Cookie", it)
                }
                if (connection is HttpsURLConnection) NativeDownloadTls.apply(connection)
                configure(connection)
                val code = connection.responseCode
                if (code !in setOf(301, 302, 303, 307, 308)) return connection
                val next = URL(url, connection.getHeaderField("Location"))
                redirected(next.toString(), code == 301 || code == 308)
                control.detach(connection)
                if (request == 3) throw DownloadFailure(25)
                url = next
            } catch (error: Exception) { control.detach(connection); throw error }
        }
        throw DownloadFailure(25)
    }
}
