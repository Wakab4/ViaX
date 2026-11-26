package moe.via.browser.feature.proxy

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class ProxySettings(val endpoint: ProxyEndpoint? = null, val enabled: Boolean = false)

internal class BrowserProxyStore(context: Context) {
    private val values = context.getSharedPreferences("browser_proxy", Context.MODE_PRIVATE)
    private val alias = "${context.packageName}.BrowserProxy.v1"

    fun read(): ProxySettings {
        val encoded = values.getString("endpoint", null) ?: return ProxySettings()
        val fields = JSONObject(encoded)
        val credentials = if (fields.has("credentials")) JSONObject(decrypt(fields.getString("credentials"))) else null
        return ProxySettings(ProxyEndpoint.create(ProxyProtocol.valueOf(fields.getString("protocol")),
            fields.getString("host"), fields.getInt("port"), credentials?.getString("username").orEmpty(),
            credentials?.getString("password").orEmpty()), values.getBoolean("enabled", false))
    }

    fun encode(endpoint: ProxyEndpoint): String = JSONObject().apply {
        put("protocol", endpoint.protocol.name)
        put("host", endpoint.host)
        put("port", endpoint.port)
        if (endpoint.hasCredentials) put("credentials", encrypt(JSONObject().apply {
            put("username", endpoint.username)
            put("password", endpoint.password)
        }.toString()))
    }.toString()

    fun write(enabled: Boolean, encoded: String?) {
        values.edit().putBoolean("enabled", enabled).apply {
            if (encoded != null) putString("endpoint", encoded)
        }.apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key(create = true)) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        require(bytes.size >= 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, bytes, 0, 12))
        }
        return String(cipher.doFinal(bytes, 12, bytes.size - 12), Charsets.UTF_8)
    }

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        check(create) { "Proxy credential key is unavailable" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}
