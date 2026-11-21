package moe.pure.browser.feature.reader

import android.webkit.WebView
import moe.pure.browser.feature.internalpages.GeneratedPageDirtyState
import java.lang.ref.WeakReference

/** R7/l3/U7/M4: the captured view is a lifetime gate, while each operation targets current. */
internal class ReaderPageEffects(
    private val current: () -> WebView?,
    private val probe: (WebView, (Int) -> Unit) -> Unit,
    private val apply: () -> Unit
) : AutoCloseable {
    private var closed = false

    fun consume() {
        if (closed || !GeneratedPageDirtyState.isDirty(GeneratedPageDirtyState.PAGE_EFFECTS)) return
        GeneratedPageDirtyState.clear(GeneratedPageDirtyState.PAGE_EFFECTS)
        val original = current() ?: return
        val reference = WeakReference(original)
        val owner = WeakReference(this)
        original.post {
            val effects = owner.get()?.takeUnless { it.closed } ?: return@post
            if (reference.get() == null) return@post
            val selected = effects.current()?.takeIf { it.isShown } ?: return@post
            effects.probe(selected) { state ->
                val live = owner.get()?.takeUnless { it.closed }
                if (live != null && reference.get() != null && state == 3) live.apply()
            }
        }
    }

    override fun close() { closed = true }
}
