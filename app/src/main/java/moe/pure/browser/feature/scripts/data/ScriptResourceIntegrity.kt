package moe.pure.browser.feature.scripts.data

import android.util.Base64
import java.security.MessageDigest

/** p5.a.M/N/O/R/S validates reads, not the download or the file-info/viewer UI. */
internal object ScriptResourceIntegrity {
    private data class Expected(val algorithm: String, val hex: String)
    private val algorithms = arrayOf(Triple("md5", "MD5", 32), Triple("sha256", "SHA-256", 64))

    fun bytes(url: String, value: ByteArray): ByteArray =
        if (value.isEmpty() || matches(url, value)) value else byteArrayOf()

    fun text(url: String, value: String?): String? =
        if (value.isNullOrEmpty() || matches(url, value.toByteArray(Charsets.UTF_8))) value else ""

    private fun matches(url: String, bytes: ByteArray): Boolean {
        val expected = expected(url) ?: return true
        return try {
            expected.hex.equals(hex(MessageDigest.getInstance(expected.algorithm).digest(bytes)), true)
        } catch (_: Exception) { true }
    }

    private fun expected(url: String): Expected? {
        val hash = url.lastIndexOf('#')
        if (hash <= 0 || !url.contains("://")) return null
        for ((name, algorithm, length) in algorithms) {
            val at = url.indexOf(name, hash + 1)
            if (at < 0) continue
            val separator = at + name.length
            if (url.length <= separator || (url[separator] != '=' && url[separator] != '-')) continue
            val start = separator + 1
            var end = url.indexOf(',', start)
            if (end < 0) end = url.indexOf(';', start)
            if (end < 0) end = url.length
            if (end <= start + 1) return null
            var value = url.substring(start, end)
            if (value.length != length) {
                try { value = hex(Base64.decode(value.toByteArray(Charsets.UTF_8), Base64.DEFAULT)) }
                catch (_: Exception) { /* Native retains the original fragment after a decode failure. */ }
            }
            return Expected(algorithm, value)
        }
        return null
    }

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { Integer.toHexString(it.toInt() and 255).padStart(2, '0') }
}
