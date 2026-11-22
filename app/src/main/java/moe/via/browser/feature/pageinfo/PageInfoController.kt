package moe.via.browser.feature.pageinfo

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.content.res.ColorStateList
import android.net.Uri
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.browser.navigation.AddressResolver
import moe.via.browser.feature.home.HomeDocument
import moe.via.browser.feature.site.SiteDomains
import moe.via.browser.feature.site.ui.SiteSettingsNavigation
import moe.via.browser.feature.site.data.SiteSettingsRepository
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.ui.theme.window.ChromeLayoutPolicy
import moe.via.browser.ui.components.ContentOverlay
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

class PageInfoController(private val activity: Activity, parent: FrameLayout,
    private val sites: SiteSettingsRepository, private val actions: Actions) {
    interface Actions {
        fun qrCode(url: String)
        fun history(query: String)
        fun readerState(completed: (Int) -> Unit) { completed(0) }
        fun readerOpen(tab: BrowserTab) = Unit
        fun readerClose(tab: BrowserTab) = Unit
        fun readerAppearance(tab: BrowserTab, close: () -> Unit, more: () -> Unit): View? = null
        fun scriptsAvailable(tab: BrowserTab): Boolean = false
        fun scriptsOpen(tab: BrowserTab) = Unit
    }
    private val overlay = ContentOverlay(activity, parent)
    private val data = PageDataDialogs(activity)
    val isShowing get() = overlay.isShowing
    fun close(): Boolean = overlay.close()

    fun showReader(tab: BrowserTab) { close(); show(tab) }

    fun show(tab: BrowserTab, normal: Boolean = false) {
        if (overlay.close()) return
        actions.readerState { mode -> show(tab, normal, mode) }
    }

    private fun show(tab: BrowserTab, normal: Boolean, readerState: Int) {
        val url = tab.url.removePrefix("view-source:")
        val internal = tab.home || url == HomeDocument.url(activity) ||
            moe.via.browser.feature.network.NetworkLogPages.isPage(activity, url)
        val web = AddressResolver.isWebUrl(url) && !HomeDocument.isHome(activity, url)
        val view = tab.webView
        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val header = LinearLayout(activity).apply { gravity = Gravity.TOP }
        val labels = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), 0, dp(4), 0) }
        labels.addView(NativeTypography.text(activity).apply {
            text = tab.title.ifBlank { Uri.parse(url).host ?: url }
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.page_info_title_size).toFloat())
            setTextColor(activity.palette.text); maxLines = 2; ellipsize = TextUtils.TruncateAt.END
            setPadding(0, px(12), 0, px(2))
            setLineSpacing(dp(2).toFloat(), 1f)
            setOnLongClickListener { data.copy(text.toString()); true }
        }, LinearLayout.LayoutParams(-1, -2))
        if (!internal && url.isNotBlank()) labels.addView(NativeTypography.text(activity).apply {
            text = SpannableString(Uri.decode(url)).apply {
                if (url.startsWith("https://") && view.certificate != null) setSpan(ForegroundColorSpan(0xFF238E45.toInt()), 0, 5, 0)
                val start = indexOf("://") + 3
                val end = indexOf('/', start)
                if (start >= 3 && end > start) setSpan(ForegroundColorSpan(activity.palette.text), start, end, 0)
            }
            textSize = 14f; setTextColor(activity.palette.secondary); maxLines = 2; ellipsize = TextUtils.TruncateAt.END
            setPadding(0, px(2), 0, px(2)); setLineSpacing(dp(2).toFloat(), 1f)
            background = activity.touchFeedback()
            setOnClickListener {
                if (maxLines <= 2) {
                    maxLines = kotlin.math.ceil(text.length / 40.0).toInt().coerceIn(3, 12)
                    textSize = if (maxLines >= 8) 12f else if (maxLines >= 5) 13f else 14f
                } else { maxLines = 2; textSize = 14f }
            }
            setOnLongClickListener { data.copy(url); true }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = px(8) })
        header.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        if (!internal && url.isNotBlank()) {
            header.addView(icon(R.drawable.ref_ic_history_search, "历史") { close(); actions.history(Uri.parse(url).host.orEmpty()) })
            header.addView(icon(R.drawable.ref_ic_qrcode, "二维码") { close(); actions.qrCode(url) })
        }
        body.addView(header, LinearLayout.LayoutParams(-1, -2))
        if (!normal && readerState == 3) {
            actions.readerAppearance(tab, { close() }, { close(); show(tab, true) })?.let {
                body.addView(it); showBody(body); return
            }
        }
        if (web && view.certificate != null) body.addView(row("查看证书") { close(); data.certificate(view) })
        if (web) body.addView(row("查看 Cookies") { close(); data.cookies(url) })
        val configured = sites.forUrl(url)?.enabled == true
        body.addView(row(if (configured) "网站设定 (已启用)" else "网站设定") {
            close(); SiteSettingsNavigation.open(activity, if (internal) null else url)
        })
        if (web && actions.scriptsAvailable(tab)) body.addView(row("查看脚本") { close(); actions.scriptsOpen(tab) })
        if (readerState == 3) body.addView(row("关闭阅读模式") { close(); actions.readerClose(tab) })
        else if (web && readerState in listOf(1, 5)) body.addView(row("打开阅读模式") { close(); actions.readerOpen(tab) })
        showBody(body)
    }
    private fun showBody(body: View) {
        val spec = ChromeLayoutPolicy.resolve(activity)
        val top = spec.topAnchored
        overlay.show(ScrollView(activity).apply { isVerticalScrollBarEnabled = false; addView(body) }, spec.popupWidth(activity), top)
    }
    private fun row(title: String, action: () -> Unit) = NativeTypography.text(activity).apply {
        text = title; setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.page_info_action_size).toFloat())
        setTextColor(activity.palette.accent); gravity = Gravity.CENTER_VERTICAL
        setPadding(px(16), px(12), px(16), px(12)); setSingleLine(true)
        background = activity.touchFeedback(); setOnClickListener { action() }
    }
    private fun icon(resource: Int, title: String, action: () -> Unit) = ImageView(activity).apply {
        setImageResource(resource); imageTintList = ColorStateList.valueOf(activity.palette.icon)
        contentDescription = title; setPadding(dp(13), dp(13), dp(13), dp(13))
        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)); background = activity.touchFeedback()
        setOnClickListener { action() }
    }
    private fun px(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
}
