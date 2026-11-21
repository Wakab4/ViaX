package moe.pure.browser.ui.components

import android.app.Activity
import android.widget.FrameLayout
import moe.pure.browser.ui.message.MessageContent
import moe.pure.browser.ui.message.MessageOwner

/** Source-compatible facade over the process-wide Native short-message manager. */
class BrowserMessage(activity: Activity, parent: FrameLayout? = null) {
    private val owner = MessageOwner(activity, parent)
    fun show(message: String, action: String? = null, clicked: (() -> Unit)? = null) {
        owner.show(MessageContent(message, action, clicked = clicked))
    }
    fun close(animate: Boolean = false) = owner.close(animate)
}
