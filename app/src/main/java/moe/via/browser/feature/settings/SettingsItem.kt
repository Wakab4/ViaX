package moe.via.browser.feature.settings

import android.view.View

/** List identity follows a6.k; callbacks and widget construction belong to the row content. */
internal sealed interface SettingsItem {
    val row: SettingRow

    data class Entry(val id: Int, override val row: SettingRow) : SettingsItem
    data class Section(override val row: SettingRow.Section) : SettingsItem
    data class Content(val key: Any, val model: Any, override val row: SettingRow) : SettingsItem

    companion object {
        fun link(id: Int, title: String, detail: String = "", enabled: Boolean = true,
            highlighted: Boolean = false, onClick: () -> Unit) =
            Entry(id, SettingRow.Link(title, detail, enabled, highlighted = highlighted, onClick = onClick))

        fun toggle(id: Int, title: String, checked: Boolean, detail: String = "", enabled: Boolean = true,
            updateCheckOnClick: Boolean = true, changed: (Boolean) -> Unit) =
            Entry(id, SettingRow.Toggle(title, checked, detail, enabled, updateCheckOnClick, changed))

        fun radio(id: Int, title: String, checked: Boolean, onLongClick: (() -> Unit)? = null,
            detail: String = "", onClick: () -> Unit) =
            Entry(id, SettingRow.Radio(title, checked, onLongClick, detail = detail, onClick = onClick))

        fun section(title: String) = Section(SettingRow.Section(title))
        fun content(key: Any, model: Any, create: () -> View) = Content(key, model, SettingRow.Custom(create))
    }
}

/** ib.y/ib.z: preview identity is its model class; payload bits describe the three visible values. */
internal data class ToolbarPreviewState(val layout: Int, val tabs: Boolean, val address: Int) {
    fun changes(next: ToolbarPreviewState): Int =
        (if (layout != next.layout) 1 else 0) or
            (if (tabs != next.tabs) 2 else 0) or
            (if (address != next.address) 4 else 0)
}
