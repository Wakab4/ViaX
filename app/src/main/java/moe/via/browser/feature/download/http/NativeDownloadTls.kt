package moe.via.browser.feature.download.http

import android.annotation.SuppressLint
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.X509TrustManager

/** l5.b.c / i5.a/c. This permissive policy belongs to Native's download GET connections. */
@SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager", "BadHostnameVerifier")
internal object NativeDownloadTls {
    fun apply(connection: HttpsURLConnection) {
        try {
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate>? = null
            }), SecureRandom())
            connection.sslSocketFactory = context.socketFactory
        } catch (error: Exception) { moe.via.browser.core.logging.NativeLog.warn("DownloadTls", "Could not create Native TLS factory", error) }
        connection.hostnameVerifier = object : HostnameVerifier {
            override fun verify(hostname: String?, session: SSLSession?) = true
            override fun toString() = "ALLOW_ALL"
        }
    }
}
