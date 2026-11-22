package moe.via.browser.ui.components

import android.os.SystemClock
import android.view.View

/** v5.d triggers the toolbar action on the second tap within 300ms. */
internal object NativeToolbarTap {
    fun bind(view: View, action: () -> Unit) {
        var last = 0L
        view.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            if (now - last >= 300) last = now else { last = 0; action() }
        }
    }
}
