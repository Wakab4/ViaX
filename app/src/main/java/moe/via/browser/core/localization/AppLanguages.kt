package moe.via.browser.core.localization

import android.content.Context
import moe.via.browser.R

/** Languages available for the application interface. The empty tag follows the device. */
internal object AppLanguages {
    private val names = linkedMapOf(
        "zh-CN" to "中文(简体)",
        "zh-TW" to "中文(繁體)",
        "en" to "English",
        "ja" to "日本語"
    )

    fun choices(context: Context): Map<String, String> =
        linkedMapOf("" to context.getString(R.string.settings_follow_system)) + names

    fun selected(tag: String): String = tag.takeIf(names::containsKey).orEmpty()

    fun label(context: Context, tag: String): String =
        names[selected(tag)] ?: context.getString(R.string.settings_follow_system)
}
