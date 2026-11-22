package moe.via.browser.feature.library.navigation

/** l8.a's per-URL destination. CURRENT applies the browser's input/internal-route policy. */
enum class LibraryOpenWhere(val nativeValue: Int) {
    CURRENT(0), FOREGROUND(1), BACKGROUND(2), FORCE_CURRENT(3);

    companion object {
        fun fromNative(value: Int): LibraryOpenWhere = entries.firstOrNull { it.nativeValue == value } ?: CURRENT
    }
}

data class LibraryOpenRequest(val url: String, val where: LibraryOpenWhere)
