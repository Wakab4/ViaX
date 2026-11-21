package moe.pure.browser.web

import android.view.MotionEvent
import android.webkit.WebView
import moe.pure.browser.R
import moe.pure.browser.ui.dialog.DialogAnchor

object WebTouchPoint {
    fun observe(view: WebView) {
        view.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) view.setTag(R.id.web_context_point, DialogAnchor(event.rawX.toInt(), event.rawY.toInt()))
            false
        }
    }
    fun last(view: WebView) = view.getTag(R.id.web_context_point) as? DialogAnchor
}
