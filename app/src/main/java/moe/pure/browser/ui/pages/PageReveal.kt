package moe.pure.browser.ui.pages

import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.withSign
import moe.pure.browser.R

/** Native y.b keeps the actual preceding view, exposing it only while a drag needs it. */
internal class PageReveal(private val view: View, private val hidden: () -> Boolean) : AutoCloseable {
    private val originalVisibility = view.visibility
    private val originalAccessibility = view.importantForAccessibility
    private val displacement = view.resources.getDimension(R.dimen.page_previous_offset).let { max(view.width * .3f, abs(it)).withSign(it) }
    private var completed = false
    init { view.visibility = View.VISIBLE; view.translationX = displacement }
    fun progress(value: Float) {
        if (completed) return
        if (value >= 1f) { completed = true; view.animate().cancel(); view.translationX = 0f }
        else if (!hidden()) {
            view.animate().translationX(0f).setDuration(120).setInterpolator(DecelerateInterpolator()).start()
        } else { view.visibility = View.VISIBLE; view.translationX = displacement * (1f - value) }
    }
    fun complete() { completed = true; view.animate().cancel(); view.translationX = 0f }
    override fun close() {
        view.animate().cancel(); view.translationX = 0f; view.importantForAccessibility = originalAccessibility
        if (!completed) view.visibility = originalVisibility
    }
}
