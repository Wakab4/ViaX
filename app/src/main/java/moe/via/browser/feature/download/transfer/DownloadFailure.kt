package moe.via.browser.feature.download.transfer

import java.io.IOException
import java.io.InterruptedIOException
import java.net.MalformedURLException
import java.net.UnknownHostException
import java.security.GeneralSecurityException
import java.util.concurrent.RejectedExecutionException

/** m5.h: error identity drives retries; its canonical message is what Native persists in tasks. */
internal class DownloadFailure(val code: Int, message: String? = name(code), cause: Throwable? = null) : IOException(message, cause) {
    val retryable get() = code == 503 || code == 500 || code == 20 || code == 31

    companion object {
        fun http(code: Int, reason: String?) = DownloadFailure(code, "HTTP Error: $code $reason")
        fun from(error: Exception): DownloadFailure = when (error) {
            is DownloadFailure -> error
            is TransferStopped, is InterruptedIOException -> DownloadFailure(1, cause = error)
            is MalformedURLException, is UnknownHostException -> DownloadFailure(21, cause = error)
            is GeneralSecurityException -> DownloadFailure(22, cause = error)
            is RejectedExecutionException -> DownloadFailure(30, cause = error)
            else -> DownloadFailure(20, cause = error)
        }
        private fun name(code: Int): String? = when (code) {
            1 -> "User Cancel"
            2 -> "Unsupported Protocol"
            11 -> "Can Not Create File"
            12 -> "Can Not Write File"
            20 -> "Http Data Error"
            21 -> "Http Bad Request"
            22 -> "Certificate Error"
            23 -> "Network Timeout"
            24 -> "Non Resumable"
            25 -> "Too Many Redirects"
            30 -> "Task Submit Failed"
            31 -> "Chunk Verify Failed"
            32 -> "No Network Connection"
            40 -> "Decode Error"
            else -> null
        }
    }
}
