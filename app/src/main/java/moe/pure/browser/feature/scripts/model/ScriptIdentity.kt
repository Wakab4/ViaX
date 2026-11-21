package moe.pure.browser.feature.scripts.model

import java.security.MessageDigest

/** Via identifies a script by its original namespace and name, without a separator. */
object ScriptIdentity {
    fun key(namespace: String, name: String): String {
        val identity = namespace + name
        if (identity.isEmpty()) return ""
        return MessageDigest.getInstance("MD5").digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
