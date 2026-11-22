package moe.via.browser.feature.firstrun.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Space
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.components.SkinIconBinder
import moe.via.browser.ui.components.applySafeArea
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

/** A Native document WebView without the browser's command bridges. */
@Suppress("DEPRECATION")
internal class FirstRunDocumentView(
    private val activity: Activity,
    title: String,
    private val url: String,
    private val close: () -> Unit
) {
    val root = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(activity.palette.canvas)
        applySafeArea()
    }
    private val icons = SkinIconBinder(activity)
    private val heading = NativeTypography.text(activity).apply {
        text = title
        textSize = 16f
        setTextColor(activity.palette.text)
        NativeTypography.style(this, android.graphics.Typeface.BOLD)
        gravity = Gravity.CENTER_VERTICAL
        setSingleLine()
        ellipsize = TextUtils.TruncateAt.END
        textDirection = View.TEXT_DIRECTION_LOCALE
    }
    private val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal)
    private val loadingProgress = FirstRunProgress(progress)
    private val page = FrameLayout(activity)
    private var webView: WebView? = null
    private var disposed = false
    private var started = false

    init {
        val header = FrameLayout(activity)
        val row = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
        val back = ImageView(activity).apply {
            imageTintList = ColorStateList.valueOf(activity.palette.icon)
            setPadding(activity.dp(13), 0, activity.dp(13), 0)
            contentDescription = activity.getString(R.string.ref_first_run_back)
            background = activity.touchFeedback()
            setOnClickListener { back() }
        }
        icons.image(back, R.drawable.ref_ic_back)
        row.addView(back, LinearLayout.LayoutParams(activity.dp(48), -1).apply {
            setMargins(activity.dp(1), activity.dp(1), activity.dp(1), activity.dp(1))
        })
        row.addView(heading, LinearLayout.LayoutParams(0, -1, 24f))
        row.addView(Space(activity), LinearLayout.LayoutParams(0, -1, 1f))
        header.addView(row, FrameLayout.LayoutParams(-1, -1))
        header.addView(View(activity).apply { setBackgroundColor(0x30808080) },
            FrameLayout.LayoutParams(-1, 2, Gravity.BOTTOM))
        root.addView(header, LinearLayout.LayoutParams(-1, activity.dp(54)))
        root.addView(page, LinearLayout.LayoutParams(-1, 0, 1f))

        val confirm = NativeTypography.text(activity).apply {
            setText(android.R.string.ok)
            textSize = 14f
            NativeTypography.style(this, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(activity.palette.accent)
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(0x30808080))
                addState(intArrayOf(android.R.attr.state_focused), ColorDrawable(0x30808080))
                addState(intArrayOf(), ColorDrawable(android.graphics.Color.TRANSPARENT))
            }
            setOnClickListener { close() }
        }
        createWebView()
        page.addView(progress, FrameLayout.LayoutParams(-1, activity.dp(2), Gravity.TOP))
        page.addView(confirm, FrameLayout.LayoutParams(-1, activity.dp(48), Gravity.BOTTOM))
    }

    /** Called after the host attaches this page; no request is made by the welcome view. */
    fun start() {
        if (disposed || started) return
        started = true
        try {
            webView?.loadUrl(url)
        } catch (_: RuntimeException) {
            showFailure()
        } catch (_: LinkageError) {
            showFailure()
        }
    }

    fun back() {
        val web = webView
        if (web != null && web.canGoBack()) web.goBack() else close()
    }

    fun resume() { if (!disposed) webView?.onResume() }
    fun pause() { if (!disposed) webView?.onPause() }

    fun dispose() {
        if (disposed) return
        disposed = true
        loadingProgress.dispose()
        releaseWebView()
        icons.close()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView() {
        try {
            val web = WebView(activity)
            webView = web
            web.apply {
                moe.via.browser.webview.NativeWebViewDefaults.apply(this)
                isHorizontalScrollBarEnabled = false
                settings.apply {
                    javaScriptEnabled = true
                    javaScriptCanOpenWindowsAutomatically = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    loadWithOverviewMode = false
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, value: Int) {
                        if (!disposed && view === webView) loadingProgress.update(value)
                    }
                    override fun onReceivedTitle(view: WebView?, value: String?) {
                        if (!disposed && view === webView && value != null) heading.text = value
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
                        request?.url?.toString()?.let { !allowed(it) } ?: false
                    override fun shouldOverrideUrlLoading(view: WebView?, value: String?): Boolean =
                        value?.let { !allowed(it) } ?: true
                    override fun onPageStarted(view: WebView?, value: String?, favicon: Bitmap?) {
                        if (!disposed && view === webView) loadingProgress.update(0)
                    }
                    override fun onPageFinished(view: WebView?, value: String?) {
                        if (!disposed && view === webView) loadingProgress.update(100)
                    }
                }
            }
            page.addView(web, FrameLayout.LayoutParams(-1, -1).apply { bottomMargin = activity.dp(48) })
        } catch (_: RuntimeException) {
            showFailure()
        } catch (_: LinkageError) {
            showFailure()
        }
    }

    private fun allowed(value: String): Boolean = value.startsWith("https://", true) ||
        value.startsWith("http://", true) || value.startsWith("file://", true)

    private fun showFailure() {
        releaseWebView()
        loadingProgress.hide()
        if (page.findViewWithTag<View>(FAILURE_TAG) != null) return
        page.addView(NativeTypography.text(activity).apply {
            tag = FAILURE_TAG
            setText(R.string.ref_first_run_document_failure)
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(activity.palette.secondary)
            setPadding(activity.dp(24), activity.dp(24), activity.dp(24), activity.dp(24))
        }, 0, FrameLayout.LayoutParams(-1, -1).apply { bottomMargin = activity.dp(48) })
    }

    private fun releaseWebView() {
        val web = webView ?: return
        webView = null
        (web.parent as? ViewGroup)?.removeView(web)
        runCatching { web.stopLoading() }
        runCatching { web.webChromeClient = null; web.webViewClient = WebViewClient() }
        runCatching { web.destroy() }
    }

    companion object { private const val FAILURE_TAG = "first_run_document_failure" }
}
