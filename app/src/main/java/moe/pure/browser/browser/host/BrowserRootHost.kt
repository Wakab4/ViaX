package moe.pure.browser.browser.host

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import moe.pure.browser.R
import moe.pure.browser.ui.BrowserChrome
import moe.pure.browser.ui.theme.ThemedActivity

/** Startup gates and actual root Fragment installation, independent of browser/session business code. */
internal class BrowserRootHost(private val activity: ThemedActivity, private val visibility: (Boolean) -> Unit,
    private val siteSettingsResult: (Int) -> Unit, private val libraryReturned: (Bundle?) -> Unit,
    private val scanReturned: (String) -> Unit) : BrowserRootFragment.Host, AutoCloseable {
    private val manager get() = activity.supportFragmentManager
    override fun siteSettingsChanged(changed: Int) = siteSettingsResult(changed)
    override fun libraryResult(result: Bundle?) = libraryReturned(result)
    override fun scannerResult(result: String) = scanReturned(result)
    private var root: BrowserRootFragment? = null
    private var shell: FrameLayout? = null
    private var browser: FrameLayout? = null
    private var gate: FrameLayout? = null
    private val suspended = linkedSetOf<Fragment>()
    private var active = false
    private var wantBrowser = false
    val isPresented get() = active && root?.isVisible == true && root?.view?.isShown == true
    val fragment get() = checkNotNull(root)
    val inlineManager get() = checkNotNull(root).childFragmentManager
    val inlineContainer get() = checkNotNull(root).inlineContainer()

    fun prepare() {
        if (shell != null) return
        activity.pageStack.presentation(false)
        val chromeHost = FrameLayout(activity).apply { id = R.id.browser_root_container }
        val gateHost = FrameLayout(activity).apply { visibility = View.GONE }
        val outer = FrameLayout(activity).apply {
            addView(chromeHost, FrameLayout.LayoutParams(-1, -1)); addView(gateHost, FrameLayout.LayoutParams(-1, -1))
        }
        browser = chromeHost; gate = gateHost; shell = outer; activity.setContentView(outer)
        val existing = manager.findFragmentByTag(BrowserRootFragment.TAG) as? BrowserRootFragment
        val fragment = existing ?: BrowserRootFragment()
        val transaction = manager.beginTransaction().setReorderingAllowed(true)
        if (existing == null) transaction.add(R.id.browser_root_container, fragment, BrowserRootFragment.TAG)
        manager.fragments.filter { it.isAdded }.forEach { suspended += it; transaction.setMaxLifecycle(it, Lifecycle.State.CREATED) }
        suspended += fragment; transaction.setMaxLifecycle(fragment, Lifecycle.State.CREATED).commitNow()
        root = fragment; fragment.bind(this); activity.pageStack.bindBase(fragment)
    }
    fun showGate(view: View) {
        prepare(); active = false; wantBrowser = false; activity.pageStack.presentation(false)
        gate?.let { target ->
            target.removeAllViews(); (view.parent as? android.view.ViewGroup)?.removeView(view)
            target.addView(view, FrameLayout.LayoutParams(-1, -1)); target.visibility = View.VISIBLE
        }
        if (!manager.isStateSaved) {
            val transaction = manager.beginTransaction().setReorderingAllowed(true)
            manager.fragments.filter { it.isAdded }.forEach { suspended += it; transaction.setMaxLifecycle(it, Lifecycle.State.CREATED) }
            transaction.commitNow()
        }
    }
    fun showBrowser(chrome: BrowserChrome) {
        prepare(); checkNotNull(root).chrome(chrome)
        wantBrowser = true
        activate()
    }
    fun resume() { if (wantBrowser) activate() }
    private fun activate() {
        if (manager.isStateSaved) return
        gate?.apply { removeAllViews(); visibility = View.GONE }; browser?.visibility = View.VISIBLE
        active = true
        if (suspended.isNotEmpty() && !manager.isStateSaved) {
            val transaction = manager.beginTransaction().setReorderingAllowed(true)
            suspended.filter { it.isAdded }.forEach { transaction.setMaxLifecycle(it, Lifecycle.State.RESUMED) }
            suspended.clear(); transaction.commitNow()
        }
        activity.pageStack.presentation(true)
    }
    override fun browserVisibilityChanged(visible: Boolean) {
        root?.view?.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        visibility(active && visible)
    }
    override fun close() { root?.release(this); root = null; suspended.clear(); shell = null; browser = null; gate = null }
}
