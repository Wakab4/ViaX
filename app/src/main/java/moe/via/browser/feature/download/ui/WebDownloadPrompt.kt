package moe.via.browser.feature.download.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.app.Dialog
import android.graphics.Typeface
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.TextView
import moe.via.browser.ui.message.BrowserMessages
import android.webkit.CookieManager
import moe.via.browser.R
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.dialog.DialogLayoutOwner
import moe.via.browser.ui.theme.palette
import moe.via.browser.feature.download.DownloadClipboard
import moe.via.browser.feature.download.DownloadNames
import moe.via.browser.feature.download.data.DownloadPayloads
import moe.via.browser.feature.download.http.DownloadHttp
import moe.via.browser.feature.download.model.DownloadDraft
import moe.via.browser.feature.download.transfer.DownloadControl

/** The reference's web-download confirmation is separate from its generic edit dialogs. */
class WebDownloadPrompt(private val activity: Activity, draft: DownloadDraft,
    private val approved: (DownloadDraft) -> Unit) : Dialog(activity, R.style.Theme_ViaBrowser_Dialog), DialogLayoutOwner {
    private val control = DownloadControl()
    private var request = draft.copy(name = draft.name ?: DownloadNames.guess(draft))
    private val legacy = ContextThemeWrapper(activity, if (activity.palette.night) android.R.style.Theme_Black else android.R.style.Theme_Light)
    private val body = LayoutInflater.from(legacy).inflate(R.layout.web_download_prompt, null).also { NativeTypography.tree(it) }
    private val filename = body.findViewById<EditText>(R.id.web_download_name)
    private val size = body.findViewById<TextView>(R.id.web_download_size)
    private val copy = body.findViewById<TextView>(R.id.web_download_copy)
    var approvedDownload = false; private set
    var cancelledByUser = false; private set
    fun snapshot() = request.copy(name = filename.text.toString())
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE); setContentView(body); setCanceledOnTouchOutside(false)
        body.findViewById<TextView>(R.id.web_download_title).apply { setTextColor(activity.palette.text); NativeTypography.style(this, android.graphics.Typeface.BOLD) }
        filename.apply {
            setTextColor(activity.palette.text); setHintTextColor(activity.palette.secondary); setText(request.name)
            setSelectAllOnFocus(true)
            setOnFocusChangeListener { _, focused -> if (focused) {
                val text = text.toString(); val dot = text.lastIndexOf('.')
                if (dot < 0 || text.length - dot >= 7) selectAll() else setSelection(0, dot)
            } }
        }
        size.setTextColor(activity.palette.secondary)
        copy.setOnClickListener {
            if (request.payload == null) {
                copied(request.url)
            } else {
                copy.isEnabled = false
                BrowserExecutors.network.execute {
                    val result = runCatching { DownloadPayloads.get(activity.applicationContext).dataUrl(request.payload!!) }
                    activity.runOnUiThread { if (isShowing && !activity.isDestroyed) {
                        result.onSuccess(::copied)
                        result.onFailure { copy.isEnabled = true }
                    } }
                }
            }
        }
        body.findViewById<TextView>(R.id.web_download_cancel).setOnClickListener { cancel() }
        body.findViewById<TextView>(R.id.web_download_accept).setOnClickListener {
            if (approvedDownload) return@setOnClickListener
            val name = filename.text.toString().trim()
            val mime = if ('.' in name) DownloadNames.mime(name) else request.mime ?: "application/octet-stream"
            approvedDownload = true; approved(request.copy(name = name, mime = mime)); dismiss()
        }
        refresh()
    }
    private fun copied(value: String) {
        DownloadClipboard.put(activity, value).onSuccess { cancelledByUser = true; dismiss() }.onFailure {
            copy.isEnabled = true; BrowserMessages.show(activity, "无法复制链接")
        }
    }
    override fun show() {
        updateGeometry(); super.show(); updateGeometry()
        if (request.length <= 0 && request.url.startsWith("http", true)) probe()
    }
    private fun probe() {
        val headers = buildMap {
            CookieManager.getInstance().getCookie(request.url)?.let { put("Cookie", it) }
            put("Referer", request.url)
            request.userAgent?.let { put("User-Agent", it) }
            request.authorization?.let { put("Authorization", it) }
        }
        BrowserExecutors.network.execute {
            val result = runCatching { DownloadHttp(control).head(request.url, headers) }.getOrNull() ?: return@execute
            activity.runOnUiThread {
                if (!isShowing || activity.isDestroyed) return@runOnUiThread
                request = request.copy(length = result.length, mime = result.mime ?: request.mime, disposition = result.disposition ?: request.disposition)
                if (request.overwriteName) {
                    request = request.copy(name = DownloadNames.guess(request))
                    if (request.name?.endsWith(".apk", true) == true) request = request.copy(mime = "application/vnd.android.package-archive")
                    filename.setText(request.name)
                }
                refresh()
            }
        }
    }
    private fun refresh() {
        size.text = "文件大小：${DownloadNames.size(request.length)}"
        copy.visibility = if (request.length > if (request.payload != null || request.url.startsWith("data:")) 1019904 else 64198568) View.GONE else View.VISIBLE
    }
    override fun updateGeometry() {
        val metrics = activity.resources.displayMetrics
        window?.apply { setLayout(minOf(activity.dp(384), minOf(metrics.widthPixels, metrics.heightPixels) - activity.dp(72)), -2); setGravity(Gravity.CENTER); setDimAmount(.4f) }
    }
    override fun dismiss() { control.cancel(); super.dismiss() }
    override fun cancel() { cancelledByUser = true; super.cancel() }
}
