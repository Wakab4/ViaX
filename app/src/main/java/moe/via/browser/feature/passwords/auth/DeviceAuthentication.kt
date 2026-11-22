package moe.via.browser.feature.passwords.auth

import moe.via.browser.ui.message.BrowserMessages

import android.app.Activity
import android.app.KeyguardManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
/** System confirmation is a UI gate; it does not derive keys or bind decryption to a CryptoObject. */
@Suppress("DEPRECATION")
class DeviceAuthentication private constructor(private val activity: Activity, owner: Fragment?) {
    constructor(activity: Activity) : this(activity, null)
    constructor(owner: Fragment) : this(owner.requireActivity(), owner)
    private data class Request(val success: () -> Unit, val cancelled: () -> Unit, val error: (String?) -> Unit)
    private var pending: Request? = null
    private var signal: CancellationSignal? = null
    private var generation = 0L
    private var disposed = false
    private val credentialLauncher = owner?.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { completeCredential(it.resultCode) }

    fun authenticate(title: String, description: String, onSuccess: () -> Unit,
        onCancelled: () -> Unit = {}, onError: (String?) -> Unit = { text ->
            if (!text.isNullOrEmpty()) BrowserMessages.show(activity, text)
        }) {
        if (disposed || activity.isDestroyed || activity.isFinishing) return
        cancel()
        if (System.currentTimeMillis() < confirmedUntil) { onSuccess(); return }
        pending = Request(onSuccess, onCancelled, onError)
        val current = generation
        try {
            val builder = BiometricPrompt.Builder(activity).setTitle(title).setSubtitle(description)
            when {
                Build.VERSION.SDK_INT >= 30 -> builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                Build.VERSION.SDK_INT >= 29 -> builder.setDeviceCredentialAllowed(true)
                else -> builder.setNegativeButton("使用设备凭据", activity.mainExecutor) { _, _ ->
                    if (current == generation && pending != null) credentialFromPrompt(title, description)
                }
            }
            val cancellation = CancellationSignal().also { signal = it }
            builder.build().authenticate(cancellation, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (current == generation) success(true)
                }
                override fun onAuthenticationError(code: Int, text: CharSequence) {
                    if (current != generation || pending == null) return
                    if (Build.VERSION.SDK_INT == 28 && code in setOf(11, 12, 14)) {
                        credentialFromPrompt(title, description); return
                    }
                    when (code) {
                        11, 14 -> success(false)
                        10, 13 -> cancelled()
                        12 -> credential(title, description)
                        else -> error(text.toString())
                    }
                }
                override fun onAuthenticationFailed() {
                    // Native resolves this attempt as an error without a message.
                    if (Build.VERSION.SDK_INT >= 29 && current == generation) error(null)
                }
            })
        } catch (_: Exception) {
            if (Build.VERSION.SDK_INT == 28) credentialFromPrompt(title, description) else credential(title, description)
        }
    }

    /** API 28 uses the platform prompt instead of Native's removed legacy fingerprint dialog API. */
    private fun credentialFromPrompt(title: String, description: String) {
        generation++
        signal?.cancel(); signal = null
        credential(title, description)
    }

    private fun credential(title: String, description: String) {
        val keyguard = activity.getSystemService(KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isKeyguardSecure) { success(false); return }
        val intent = keyguard.createConfirmDeviceCredentialIntent(title, description)
        if (intent == null) { success(false); return }
        try { credentialLauncher?.launch(intent) ?: activity.startActivityForResult(intent, CREDENTIAL_REQUEST) }
        catch (_: Exception) { error("无法确认设备凭据") }
    }

    fun onActivityResult(request: Int, result: Int): Boolean {
        if (request != CREDENTIAL_REQUEST) return false
        completeCredential(result)
        return true
    }
    private fun completeCredential(result: Int) {
        if (pending != null) { if (result == Activity.RESULT_OK) success(true) else cancelled() }
    }

    private fun success(cache: Boolean) {
        val callback = pending ?: return
        if (cache) confirmedUntil = System.currentTimeMillis() + 180_000
        cancel(); callback.success()
    }
    private fun cancelled() { val callback = pending ?: return; cancel(); callback.cancelled() }
    private fun error(message: String?) { val callback = pending ?: return; cancel(); callback.error(message) }
    fun cancel() {
        generation++; pending = null
        signal?.cancel(); signal = null
    }
    fun dispose() { disposed = true; cancel() }

    companion object {
        private const val CREDENTIAL_REQUEST = 6380
        @Volatile private var confirmedUntil = 0L
    }
}
