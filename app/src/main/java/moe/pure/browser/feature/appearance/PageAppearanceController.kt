package moe.pure.browser.feature.appearance

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.os.Looper
import android.webkit.WebView
import moe.pure.browser.core.BrowserInternalPages
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.model.BrowserTab
import moe.pure.browser.ui.theme.palette
import moe.pure.browser.web.selection.BrowserPageView
import java.lang.ref.WeakReference

/** e8.k0: raw per-view COLOR and authority cache are separate from the animated window surface. */
class PageAppearanceController(private val activity: Activity, private val preferences: BrowserPreferences,
    private val apply: (tab: BrowserTab, color: Int?, foreground: Int?) -> Unit) {
    private data class Target(val tabId: String, val pageId: Int, val color: Int?)
    private var selected: WeakReference<BrowserTab>? = null
    private var disposed = false
    private var animator: ValueAnimator? = null
    private var target: Target? = null
    private var lastColor: Int? = null

    fun selected(tab: BrowserTab) {
        checkMain()
        if (disposed) return
        selected = WeakReference(tab)
        val view = tab.liveView ?: return
        // Native D reads the shared URL cache, even when the view restored a different raw COLOR.
        publish(tab, view, if (internal(tab.url)) 0 else PageColorCache.color(tab.url) ?: activity.palette.canvas, true)
    }

    fun navigation(tab: BrowserTab, source: WebView, url: String) {
        checkMain()
        if (disposed) return
        val color = if (internal(url)) {
            rawColor(source, 0)
            0
        } else {
            val cached = PageColorCache.color(url) ?: 0
            if (cached != 0) rawColor(source, cached)
            // A cache miss changes the UI fallback, not the view's already-restored COLOR.
            if (cached == 0) activity.palette.canvas else cached
        }
        if (source.isShown) publish(tab, source, color)
    }

    fun finished(tab: BrowserTab, source: WebView, url: String) {
        checkMain()
        if (!disposed && !internal(url)) sampleWhenNeeded(tab, source, url)
    }

    fun progress(tab: BrowserTab, source: WebView, progress: Int) {
        checkMain()
        val url = tab.sourceUrl(source).orEmpty()
        if (!disposed && progress > 70 && progress <= 100 && !internal(url) && (PageColorCache.color(url) ?: 0) == 0)
            sampleWhenNeeded(tab, source, url)
    }

    fun refresh(tab: BrowserTab) {
        checkMain()
        if (disposed) return
        val view = tab.liveView ?: return
        publish(tab, view, if (internal(tab.url)) 0 else PageColorCache.color(tab.url) ?: activity.palette.canvas, true)
    }

    /** s6.sb resets ordinary window chrome to the new theme while preserving raw/cache page colors. */
    fun themeChanged(tab: BrowserTab) {
        checkMain()
        if (disposed) return
        val view = tab.liveView ?: return
        publish(tab, view, if (internal(tab.url)) 0 else activity.palette.canvas, true)
    }

    /** M7 updates the raw current-view color and URL cache before applying window color policy. */
    fun readerStyled(tab: BrowserTab, source: WebView, color: Int) {
        checkMain()
        if (disposed) return
        rawColor(source, color)
        PageColorCache.put(source.url ?: tab.url, color)
        publish(tab, source, color)
    }

    /** T2 clears the current authority cache and restores the view accent before window policy. */
    fun readerExited(tab: BrowserTab, source: WebView) {
        checkMain()
        if (disposed) return
        PageColorCache.invalidate(source.url ?: tab.url)
        rawColor(source, -1)
        publish(tab, source, -1)
    }

    private fun sampleWhenNeeded(tab: BrowserTab, source: WebView, capturedUrl: String) {
        if (capturedUrl.isEmpty() || moe.pure.browser.ui.theme.BrowserNightMode.global(activity) || PageColorCache.contains(capturedUrl)) return
        if (capturedUrl.length > 12 && capturedUrl.take(12).equals("view-source:", true)) {
            rawColor(source, activity.palette.canvas)
            if (source.isShown) publish(tab, source, activity.palette.canvas)
            return
        }
        PageColorCache.put(capturedUrl, 0)
        val reference = WeakReference(source)
        source.postDelayed({
            val view = reference.get() ?: return@postDelayed
            // Native keeps this captured URL and accepts a still-live view after another navigation.
            val sampled = PageColorSampler.sample(view).takeIf { it != 0 } ?: activity.palette.canvas
            PageColorCache.put(capturedUrl, sampled)
            rawColor(view, sampled)
            if (!disposed && view.isShown) publish(tab, view, sampled)
        }, SAMPLE_DELAY_MS)
    }

    private fun rawColor(source: WebView, color: Int) { (source as? BrowserPageView)?.pageAccentColor = color }
    private fun internal(url: String) = BrowserInternalPages.isInternal(activity, url)

    private fun publish(tab: BrowserTab, source: WebView, raw: Int, force: Boolean = false) {
        if (disposed || selected?.get() !== tab) return
        val color = raw.takeIf { it != 0 && preferences.colorMode && !moe.pure.browser.ui.theme.BrowserNightMode.global(activity) }
        val next = Target(tab.id, (source as? BrowserPageView)?.pageId ?: source.id, color)
        if (next == target) {
            if (force) {
                animator?.cancel(); animator = null; lastColor = color
                apply(tab, color, color?.let(PageAppearanceColors::icon))
            }
            return
        }
        target = next
        animator?.cancel(); animator = null
        val previous = lastColor
        if (color == null || previous == null || previous == color) {
            lastColor = color
            apply(tab, color, color?.let(PageAppearanceColors::icon))
            return
        }
        val foreground = PageAppearanceColors.icon(color)
        animator = ValueAnimator.ofObject(ArgbEvaluator(), previous, color).apply {
            duration = COLOR_ANIMATION_MS
            addUpdateListener { value ->
                if (!disposed && target == next) {
                    lastColor = value.animatedValue as Int
                    apply(tab, lastColor, foreground)
                }
            }
            start()
        }
    }

    fun released(tab: BrowserTab) {
        checkMain()
        if (selected?.get() === tab) { animator?.cancel(); animator = null; selected?.clear(); target = null }
    }
    fun dispose() {
        checkMain()
        disposed = true; animator?.cancel(); animator = null
        selected?.clear(); selected = null; target = null; lastColor = null
    }
    private fun checkMain() = check(Looper.myLooper() == Looper.getMainLooper()) { "Page appearance belongs to the browser main thread" }
    companion object {
        const val SAMPLE_DELAY_MS = 200L
        const val COLOR_ANIMATION_MS = 240L
    }
}
