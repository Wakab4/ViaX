package moe.via.browser.core.text

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import moe.via.browser.core.logging.NativeLog

/** z8.v0.d keeps single-null empty, but joins multiple fields with the original null marker. */
internal object NativeStringFingerprint {
    fun of(vararg values: String?): String {
        val text = if (values.size == 1) values[0].orEmpty() else values.joinToString("") { it ?: "__NULL__" }
        if (text.isEmpty()) return ""
        return try { MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02X".format(it) } }
        catch (error: NoSuchAlgorithmException) { NativeLog.debug(null, null, error); Integer.toHexString(text.hashCode()) }
    }
}
