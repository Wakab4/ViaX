package moe.via.browser.ui.editor

import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.EditText
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.message.BrowserMessages

internal object NativeFieldFeedback {
    fun shake(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f, view.dp(24).toFloat(), 0f).apply {
            repeatCount = 2; repeatMode = ObjectAnimator.RESTART; duration = 280
            interpolator = PathInterpolator(.2f, .2f, .8f, .8f); start()
        }
    }
    fun required(editor: EditText, shake: Boolean = true) {
        if (shake) NativeFieldFeedback.shake(editor)
        BrowserMessages.show(editor.context, editor.context.getString(R.string.ref_search_page_required, editor.hint))
    }
}
