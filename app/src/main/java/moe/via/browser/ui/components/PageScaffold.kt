package moe.via.browser.ui.components

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.FrameLayout
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import moe.via.browser.ui.components.insets.NativePageInsets
import moe.via.browser.ui.components.toolbar.NativePageToolbar
import moe.via.browser.ui.theme.palette

/** Shared native page chrome; each feature owns its content and navigation state. */
class PageScaffold(private val activity: Activity, title: String, onBack: () -> Unit = { activity.finish() }, applyInsets: Boolean = true) {
    val titleView: TextView
    val toolbar: NativePageToolbar
    val body: LinearLayout
    val scroll: ScrollView
    private val contentHost = FrameLayout(activity)
    private var insets: NativePageInsets? = null
    val root = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(activity.palette.canvas)
    }

    init {
        toolbar = NativePageToolbar(activity, title, onBack)
        titleView = toolbar.titleView
        root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))
        body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll = ScrollView(activity).apply { isFillViewport = true; addView(body) }
        contentHost.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        root.addView(contentHost, LinearLayout.LayoutParams(-1, 0, 1f))
        if (applyInsets) insets = NativePageInsets(root, toolbar)
    }

    fun addTextAction(text: String, action: () -> Unit) {
        toolbar.addTextAction(text, action)
    }
    fun addIconAction(icon: Int, label: String, action: () -> Unit): ImageView = toolbar.addIconAction(icon, label, action)
    fun showContent(view: View = scroll, bottomInsets: View? = null) {
        insets?.bottom(bottomInsets)
        if (contentHost.childCount == 1 && contentHost.getChildAt(0) === view) return
        contentHost.removeAllViews()
        (view.parent as? ViewGroup)?.removeView(view)
        contentHost.addView(view, FrameLayout.LayoutParams(-1, -1))
    }
}
