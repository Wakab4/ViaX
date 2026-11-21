package moe.pure.browser.feature.scripts.runtime

import android.content.Context
import android.text.TextUtils

/** Native installed-addon discovery uses homepageUrl and preserves its numeric text and repository order. */
internal object NativeScriptPageApi {
    fun installedAddonIds(context: Context): String {
        val prefix = "https://app.viayoo.com/addons/"
        val rows = UserScriptEngine.get(context).repository.addonHomepages()
        return buildString {
            append('[')
            rows.forEach { address ->
                if (address != null && address.startsWith(prefix)) {
                    val id = address.substring(prefix.length)
                    if (TextUtils.isDigitsOnly(id)) append(',').append(id)
                }
            }
            if (length > 1) deleteCharAt(1)
            append(']')
        }
    }
}
