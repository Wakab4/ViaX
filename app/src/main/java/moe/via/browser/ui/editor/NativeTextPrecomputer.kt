package moe.via.browser.ui.editor

import android.text.PrecomputedText
import android.widget.TextView
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

/** x8.g.g/b/c uses one process executor and the original 16 KiB threshold/delayed completion. */
internal object NativeTextPrecomputer {
    private val worker by lazy { Executors.newFixedThreadPool(1) }
    fun set(view: TextView, text: String, completed: () -> Unit) {
        if (text.length < 16384) { view.text = text; completed(); return }
        view.text = text.substring(0, 16384)
        val params = view.textMetricsParams; val reference = WeakReference(view)
        val delay = minOf(50, text.length / 16384) * 10L
        worker.execute {
            val target = reference.get() ?: return@execute
            val prepared = PrecomputedText.create(text, params)
            target.post {
                val current = reference.get() ?: return@post
                current.text = prepared
                current.postDelayed({ if (reference.get() != null) completed() }, delay)
            }
        }
    }
}
