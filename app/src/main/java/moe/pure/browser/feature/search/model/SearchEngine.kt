package moe.pure.browser.feature.search.model

data class SearchEngine(
    val id: Int,
    val name: String,
    val mobileTemplate: String,
    val desktopTemplate: String = mobileTemplate,
    val custom: Boolean = id > 0 || id <= -999,
    val note: String? = null,
    val flags: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
) {
    fun template(desktop: Boolean = false): String = if (desktop) desktopTemplate else mobileTemplate
}
