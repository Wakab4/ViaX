package moe.pure.browser.feature.translation

import android.os.SystemClock

/** Process memory only; successful translations are not part of this 180-second draft. */
object TranslationDraftCache {
    data class Draft(val query: String, val source: String, val target: String)
    private var draft: Draft? = null
    private var expiresAt = 0L

    @Synchronized fun save(query: String, source: String, target: String) {
        draft = Draft(query, source, target)
        expiresAt = SystemClock.elapsedRealtime() + 180_000L
    }

    @Synchronized fun restore(): Draft? {
        if (SystemClock.elapsedRealtime() >= expiresAt) { draft = null; return null }
        return draft?.takeIf { it.source.isNotEmpty() && it.target.isNotEmpty() }
    }
}
