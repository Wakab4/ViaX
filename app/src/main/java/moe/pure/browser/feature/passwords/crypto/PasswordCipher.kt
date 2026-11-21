package moe.pure.browser.feature.passwords.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

open class PasswordCryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)
class PasswordKeyUnavailableException(cause: Throwable? = null) :
    PasswordCryptoException("密码密钥不可用，已保留保存的数据。", cause)

/** The Native v1 envelope has a random 16-byte IV followed by AES-256-CBC/PKCS5 ciphertext. */
object PasswordCipher {
    const val VERSION = 1
    private val random = SecureRandom()

    fun derive(password: CharArray, salt: String): ByteArray {
        val spec = PBEKeySpec(password, salt.toByteArray(Charsets.UTF_8), 10_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }

    fun encrypt(key: ByteArray, plain: ByteArray): ByteArray {
        val iv = ByteArray(16).also(random::nextBytes)
        return iv + cipher(Cipher.ENCRYPT_MODE, key, iv).doFinal(plain)
    }

    fun decrypt(key: ByteArray, envelope: ByteArray): ByteArray {
        if (envelope.size < 32 || (envelope.size - 16) % 16 != 0) throw PasswordCryptoException("保存的密码格式无效。")
        return try { cipher(Cipher.DECRYPT_MODE, key, envelope.copyOfRange(0, 16)).doFinal(envelope, 16, envelope.size - 16) }
        catch (error: Exception) { throw PasswordCryptoException("无法解密保存的密码。", error) }
    }

    internal fun cipher(mode: Int, key: ByteArray, iv: ByteArray): Cipher {
        require(key.size >= 32 && iv.size == 16)
        return Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
            init(mode, SecretKeySpec(key, 0, 32, "AES"), IvParameterSpec(iv))
        }
    }
}
