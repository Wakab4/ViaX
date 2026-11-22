package moe.via.browser.feature.search.ui

import android.os.SystemClock

/** Native address drafts are contextual process memory, with no browser database or disk entry. */
internal object SearchDraftCache {
    data class Draft(val initial: String, val text: String, val engineId: Int?)
    private var draft: Draft? = null
    private var expires = 0L

    fun read(initial: String): Draft? {
        if (SystemClock.elapsedRealtime() >= expires) { draft = null; return null }
        return draft?.takeIf { it.initial == initial }
    }

    fun save(initial: String, text: String, engineId: Int?) {
        if (text.isEmpty() || text == initial) { clear(); return }
        draft = Draft(initial, text, engineId)
        expires = SystemClock.elapsedRealtime() + 180_000L
    }

    fun clear() { draft = null; expires = 0L }
}
