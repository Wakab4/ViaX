package moe.pure.browser.ui.message

import android.app.Activity
import android.widget.FrameLayout

/** A window/feature handle, not a private message queue. close only affects this owner's entries. */
class MessageOwner(private val activity: Activity, private val parent: FrameLayout? = null) {
    private val identity = Any()
    fun show(content: MessageContent): Boolean = ProcessMessageManager.show(identity, activity, parent, content)
    fun close(animate: Boolean = false, finished: () -> Unit = {}) = ProcessMessageManager.close(identity, animate, finished)
}
