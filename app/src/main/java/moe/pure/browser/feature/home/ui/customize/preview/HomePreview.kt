package moe.pure.browser.feature.home.ui.customize.preview

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.feature.home.render.HomeDocumentCache
import moe.pure.browser.feature.home.render.HomeHtmlSource
import moe.pure.browser.feature.home.render.HomeRenderContext
import moe.pure.browser.feature.home.ui.customize.HomeCustomizeState
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.tasks.NativeLifecycleJobs
import moe.pure.browser.web.lifecycle.NativeWebViewDisposal

/** d9.q uses one configuration snapshot for two independently scheduled document branches. */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
internal class HomePreview(private val owner: Fragment, activity: Activity, private val host: FrameLayout,
    private val model: HomeCustomizeState) {
    private val cache = HomeDocumentCache(activity)
    private val jobs = NativeLifecycleJobs { owner.viewLifecycleOwner.lifecycle }
    val view = WebView(activity).apply {
        setOnTouchListener { _, _ -> true }
        isHorizontalScrollBarEnabled = false; isVerticalScrollBarEnabled = false
        setBackgroundColor(Color.TRANSPARENT); scaleX = .95f; scaleY = .95f
        isClickable = false; isFocusable = false
        settings.apply {
            javaScriptEnabled = true; cacheMode = WebSettings.LOAD_NO_CACHE
            allowContentAccess = true; allowFileAccess = true
            @Suppress("DEPRECATION")
            allowFileAccessFromFileURLs = true
        }
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) = HomePreviewStyles.intercept(request.url.toString(), owner::requireContext)
            @Deprecated("Legacy WebView dispatch") override fun shouldInterceptRequest(view: WebView, url: String?) = HomePreviewStyles.intercept(url, owner::requireContext)
        }
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) { outline.setRoundRect(0, 0, view.width, view.height, activity.dp(18).toFloat()) }
        }
        clipToOutline = true
    }
    private val background = HomePreviewBackground(owner, view, model, jobs)
    init { host.addView(view, FrameLayout.LayoutParams(-1, -1)) }
    fun load() = document { view.loadUrl(it) }
    fun refresh() { background.bind(); document { view.reload() } }
    fun backgroundChanged() = background.refresh()
    fun postBackground() { view.post { background.refresh() } }
    private fun document(loaded: (String) -> Unit) {
        val context = owner.requireContext()
        val config = model.preferences.config(isRtl = HomeRenderContext.isRtl(context))
        jobs.zip({ cache.style(config) }, { cache.html(config, { HomeRenderContext.options(context) }) { HomeHtmlSource.read(context) } },
            failed = { NativeLog.error(null, it) }) { _, html -> loaded("file://${html.absolutePath}") }
    }
    fun clearCache() = view.clearCache(false)
    fun pause() = view.onPause()
    fun resume() = view.onResume()
    fun dispose() {
        jobs.close(); host.removeAllViews(); NativeWebViewDisposal.destroy(view)
    }
}
