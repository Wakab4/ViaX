package moe.via.browser.feature.home.ui.favorites

import android.animation.ObjectAnimator
import android.os.SystemClock
import android.view.animation.PathInterpolator
import android.widget.EditText
import moe.via.browser.R
import moe.via.browser.ui.components.dp

/** qa.h0.G3/o.m3 own a 300ms timestamp per editor and shake, rather than EditText.error. */
internal class FavoriteFormInput {
    private var lastSubmit = 0L

    fun begin(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSubmit < 300) return false
        lastSubmit = now
        return true
    }

    fun required(field: EditText): String {
        ObjectAnimator.ofFloat(field, "translationX", 0f, field.context.dp(24).toFloat(), 0f).apply {
            repeatCount = 2
            repeatMode = ObjectAnimator.RESTART
            duration = 280L
            interpolator = PathInterpolator(.2f, .2f, .8f, .8f)
            start()
        }
        return field.context.getString(R.string.ref_bookmark_form_required, field.hint)
    }
}
