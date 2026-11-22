package moe.via.browser.feature.backup.crypto

/** Native archive wire constants only; these are never used as an installation key or logged. */
internal object InternalBackupProtocol {
    private const val FALLBACK = "3_=cZHGxZ#FZgZA@"
    private val saltMask = byteArrayOf(90, 60, 127, -89)
    fun password(input: CharArray): CharArray = if (input.isEmpty()) FALLBACK.toCharArray() else input.copyOf()
    fun encodeSalt(salt: String): ByteArray = salt.toByteArray(Charsets.UTF_8).also { bytes ->
        for (i in bytes.indices) bytes[i] = (bytes[i].toInt() xor saltMask[i % saltMask.size].toInt()).toByte()
    }
    fun decodeSalt(input: ByteArray): String {
        require(input.size in 1..128) { "加密备份信息无效" }
        val decoded = input.copyOf()
        for (i in decoded.indices) decoded[i] = (decoded[i].toInt() xor saltMask[i % saltMask.size].toInt()).toByte()
        return String(decoded, Charsets.UTF_8).also { require(it.matches(Regex("[0-9a-fA-F]{32}"))) { "加密备份信息无效" } }
    }
}
