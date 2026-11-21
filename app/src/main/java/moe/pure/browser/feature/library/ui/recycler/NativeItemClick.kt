package moe.pure.browser.feature.library.ui.recycler

import android.os.SystemClock
import android.view.View

/** y5.a delegates clicks to a fresh v5.c on every full bind; its interval is strictly over 300 ms. */
internal fun View.bindNativeItemClick(clicked: () -> Unit) {
    var previous: Long? = null
    setOnClickListener {
        val now = SystemClock.elapsedRealtime()
        val last = previous
        if (last == null || kotlin.math.abs(now - last) > 300L) { previous = now; clicked() }
    }
}
