package moe.pure.browser.feature.shortcut

import android.os.Bundle

data class ShortcutDraft(val url: String, val title: String, val icon: String? = null, val iconFromUrl: String? = null) {
    fun save() = Bundle().apply { putString("url", url); putString("title", title); putString("icon", icon); putString("icon_origin", iconFromUrl) }
    companion object {
        fun restore(bundle: Bundle) = ShortcutDraft(bundle.getString("url").orEmpty(), bundle.getString("title").orEmpty(), bundle.getString("icon"), bundle.getString("icon_origin"))
    }
}
