package moe.pure.browser.browser.host

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import moe.pure.browser.R
import moe.pure.browser.ui.BrowserChrome
import java.lang.ref.WeakReference

/** The real browser root owns Chrome's view and the native child-panel container. */
class BrowserRootFragment : Fragment() {
    internal interface Host {
        fun browserVisibilityChanged(visible: Boolean)
        fun siteSettingsChanged(changed: Int)
        fun libraryResult(result: Bundle?)
        fun scannerResult(result: String)
    }
    private var host: WeakReference<Host>? = null
    private var chrome: BrowserChrome? = null
    private var holder: FrameLayout? = null
    private var panels: FrameLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); inlineContainer() }
    internal fun inlineContainer(): FrameLayout = panels ?: FrameLayout(requireContext()).apply {
        id = R.id.browser_inline_panels; layoutParams = FrameLayout.LayoutParams(-1, -1)
    }.also { panels = it }
    internal fun bind(host: Host) { this.host = WeakReference(host) }
    internal fun siteSettingsChanged(changed: Int) { host?.get()?.siteSettingsChanged(changed) }
    internal fun libraryResult(result: Bundle?) { host?.get()?.libraryResult(result) }
    internal fun scannerResult(result: String) { host?.get()?.scannerResult(result) }
    internal fun release(host: Host) { if (this.host?.get() === host) this.host = null }
    internal fun chrome(value: BrowserChrome) { chrome = value; attachChrome() }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FrameLayout(requireContext()).also { holder = it; attachChrome() }
    private fun attachChrome() {
        val target = holder ?: return
        val content = chrome?.root ?: inlineContainer()
        if (content.parent !== target) {
            target.removeAllViews(); (content.parent as? ViewGroup)?.removeView(content)
            target.addView(content, FrameLayout.LayoutParams(-1, -1))
        }
        target.visibility = if (chrome == null) View.INVISIBLE else View.VISIBLE
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { host?.get()?.browserVisibilityChanged(isVisible) }
    override fun onHiddenChanged(hidden: Boolean) { super.onHiddenChanged(hidden); host?.get()?.browserVisibilityChanged(!hidden) }
    override fun onResume() { super.onResume(); host?.get()?.browserVisibilityChanged(isVisible) }
    override fun onPause() { view?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS; super.onPause() }
    override fun onDestroyView() { chrome?.dispose(); chrome = null; holder = null; super.onDestroyView() }
    override fun onDestroy() { host = null; panels = null; super.onDestroy() }
    companion object {
        const val TAG = "native_browser_root"
        internal fun find(activity: androidx.fragment.app.FragmentActivity) = activity.supportFragmentManager.findFragmentByTag(TAG) as? BrowserRootFragment
    }
}
