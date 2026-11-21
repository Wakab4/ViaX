package moe.pure.browser.feature.backup.data.export

import java.security.MessageDigest
import java.util.Locale

/** m9.l.i/p.j and v0.g name actual backup attachments, including the original ellipsis. */
internal object SettingsAttachmentNames {
    private val forbidden = Regex("[\\\\/:*?\"<>|\\s]")
    private val repeated = Regex("-{2,}")

    fun prefix(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        val normalized = value.trim { it <= ' ' }.replace(forbidden, "-").replace(repeated, "-").lowercase(Locale.ROOT)
        val part = if (normalized.length > 128) normalized.substring(0, 128) + "..." else normalized
        return if (part.isEmpty()) "" else "$part-"
    }

    fun urlDigest(value: String): String = try {
        val bytes = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        buildString(bytes.size * 2) {
            for (byte in bytes) {
                val number = byte.toInt() and 255
                if (number < 16) append('0')
                append(Integer.toHexString(number))
            }
        }
    } catch (error: Exception) { error.printStackTrace(); value }
}
