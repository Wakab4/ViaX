package moe.pure.browser.feature.passwords.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.Closeable
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.util.UUID
import javax.crypto.Cipher

/** Lazy worker-only local key access. Construction never opens KeyStore or reads a password. */
class PasswordCrypto(context: Context) : Closeable {
    private val app = context.applicationContext
    private val preferences by lazy { app.getSharedPreferences("passwords_crypto", Context.MODE_PRIVATE) }
    private val alias = app.packageName + ".Passwords.v1"
    private var material: ByteArray? = null
    private var closed = false

    fun encrypt(password: String, allowCreate: Boolean): String = synchronized(LOCK) {
        val bytes = password.toByteArray(Charsets.UTF_8)
        try { Base64.encodeToString(PasswordCipher.encrypt(key(allowCreate), bytes), Base64.NO_WRAP) }
        catch (error: PasswordCryptoException) { throw error }
        catch (error: Exception) { throw PasswordCryptoException("无法加密密码。", error) }
        finally { bytes.fill(0) }
    }

    fun decrypt(encoded: String): String = synchronized(LOCK) {
        val bytes = try { Base64.decode(encoded, Base64.NO_WRAP) }
        catch (error: IllegalArgumentException) { throw PasswordCryptoException("保存的密码格式无效。", error) }
        val plain = PasswordCipher.decrypt(key(false), bytes)
        try { String(plain, Charsets.UTF_8) } finally { plain.fill(0) }
    }

    private fun key(allowCreate: Boolean): ByteArray {
        check(!closed) { "Password crypto is closed" }
        material?.let { return it }
        try {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val wrapped = preferences.getString("psk", null)
            if (!wrapped.isNullOrEmpty()) {
                val privateKey = store.getKey(alias, null) as? PrivateKey ?: throw PasswordKeyUnavailableException()
                val decoded = Base64.decode(wrapped, Base64.NO_WRAP)
                val unwrapped = Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
                    init(Cipher.DECRYPT_MODE, privateKey); doFinal(decoded)
                }
                if (unwrapped.size != 32) { unwrapped.fill(0); throw PasswordKeyUnavailableException() }
                material = unwrapped
                return unwrapped
            }
            // Existing rows with missing material remain recoverable; never silently replace their key.
            if (!allowCreate) throw PasswordKeyUnavailableException()
            if (!store.containsAlias(alias)) {
                KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore").apply {
                    initialize(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setKeySize(2048).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1).build())
                }.generateKeyPair()
            }
            val publicKey = store.getCertificate(alias)?.publicKey ?: throw PasswordKeyUnavailableException()
            val privateKey = store.getKey(alias, null) as? PrivateKey ?: throw PasswordKeyUnavailableException()
            val salt = preferences.getString("pst", null)?.takeIf(String::isNotEmpty) ?: UUID.randomUUID().toString().replace("-", "")
            val randomText = UUID.randomUUID().toString().replace("-", "").toCharArray()
            val generated = try { PasswordCipher.derive(randomText, salt) } finally { randomText.fill('\u0000') }
            try {
                val envelope = Cipher.getInstance("RSA/ECB/PKCS1Padding").run { init(Cipher.ENCRYPT_MODE, publicKey); doFinal(generated) }
                val restored = Cipher.getInstance("RSA/ECB/PKCS1Padding").run { init(Cipher.DECRYPT_MODE, privateKey); doFinal(envelope) }
                val valid = try { MessageDigest.isEqual(generated, restored) } finally { restored.fill(0) }
                if (!valid || envelope.isEmpty() || !preferences.edit().putString("psk", Base64.encodeToString(envelope, Base64.NO_WRAP))
                        .putString("pst", salt).commit()) throw PasswordKeyUnavailableException()
                material = generated
                return generated
            } catch (error: Exception) { generated.fill(0); throw error }
        } catch (error: PasswordCryptoException) { throw error }
        catch (error: Exception) { throw PasswordKeyUnavailableException(error) }
    }

    override fun close() = synchronized(LOCK) {
        closed = true; material?.fill(0); material = null
    }
    private companion object { val LOCK = Any() }
}
