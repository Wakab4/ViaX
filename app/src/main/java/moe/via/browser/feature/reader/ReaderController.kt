package moe.via.browser.feature.reader

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.core.config.NativeRemoteFeatureValues
import moe.via.browser.feature.reader.ui.ReaderColorPicker
import moe.via.browser.feature.reader.ui.ReaderFontSizeControl
import moe.via.browser.feature.reader.ui.ReaderSettingsEntry
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.palette

/** Reader actions resolve the current page at dispatch and again for native result effects. */
class ReaderController(private val activity: ThemedActivity, private val current: () -> BrowserTab?,
    private val styled: (WebView, Int) -> Unit, private val restored: (WebView) -> Unit,
    private val openCssEditor: () -> Unit, private val changed: () -> Unit) {
    private val preferences = ReaderPreferences(activity)
    private val protocol = ReaderPageProtocol(activity)
    private val main = Handler(Looper.getMainLooper())
    private var disposed = false
    private var hint = false
    private var hintExpiration: Runnable? = null
    private val pageEffects = ReaderPageEffects({ current()?.liveView }, ::query) { appearance() }

    fun hintVisible() = hint
    fun navigation(tab: BrowserTab) { if (tab === current()) hideHint() }

    /** S7/c4 reads a visible current page; its effects target current when the callback arrives. */
    fun probeCurrent() = probe { next ->
        if (next == 1) showHint() else if (next == 5) enter()
    }
    fun probe(completed: (Int) -> Unit) {
        val view = current()?.liveView?.takeIf { it.isShown } ?: return
        query(view, completed)
    }
    private fun query(view: WebView, completed: (Int) -> Unit) {
        if (disposed) return
        protocol.probe(ReaderPage(view)) { if (!disposed) completed(it) }
    }
    private fun showHint() {
        hintExpiration?.let(main::removeCallbacks)
        hint = true
        hintExpiration = Runnable { hideHint() }
        main.postDelayed(hintExpiration!!, 3000)
        changed()
    }
    private fun hideHint() {
        if (!hint) return
        hint = false
        hintExpiration?.let(main::removeCallbacks)
        hintExpiration = null
        changed()
    }

    fun addressClick(showInformation: () -> Unit) {
        if (hint && !preferences.confirmEntry) enter() else showInformation()
    }
    fun toggle() = probe { if (it == 3) exit() else enter() }
    fun settings() = ReaderSettingsEntry.open(activity)

    fun enter() {
        val view = current()?.liveView ?: return
        val page = ReaderPage(view)
        if (NativeRemoteFeatureValues.sites.reader(page.url)) { message(R.string.ref_reader_unsupported_site); return }
        protocol.enter(page) entered@{ result ->
            if (disposed) return@entered
            when (result) {
                0 -> { appearance(); message(R.string.ref_reader_opened); hideHint() }
                1 -> message(R.string.ref_reader_requires_javascript)
                3 -> message(R.string.ref_reader_page_incomplete)
                4 -> message(R.string.ref_reader_requires_http)
            }
        }
    }
    fun exit() {
        val view = current()?.liveView ?: return
        protocol.exit(ReaderPage(view)) exited@{ result ->
            if (disposed) return@exited
            if (result == 0) { current()?.liveView?.let(restored); message(R.string.ref_reader_closed) }
            else if (result == 1) message(R.string.ref_reader_requires_javascript)
        }
    }
    fun appearance() {
        val view = current()?.liveView ?: return
        if (disposed) return
        val appearance = ReaderAppearance.from(preferences)
        ReaderPage(view).evaluate(appearance.script())
        styled(view, appearance.color)
    }
    fun consumePageEffects() = pageEffects.consume()

    fun panel(close: () -> Unit, more: () -> Unit): View = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        addView(ReaderColorPicker(activity, preferences.themeColor) { preferences.themeColor = it; appearance() }, LinearLayout.LayoutParams(-1, -2))
        addView(ReaderFontSizeControl(activity, preferences.textSize) { preferences.textSize = it; appearance() }, LinearLayout.LayoutParams(-1, -2))
        addView(row("关闭阅读模式") { close(); exit() })
        addView(row("更多选项", { close(); openCssEditor() }) { more() })
    }
    private fun row(text: String, longClick: (() -> Unit)? = null, click: () -> Unit) = NativeTypography.text(activity).apply {
        this.text = text
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.page_info_action_size).toFloat())
        setTextColor(activity.palette.accent); gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(12), dp(16), dp(12)); setSingleLine(true)
        background = activity.touchFeedback(); setOnClickListener { click() }
        if (longClick != null) setOnLongClickListener { longClick(); true }
    }
    private fun message(resource: Int) {
        if (!activity.isDestroyed && !activity.isFinishing) BrowserMessages.show(activity, activity.getString(resource))
    }
    fun dispose() { disposed = true; pageEffects.close(); main.removeCallbacksAndMessages(null) }
}
