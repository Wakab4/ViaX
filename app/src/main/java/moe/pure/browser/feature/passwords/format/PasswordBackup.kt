package moe.pure.browser.feature.passwords.format

import moe.pure.browser.feature.passwords.crypto.PasswordCipher
import moe.pure.browser.feature.passwords.crypto.PasswordCryptoException
import moe.pure.browser.feature.passwords.data.PasswordRepository
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream

/** pass.enc payload only. The backup host owns ZIP/info.enc and supplies its explicit password and recovered salt. */
object PasswordBackup {
    const val ENTRY_NAME = "pass.enc"

    /** Caller owns output and clears its password buffer. No intermediate plaintext CSV file is created. */
    fun exportEncrypted(repository: PasswordRepository, output: OutputStream, password: CharArray, salt: String): Int {
        val credentials = repository.exportCredentials()
        val key = PasswordCipher.derive(password, salt)
        try {
            val iv = ByteArray(16).also(SecureRandom()::nextBytes)
            output.write(iv)
            val nonClosing = object : FilterOutputStream(output) {
                override fun write(bytes: ByteArray, offset: Int, length: Int) { output.write(bytes, offset, length) }
                override fun close() { flush() }
            }
            val encrypted = CipherOutputStream(nonClosing, PasswordCipher.cipher(Cipher.ENCRYPT_MODE, key, iv))
            return OutputStreamWriter(encrypted).use { PasswordCsv.write(it, credentials) }
        } finally { key.fill(0) }
    }

    /** The CSV parser reaches and validates CBC's final padding before any import transaction is committed. */
    fun importEncrypted(repository: PasswordRepository, input: InputStream, password: CharArray, salt: String): Int {
        val key = PasswordCipher.derive(password, salt)
        try {
            val iv = ByteArray(16)
            var offset = 0
            while (offset < iv.size) {
                val read = input.read(iv, offset, iv.size - offset)
                if (read < 0) throw PasswordCryptoException("密码备份格式无效。")
                if (read > 0) offset += read
            }
            val cipher = PasswordCipher.cipher(Cipher.DECRYPT_MODE, key, iv)
            return InputStreamReader(DecryptingInput(input, cipher)).use { repository.importCsv(it) }
        } catch (error: PasswordCryptoException) { throw error }
        catch (error: Exception) { throw PasswordCryptoException("无法解密密码备份，请检查密码。", error) }
        finally { key.fill(0) }
    }

    /** Unlike permissive stream wrappers, EOF always calls doFinal and propagates invalid padding. */
    private class DecryptingInput(private val source: InputStream, private val cipher: Cipher) : InputStream() {
        private val encrypted = ByteArray(4096)
        private var plain = ByteArray(0)
        private var offset = 0
        private var ended = false
        override fun read(): Int {
            val byte = ByteArray(1)
            return if (read(byte, 0, 1) < 0) -1 else byte[0].toInt() and 255
        }
        override fun read(target: ByteArray, start: Int, length: Int): Int {
            if (start < 0 || length < 0 || start > target.size - length) throw IndexOutOfBoundsException()
            if (length == 0) return 0
            while (offset == plain.size) {
                plain.fill(0)
                if (ended) return -1
                val read = source.read(encrypted)
                plain = try {
                    if (read < 0) { ended = true; cipher.doFinal() }
                    else if (read == 0) continue else cipher.update(encrypted, 0, read) ?: ByteArray(0)
                } catch (error: Exception) { throw IOException("Invalid encrypted password backup", error) }
                offset = 0
            }
            val size = minOf(length, plain.size - offset)
            plain.copyInto(target, start, offset, offset + size); offset += size
            return size
        }
        override fun close() { plain.fill(0); encrypted.fill(0); ended = true }
    }
}
