package moe.pure.browser.feature.tools.font

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.view.Gravity
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.site.NativeSiteKeys
import moe.pure.browser.feature.site.SiteFeature
import moe.pure.browser.feature.site.SiteSettingsRepository
import moe.pure.browser.feature.site.ui.common.NativeSiteSeekBar
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.theme.palette
import java.lang.ref.WeakReference
import java.util.Locale

/** s6.bb and its font-return path: preview while dragging, persist on stop, return on user cancel. */
class PageFontDialog(private val activity: Activity, private val settings: BrowserSettings,
    private val sites: SiteSettingsRepository, private val currentView: () -> WebView?,
    private val changed: () -> Unit, private val reopenSite: (Int) -> Unit) : AutoCloseable {
    private val scope = DialogScope()
    private var current: BrowserDialog? = null
    private var disposed = false

    fun show(url: String, fromSite: Boolean = false, returnFlags: Int = 0) {
        if (disposed) return
        current?.dismiss()
        val configured = sites.forUrl(url)?.takeIf { it.enabled }?.values?.get(SiteFeature.FONT_SIZE) ?: 0
        val siteMode = configured > 0 || fromSite
        val initial = if (siteMode && configured > 0) configured else settings.textZoom
        val highlight = if (siteMode) settings.textZoom else 100
        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val label = NativeTypography.text(activity).apply {
            text = String.format(Locale.ROOT, "%d%%", initial); textSize = 16f; gravity = Gravity.CENTER
            setTextColor(activity.palette.text); setPadding(0, dp(16), 0, dp(16))
        }
        body.addView(label, LinearLayout.LayoutParams(-1, -2))
        var preview: WeakReference<WebView>? = null
        val slider = NativeSiteSeekBar(activity).apply {
            max = 30; progress = (initial - 50) / 5; highlightProgress = (highlight - 50) / 5; minimumHeight = dp(2)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val value = progress * 5 + 50
                    label.text = String.format(Locale.ROOT, "%d%%", value)
                    preview?.get()?.settings?.textZoom = value
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    preview = currentView()?.takeUnless { it.url?.startsWith("file://", true) == true }?.let(::WeakReference)
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val value = seekBar.progress * 5 + 50
                    if (siteMode) {
                        val draft = sites.editDraft(NativeSiteKeys.fromUrl(url))
                        draft.config = draft.config.withEnabled(true).withFeature(SiteFeature.FONT_SIZE,
                            value.takeIf { it != settings.textZoom })
                        sites.commit(draft)
                    } else settings.textZoom = value
                    changed()
                }
            })
        }
        body.addView(slider, LinearLayout.LayoutParams(-1, -2).apply {
            marginStart = activity.dp(16); marginEnd = activity.dp(16); bottomMargin = activity.dp(16)
        })
        if (siteMode) body.addView(NativeTypography.text(activity).apply {
            setText(R.string.ref_page_font_site_hint); gravity = Gravity.CENTER; textSize = 12f
            setTextColor(activity.palette.secondary); setPadding(0, dp(2), 0, dp(8))
        }, LinearLayout.LayoutParams(-1, -2))
        val dialog = BrowserDialog(activity).custom(body)
        dialog.setOnCancelListener { if (!disposed && fromSite) reopenSite(returnFlags or 4) }
        current = dialog
        scope.show(dialog) { if (current === dialog) current = null }
    }

    override fun close() { disposed = true; scope.dispose(); current = null }
}
