package moe.via.browser.feature.sync.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/** Local configuration only. AndroidKeyStore keeps installation keys out of exported settings. */
internal class SyncCredentialCipher(context: Context, purpose: String) {
    private val alias = context.applicationContext.packageName + ".SyncCredentials.$purpose.v1"
    fun encrypt(value: String, allowCreate: Boolean): String = synchronized(LOCK) {
        try {
            val key = key(allowCreate)
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding").apply { init(Cipher.ENCRYPT_MODE, key) }
            val bytes = value.toByteArray(Charsets.UTF_8)
            try { Base64.encodeToString(cipher.iv + cipher.doFinal(bytes), Base64.NO_WRAP) } finally { bytes.fill(0) }
        } catch (error: Exception) { throw IOException("无法保存同步凭据。", error) }
    }
    fun decrypt(value: String): String = synchronized(LOCK) {
        try {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            require(bytes.size >= 32 && (bytes.size - 16) % 16 == 0)
            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding").apply {
                init(Cipher.DECRYPT_MODE, key(false), IvParameterSpec(bytes, 0, 16))
            }
            val plain = cipher.doFinal(bytes, 16, bytes.size - 16)
            try { String(plain, Charsets.UTF_8) } finally { plain.fill(0) }
        } catch (error: Exception) { throw IOException("同步凭据不可用，已保留原配置。", error) }
    }
    private fun key(allowCreate: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        check(allowCreate) { "Missing sync configuration key" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7).build())
        }.generateKey()
    }
    private companion object { val LOCK = Any() }
}
