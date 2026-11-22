package moe.via.browser.feature.passwords.model

import java.util.UUID

/** Metadata only. Native timestamps are epoch milliseconds; zero is filled at the initial save. */
data class PasswordEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String = "",
    val username: String,
    val note: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) {
    override fun toString() = "PasswordEntry(id=$id)"
}

data class StoredPassword(val entry: PasswordEntry, val ciphertext: String, val version: Int = 1) {
    override fun toString() = "StoredPassword(id=${entry.id}, version=$version)"
}

/** Only an explicitly requested decrypt, capture, edit or import owns this transient plaintext. */
data class PasswordCredential(val entry: PasswordEntry, val password: String) {
    override fun toString() = "PasswordCredential(id=${entry.id}, password=<redacted>)"
}
