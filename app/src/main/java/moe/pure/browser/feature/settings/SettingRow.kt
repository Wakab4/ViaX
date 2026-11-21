package moe.pure.browser.feature.settings

import android.view.View

sealed interface SettingRow {
    data class Link(val title: String, val detail: String = "", val enabled: Boolean = true, val onLongClick: (() -> Unit)? = null, val highlighted: Boolean = false, val onClick: () -> Unit) : SettingRow
    data class Radio(val title: String, val checked: Boolean, val onLongClick: (() -> Unit)? = null, val typeface: android.graphics.Typeface? = null, val enabled: Boolean = true, val dimmed: Boolean = false, val detail: String = "", val onClick: () -> Unit) : SettingRow
    data class Toggle(val title: String, val checked: Boolean, val detail: String = "", val enabled: Boolean = true, val onChanged: (Boolean) -> Unit) : SettingRow
    data class Section(val title: String) : SettingRow
    data class Custom(val create: () -> View) : SettingRow
}
