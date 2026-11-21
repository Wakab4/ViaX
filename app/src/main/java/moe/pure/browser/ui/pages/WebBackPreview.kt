package moe.pure.browser.ui.pages

import android.view.View

internal interface PredictiveBackPreview {
    fun progress(value: Float)
    fun cancel()
    fun complete()
}

/** Keeps WebView history untouched until the system gesture commits. */
internal class WebBackPreview(private val page: View, private val navigateBack: () -> Unit) : PredictiveBackPreview {
    private val distance = page.width * .28f

    override fun progress(value: Float) {
        page.animate().cancel()
        val fraction = value.coerceIn(0f, 1f)
        page.translationX = distance * fraction
        page.alpha = 1f - .12f * fraction
    }

    override fun cancel() {
        page.animate().cancel()
        page.animate().translationX(0f).alpha(1f).setDuration(160L).start()
    }

    override fun complete() {
        page.animate().cancel()
        page.animate().translationX(distance).alpha(.88f).setDuration(90L).withEndAction {
            page.translationX = 0f
            page.alpha = 1f
            navigateBack()
        }.start()
    }
}
