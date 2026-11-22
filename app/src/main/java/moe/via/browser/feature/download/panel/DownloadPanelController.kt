package moe.via.browser.feature.download.panel

import android.content.SharedPreferences
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import moe.via.browser.R
import moe.via.browser.browser.ui.host.BrowserRootFragment
import moe.via.browser.storage.preferences.BrowserPreferences
import moe.via.browser.browser.ui.chrome.BrowserChrome
import moe.via.browser.ui.theme.window.ChromeLayoutPolicy
import moe.via.browser.ui.theme.ThemedActivity

/** f8.c/z belongs to the browser root's child manager and Chrome's inline container. */
internal class DownloadPanelController(private val activity: ThemedActivity, private val chrome: () -> BrowserChrome?,
    private val manager: FragmentManager) : DownloadPanelFragment.Host, AutoCloseable {
    private val preferences = activity.getSharedPreferences("browser", 0)
    private var disposed = false
    private var visuals = true
    private var current: DownloadPanelFragment? = null
    private var shade: View? = null
    private var observed: View? = null
    private var activeChrome: BrowserChrome? = null
    private var effects: DownloadPanelEffects? = null
    private var geometry: DownloadPanelGeometry? = null
    private val layout = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> position() }
    private val changed = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in setOf("labflag", "nightmode2")) activity.runOnUiThread { rebind() }
    }
    private val stack = FragmentManager.OnBackStackChangedListener { if (!disposed) rebind() }
    private val callbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(fm: FragmentManager, fragment: Fragment, view: View, state: android.os.Bundle?) {
            if (fragment is DownloadPanelFragment) rebind()
        }
    }
    val eligible get() = ChromeLayoutPolicy.resolve(activity).tablet
    init {
        manager.registerFragmentLifecycleCallbacks(callbacks, false); manager.addOnBackStackChangedListener(stack)
        preferences.registerOnSharedPreferenceChangeListener(changed)
    }
    private fun entries() = manager.fragments.filterIsInstance<DownloadPanelFragment>().filter { it.isAdded && !it.isRemoving }
    private fun top() = entries().lastOrNull()
    fun show() {
        if (disposed || activity.isFinishing || activity.isDestroyed) return
        if (manager.isStateSaved || manager.isDestroyed || chrome() == null) return
        visuals = true
        val host = checkNotNull(chrome()); attachEffects(host); showShade()
        val value = DownloadPanelGeometry.resolve(activity, host)
        manager.beginTransaction().setReorderingAllowed(true)
            .add(R.id.browser_inline_panels, DownloadPanelFragment.create(value), DownloadPanelFragment.TAG)
            .addToBackStack(null).commit()
    }
    fun rebind() {
        if (disposed) return
        val panels = entries()
        if (panels.isEmpty()) return
        val host = chrome() ?: return
        current = panels.last(); geometry = DownloadPanelGeometry.resolve(activity, host)
        panels.filter { it.view != null }.forEach { it.bind(checkNotNull(geometry), this) }
        if (visuals) { attachEffects(host); showShade() }
        observe(host.root); position()
    }
    private fun attachEffects(host: BrowserChrome) {
        if (activeChrome !== host) { effects?.close(); effects = DownloadPanelEffects(host); activeChrome = host }
        effects?.update(DownloadPanelGeometry.resolve(activity, host), blur(), entries().size <= 1)
    }
    private fun blur() = Build.VERSION.SDK_INT >= 31 && BrowserPreferences(preferences).blurEnabled
    private fun showShade() {
        val layer = checkNotNull(chrome()).panelBackdrop
        shade?.let { existing ->
            if (existing.parent !== layer) {
                (existing.parent as? android.view.ViewGroup)?.removeView(existing); layer.addView(existing, 0, FrameLayout.LayoutParams(-1, -1))
            }
            existing.setBackgroundColor(if (blur()) 0 else 0x70808080)
            if (existing.visibility != View.VISIBLE) {
                existing.animate().alpha(if (blur()) .5f else 1f).setDuration(220)
                    .withStartAction { existing.visibility = View.VISIBLE }.withEndAction(null).start()
            }
            return
        }
        shade = View(activity).apply {
            setBackgroundColor(if (blur()) 0 else 0x70808080)
            contentDescription = activity.getString(R.string.ref_download_panel_hide)
            isClickable = true; setOnClickListener { dismiss() }; visibility = View.GONE; alpha = 0f
            layer.addView(this, 0, FrameLayout.LayoutParams(-1, -1))
            animate().alpha(if (blur()) .5f else 1f).setDuration(220).withStartAction { visibility = View.VISIBLE }.start()
        }
    }
    private fun observe(view: View) {
        if (observed === view) return
        unobserve(); observed = view
        view.addOnLayoutChangeListener(layout)
    }
    private fun unobserve() {
        observed?.removeOnLayoutChangeListener(layout)
        observed = null
    }
    private fun position() {
        if (disposed) return
        val host = chrome() ?: return
        val visible = host.content.isShown && host.content.width > 0 && host.content.height > 0
        entries().forEach { it.show(visible) }
        if (!visible) return
        val value = DownloadPanelGeometry.resolve(activity, host)
        if (geometry != value) {
            geometry = value; entries().forEach { it.bind(value, this) }
            if (visuals) effects?.update(value, blur(), entries().size <= 1)
        }
    }
    fun hostVisibilityChanged(visible: Boolean) {
        entries().forEach { it.show(visible) }
        if (visible) rebind()
    }
    fun back(): Boolean = top()?.let { it.recordPageStack?.handleBack() == true || dismiss() } ?: false
    override fun dismiss(): Boolean {
        if (disposed) return false
        top() ?: return false
        if (manager.isStateSaved || activity.hasRecordPage) return true
        visuals = false; effects?.close(); effects = null; activeChrome = null
        manager.popBackStack()
        shade?.takeIf { it.visibility != View.GONE }?.let { view ->
            view.animate().alpha(0f).setDuration(220).withStartAction(null).withEndAction { view.visibility = View.GONE }.start()
        }
        return true
    }
    private fun removeShade(view: View) {
        (view.parent as? android.view.ViewGroup)?.removeView(view)
        if (shade !== view) return
        shade = null
    }
    override fun destroyed(fragment: DownloadPanelFragment) {
        if (current !== fragment) return
        current = entries().lastOrNull { it !== fragment && it.view != null }
        if (current == null) { unobserve(); if (visuals) releaseVisuals() }
    }
    private fun releaseVisuals() {
        unobserve(); effects?.close(); effects = null; activeChrome = null; geometry = null
        shade?.let { it.animate().cancel(); removeShade(it) }
    }
    override fun close() {
        if (disposed) return
        disposed = true
        manager.unregisterFragmentLifecycleCallbacks(callbacks); manager.removeOnBackStackChangedListener(stack)
        preferences.unregisterOnSharedPreferenceChangeListener(changed); current = null; releaseVisuals()
    }
    companion object {
        fun existing(activity: ThemedActivity): DownloadPanelFragment? {
            return BrowserRootFragment.find(activity)?.childFragmentManager?.fragments?.filterIsInstance<DownloadPanelFragment>()
                ?.lastOrNull { it.isAdded && !it.isRemoving }
        }
    }
}
