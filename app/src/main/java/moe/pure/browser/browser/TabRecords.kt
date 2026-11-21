package moe.pure.browser.browser

object TabFlags {
    const val PRIVATE = 1
    const val OPEN = 2
    const val SELECTED = 4
}

/** Persistent identity is independent of the current runtime tab's small integer ID. */
data class TabEntry(
    val id: String,
    val url: String,
    val title: String,
    val filePath: String? = null,
    val flags: Int = 0,
    val lastVisitedAt: Long = 0,
    val home: Boolean = false,
    val desktop: Boolean = false,
    val scrollX: Int = 0,
    val scrollY: Int = 0
)

/** Captured on the UI thread, then committed by the serialized persistence worker. State is Native outer bytes. */
data class TabWrite(
    val persistentId: String? = null,
    val url: String,
    val title: String,
    val persistable: Boolean = true,
    val isPrivate: Boolean = false,
    val isSelected: Boolean = false,
    val visitTime: Long = 0,
    val state: ByteArray? = null,
    val home: Boolean = false,
    val desktop: Boolean = false,
    val scrollX: Int = 0,
    val scrollY: Int = 0
)

/** A worker resolves file bytes or creates Native's URL/TITLE-only fallback before runtime construction. */
data class TabRestoration(val entry: TabEntry, val state: ByteArray)
