package moe.via.browser.feature.site.ui.common

sealed interface NativeSiteRow {
    val id: Int
    val title: CharSequence
    data class Toggle(override val id: Int, override val title: CharSequence, val checked: Boolean,
        val disabled: Boolean = false, val detail: CharSequence? = null) : NativeSiteRow
    data class Link(override val id: Int, override val title: CharSequence, val detail: CharSequence? = null,
        val disabled: Boolean = false) : NativeSiteRow
    data class Section(override val id: Int, override val title: CharSequence) : NativeSiteRow
    data class Add(override val id: Int, override val title: CharSequence) : NativeSiteRow
    data class Slider(override val id: Int, override val title: CharSequence, val value: Int,
        val highlight: Int = 100) : NativeSiteRow
}
