package moe.pure.browser.feature.home

import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import moe.pure.browser.core.graphics.NativeDisplaySize
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.core.BrowserInternalPages
import moe.pure.browser.feature.home.background.HomeBackgroundLoader
import moe.pure.browser.feature.home.data.HomePreferences
import moe.pure.browser.model.BrowserTab
import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.ui.tasks.NativeViewJobs

/** Invalidates resident home documents after committed settings or favorite changes. */
class HomeController(private val activity: ThemedActivity, private val actions: Actions) {
    interface Actions {
        fun current(): BrowserTab?
        fun documentsChanged()
        fun appearance(background: Drawable?, foreground: Int?)
    }
    private val preferences = HomePreferences(activity)
    private val backgrounds = HomeBackgroundLoader(activity)
    private val main = Handler(Looper.getMainLooper())
    private val jobs = NativeViewJobs()
    private var backgroundStamp = -1L
    private var currentBackground: Drawable? = null
    private var disposed = false
    private val changed = Runnable { actions.documentsChanged(); refresh() }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in HOME_FIELDS) { main.removeCallbacks(changed); main.post(changed) }
    }
    init { preferences.values.registerOnSharedPreferenceChangeListener(listener) }

    fun selected(tab: BrowserTab) {
        appearance(tab)
    }
    fun navigation(tab: BrowserTab) {
        if (tab === actions.current()) appearance(tab)
    }
    fun refresh() {
        if (disposed) return
        actions.current()?.let { appearance(it) }
    }
    fun rebind() { backgroundStamp = -1L; actions.current()?.let { appearance(it) } }

    private fun appearance(tab: BrowserTab) {
        if (disposed) return
        if (!hasBackground(tab)) {
            actions.appearance(null, null); return
        }
        val config = preferences.config()
        val display = NativeDisplaySize.real(activity)
        val request = backgrounds.capture(config, display.x, display.y)
        if (request == null) {
            currentBackground = null
            applyBackground()
            return
        }
        val stamp = request.cache.lastModified() + preferences.backgroundDesign.flag.toLong()
        if (request.cached && stamp == backgroundStamp) { applyBackground(); return }
        backgroundStamp = stamp
        jobs.launch({ backgrounds.image(request) }, failed = { NativeLog.error(null, it) }) { drawable -> currentBackground = drawable; applyBackground() }
        backgrounds.prepare(request)
    }

    private fun applyBackground() {
        val tab = actions.current() ?: return
        if (!hasBackground(tab)) return
        val config = preferences.config()
        actions.appearance(backgrounds.surface(config, currentBackground),
            if (config.isNight || !config.isLight) 0xDEFFFFFF.toInt() else 0xDE000000.toInt())
    }
    private fun hasBackground(tab: BrowserTab) = tab.home || BrowserInternalPages.kind(activity, tab.liveView?.url ?: tab.url) > 0
    fun dispose() {
        disposed = true
        jobs.close()
        preferences.values.unregisterOnSharedPreferenceChangeListener(listener)
        main.removeCallbacksAndMessages(null)
    }
    private companion object {
        val HOME_FIELDS = setOf("bghome", "bginfo", "urlbarcolor", "nightmode2", "taghome", "logochioce", "logoinfo2",
            "searchinfo", "custominfo", "favinfo", "csstheme", "home")
    }
}
