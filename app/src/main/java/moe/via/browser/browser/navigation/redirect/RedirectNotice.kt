package moe.via.browser.browser.navigation.redirect

import android.app.Activity
import moe.via.browser.ui.message.MessageContent
import moe.via.browser.ui.message.MessageOwner

/** The shared manager owns timeout and fade-tail interaction; redirect keeps only its navigation state. */
internal class RedirectNotice(activity: Activity) {
    private val owner = MessageOwner(activity)
    fun show(text: String, action: String, clicked: () -> Unit, timeoutMillis: Long = 3500,
        onRemoved: () -> Unit = {}): Boolean = owner.show(MessageContent(text, action, clicked = clicked,
        timeoutMillis = timeoutMillis, removed = onRemoved))
    fun close(animate: Boolean = false, finished: () -> Unit = {}) = owner.close(animate, finished)
}
