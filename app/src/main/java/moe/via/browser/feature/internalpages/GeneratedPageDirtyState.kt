package moe.via.browser.feature.internalpages

import android.content.Context

/** w9.n's process word includes generated-page and page-policy flags, with unknown bits intact. */
object GeneratedPageDirtyState {
    const val HOME = 1
    const val HOME_STYLE = 4
    const val BOOKMARKS = 8
    const val HISTORY = 16
    const val ABOUT = 32
    const val CATALOG = 64
    const val WEB_SETTINGS = 128
    const val PAGE_EFFECTS = 256
    const val ALL_GENERATED = 381
    const val SECONDARY = 384

    private var flags = 0
    private var initialized = false
    private var pendingMask = 0

    @Synchronized fun initialize(context: Context) {
        if (initialized) return
        val values = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
        val saved = (values.all["datachecker2"] as? Number)?.toInt() ?: 0
        flags = (saved and pendingMask.inv()) or (flags and pendingMask)
        pendingMask = 0
        initialized = true
    }

    /** ua.p1/k2 persist the shared word, rather than separate last-writer page booleans. */
    @Synchronized fun persist(context: Context) {
        initialize(context)
        context.getSharedPreferences("browser", Context.MODE_PRIVATE).edit().putInt("datachecker2", flags).apply()
    }

    /** w9.b.b requires the complete supplied mask; callers normally pass a single page bit. */
    @Synchronized fun isDirty(mask: Int): Boolean = (flags and mask) == mask
    @Synchronized fun markDirty(mask: Int) { update(mask, true) }
    @Synchronized fun clear(mask: Int) { update(mask, false) }
    fun markAll() = markDirty(ALL_GENERATED)
    fun markSecondary() = markDirty(SECONDARY)

    private fun update(mask: Int, dirty: Boolean) {
        flags = if (dirty) flags or mask else flags and mask.inv()
        if (!initialized) pendingMask = pendingMask or mask
    }
}
