package moe.pure.browser.feature.passwords.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle

internal object PasswordClipboard {
    fun copy(context: Context, value: String, sensitive: Boolean) {
        val clip = ClipData.newPlainText(null, value)
        if (sensitive) clip.description.extras = PersistableBundle().apply {
            putBoolean(if (Build.VERSION.SDK_INT >= 33) ClipDescription.EXTRA_IS_SENSITIVE else "android.content.extra.IS_SENSITIVE", true)
        }
        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
    }
}
