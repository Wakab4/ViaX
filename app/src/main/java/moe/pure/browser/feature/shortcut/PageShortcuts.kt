package moe.pure.browser.feature.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import moe.pure.browser.BrowserActivity
import moe.pure.browser.R
import java.security.MessageDigest

/** Platform integration is separate from the editor and icon decoding. */
class PageShortcuts(private val context: Context) {
    enum class PinResult { REQUESTED, UPDATED, FAILED }
    private val manager get() = context.getSystemService(ShortcutManager::class.java)
    val supported get() = manager.isRequestPinShortcutSupported
    fun definition(draft: ShortcutDraft, bitmap: Bitmap?): ShortcutInfo {
        require(draft.url.isNotBlank() && draft.title.isNotEmpty())
        val intent = Intent(context, BrowserActivity::class.java).apply {
            setPackage(context.packageName); action = Intent.ACTION_VIEW; data = Uri.parse(draft.url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val digest = MessageDigest.getInstance("MD5").digest(draft.url.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
        return ShortcutInfo.Builder(context, "sc_$digest").setShortLabel(draft.title).setIntent(intent)
            .setIcon(if (bitmap == null) Icon.createWithResource(context, R.drawable.ic_browser) else Icon.createWithBitmap(bitmap)).build()
    }
    fun pin(draft: ShortcutDraft, bitmap: Bitmap?): PinResult {
        val info = definition(draft, bitmap)
        return if (manager.pinnedShortcuts.any { it.id == info.id }) {
            if (manager.updateShortcuts(listOf(info))) PinResult.UPDATED else PinResult.FAILED
        } else if (manager.requestPinShortcut(info, null)) PinResult.REQUESTED else PinResult.FAILED
    }
}
