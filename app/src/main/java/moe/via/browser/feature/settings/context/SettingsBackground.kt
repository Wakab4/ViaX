package moe.via.browser.feature.settings.context

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import moe.via.browser.core.graphics.NativeDisplaySize
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.home.background.HomeBackgroundLoader
import moe.via.browser.feature.home.data.HomePreferences
import moe.via.browser.ui.tasks.NativeViewJobs
import java.util.IdentityHashMap

/** Settings uses the same single background drawable and mask as the browser homepage. */
internal class SettingsBackground(private val activity: Activity, private val content: ViewGroup,
    enabled: Boolean) : AutoCloseable, View.OnLayoutChangeListener {
    private val original = content.background
    private val children = IdentityHashMap<View, Drawable?>()
    private val active = Build.VERSION.SDK_INT >= 29 && enabled
    private var identity = ""
    private val jobs = NativeViewJobs()
    private val backgrounds = HomeBackgroundLoader(activity)
    private var closed = false
    private var applied = false

    init { if (active) { content.addOnLayoutChangeListener(this); content.post { refresh() } } }

    fun refresh() {
        val root = content
        if (!active || closed || root.width <= 0 || root.height <= 0) return
        val preferences = HomePreferences(activity)
        val config = preferences.config()
        if (config.backgroundPath.isNullOrEmpty()) {
            identity = ""; restore(); return
        }
        val display = NativeDisplaySize.real(activity)
        val snapshot = backgrounds.capture(config, display.x, display.y)
        if (snapshot == null) { identity = ""; restore(); return }
        val next = "${snapshot.cache.lastModified()}:${preferences.backgroundDesign.flag}:${config.isNight}:${config.accentColor}:${config.backgroundPath}"
        if (identity == next) return
        identity = next
        jobs.launch({ backgrounds.image(snapshot) }, failed = { NativeLog.error(null, it) }) { image ->
            root.background = backgrounds.surface(preferences.config(), image)
            for (index in 0 until root.childCount) root.getChildAt(index).let { child ->
                if (!children.containsKey(child)) children[child] = child.background
                child.background = null
            }
            applied = true
        }
        backgrounds.prepare(snapshot)
    }
    override fun onLayoutChange(view: View, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
        if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) refresh()
    }
    private fun restore() {
        if (!applied) return
        content.background = original
        children.forEach { (view, background) -> view.background = background }
        children.clear(); applied = false
    }
    override fun close() { if (!closed) { closed = true; jobs.close(); content.removeOnLayoutChangeListener(this); restore() } }
}
