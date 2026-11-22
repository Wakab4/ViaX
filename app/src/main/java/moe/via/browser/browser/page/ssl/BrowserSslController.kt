package moe.via.browser.browser.page.ssl

import android.app.Activity
import android.net.http.SslError
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.widget.ScrollView
import android.widget.TextView
import moe.via.browser.feature.pageinfo.CertificateDetails
import moe.via.browser.storage.preferences.BrowserPreferences
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.theme.palette

/** GP e8.h0 warning, process decisions and duplicate request coalescing. */
class BrowserSslController(private val activity: Activity, private val preferences: BrowserPreferences) {
    private data class Key(val host: String, val mask: Int)
    private data class Request(val view: WebView, val handler: SslErrorHandler)
    private class Warning(val requests: MutableList<Request>, var dialog: BrowserDialog? = null)
    private val warnings = linkedMapOf<Key, Warning>()
    private var disposed = false

    fun handle(view: WebView, handler: SslErrorHandler, error: SslError) {
        checkMain()
        if (disposed || activity.isFinishing || activity.isDestroyed) { handler.cancel(); return }
        val mask = SslWarning.mask(error)
        val ignored = preferences.ignoredSslWarnings
        if (ignored != 0 && ignored and mask == mask) { handler.proceed(); return }
        val resourceHost = SslWarning.host(error.url)
        val pageHost = SslWarning.host(view.url)
        val host = resourceHost.ifEmpty { pageHost }
        SslSessionDecisions.get(host)?.let { allowed ->
            if (allowed) handler.proceed() else handler.cancel()
            return
        }
        if (resourceHost.isNotEmpty() && pageHost.isNotEmpty() && resourceHost != pageHost && error.primaryError != SslError.SSL_UNTRUSTED) {
            handler.cancel(); return
        }
        val key = Key(host, mask)
        warnings[key]?.let { it.requests += Request(view, handler); return }
        val pending = Warning(mutableListOf(Request(view, handler)))
        warnings[key] = pending
        val errors = SslWarning.lines(error)
        val message = "与 ${host.ifEmpty { "该网站" }} 的连接不安全：\n$errors\n是否继续？"
        val description: CharSequence = if (error.certificate == null) message else {
            SpannableString(message).apply {
                val start = message.indexOf(errors)
                if (start >= 0 && errors.isNotEmpty()) {
                    setSpan(object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            val parent = widget.parent
                            if (parent is ScrollView) {
                                parent.layoutParams = parent.layoutParams.apply {
                                    height = maxOf(widget.measuredHeight, activity.resources.displayMetrics.heightPixels * 2 / 5)
                                }
                            }
                            if (widget is TextView) widget.text = CertificateDetails.text(error.certificate)
                        }
                    }, start, start + errors.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(ForegroundColorSpan(activity.palette.accent), start, start + errors.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
        val dialog = BrowserDialog(activity, "警告", description)
        val remember = dialog.checkbox("本次浏览过程中不再询问")
        dialog.positive("确定") {
            resolve(key, pending, allowed = true, remember = remember.isChecked)
            true
        }.negative("取消") {
            resolve(key, pending, allowed = false, remember = remember.isChecked)
            true
        }
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener {
            if (warnings[key] === pending) resolve(key, pending, allowed = false, remember = false)
        }
        pending.dialog = dialog
        runCatching { dialog.show() }.onFailure { resolve(key, pending, allowed = false, remember = false) }
    }

    private fun resolve(key: Key, expected: Warning, allowed: Boolean, remember: Boolean) {
        if (warnings[key] !== expected) return
        warnings.remove(key)
        if (remember) SslSessionDecisions.remember(key.host, allowed)
        expected.requests.toList().forEach { request -> runCatching { if (allowed) request.handler.proceed() else request.handler.cancel() } }
        expected.requests.clear()
        expected.dialog?.let { dialog -> dialog.setOnDismissListener(null); dialog.dismiss() }
        expected.dialog = null
    }

    /** Cancel stale handlers when their source document is replaced or released. Remembered choices survive. */
    fun navigation(view: WebView) {
        checkMain()
        warnings.toMap().forEach { (key, warning) ->
            val stale = warning.requests.filter { it.view === view }
            warning.requests.removeAll(stale.toSet())
            stale.forEach { runCatching { it.handler.cancel() } }
            if (warning.requests.isEmpty()) resolve(key, warning, allowed = false, remember = false)
        }
    }

    fun dispose() {
        checkMain()
        if (disposed) return
        disposed = true
        warnings.toMap().forEach { (key, warning) -> resolve(key, warning, allowed = false, remember = false) }
    }

    private fun checkMain() = check(Looper.myLooper() == Looper.getMainLooper()) { "SSL decisions belong to the browser main thread" }
}
