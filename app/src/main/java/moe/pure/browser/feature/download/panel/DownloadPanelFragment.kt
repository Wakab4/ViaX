package moe.pure.browser.feature.download.panel

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import moe.pure.browser.R
import moe.pure.browser.feature.download.ui.DownloadManagerFragment
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.pages.PageStackController
import moe.pure.browser.ui.pages.RecordPageStackOwner
import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.ui.theme.palette
import java.lang.ref.WeakReference

/** f8.z/c is an inline Fragment with its own nested page stack, not a platform dialog. */
internal class DownloadPanelFragment : Fragment(), RecordPageStackOwner {
    interface Host { fun dismiss(): Boolean; fun destroyed(fragment: DownloadPanelFragment) }
    private var host: WeakReference<Host>? = null
    private var outer: FrameLayout? = null
    private var surface: FrameLayout? = null
    private var bound = false
    private var announced = false
    private var focused = false
    private var geometry = DownloadPanelGeometry(1, android.view.Gravity.BOTTOM or android.view.Gravity.END)
    override var recordPageStack: PageStackController? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { geometry = DownloadPanelGeometry(it.getInt("width", 1), it.getInt("gravity", geometry.gravity), it.getInt("height", -1)) }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val body = FrameLayout(activity).apply { isClickable = true; isFocusable = false }
        surface = body
        val root = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1); visibility = View.INVISIBLE
            addView(body, FrameLayout.LayoutParams(geometry.width, geometry.height, geometry.gravity))
        }
        outer = root; style()
        recordPageStack = PageStackController(activity, childFragmentManager, target = { surface },
            containerId = R.id.download_panel_pages, embedded = true,
            presented = { bound && isAdded && !isRemoving && outer?.isShown == true }, rootBack = { host?.get()?.dismiss() })
            .also { it.attach() }
        return root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (childFragmentManager.fragments.none { it is DownloadManagerFragment })
            recordPageStack?.push(DownloadManagerFragment::class.java, Bundle().apply { putBoolean("draggable", false) }, root = true)
    }
    fun bind(value: DownloadPanelGeometry, owner: Host) {
        host = WeakReference(owner); bound = true
        val changed = geometry.gravity != value.gravity
        geometry = value
        arguments?.apply { putInt("width", value.width); putInt("height", value.height); putInt("gravity", value.gravity) }
        val body = surface ?: return
        val apply = Runnable {
            body.layoutParams = FrameLayout.LayoutParams(geometry.width, geometry.height, geometry.gravity)
            style()
            if (changed) body.animate().alpha(1f).setDuration(120).start()
        }
        body.animate().cancel()
        if (changed) body.animate().alpha(0f).setDuration(120).withEndAction { apply.run() }.start() else apply.run()
        show(true)
        body.accessibilityPaneTitle = getString(R.string.ref_download_ui_title)
    }
    fun show(visible: Boolean) {
        outer?.visibility = if (visible && bound) View.VISIBLE else View.INVISIBLE
        if (!visible) announced = false
        if (visible && !announced && recordPageStack?.isPresented == true) {
            announced = true; recordPageStack?.hostPresented()
            if (!focused) {
                focused = true
                surface?.post { surface?.takeIf { isAdded && it.isAttachedToWindow && it.isShown }?.let { it.requestFocus(); it.performAccessibilityAction(64, null) } }
            }
        }
    }
    private fun style() {
        val context = context ?: return
        val radius = context.dp(18).toFloat()
        surface?.background = GradientDrawable().apply {
            setColor(if (context.palette.night) 0xcc000000.toInt() else 0xebffffff.toInt())
            cornerRadii = if (geometry.top) floatArrayOf(0f, 0f, 0f, 0f, radius, radius, radius, radius)
                else floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)
        }
    }
    override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? {
        val root = view ?: return null
        val offset = if (geometry.top) -120f else 120f
        return AnimatorSet().apply {
            playTogether(ObjectAnimator.ofFloat(root, View.ALPHA, if (enter) 0f else 1f, if (enter) 1f else 0f),
                ObjectAnimator.ofFloat(root, View.TRANSLATION_Y, if (enter) offset else 0f, if (enter) 0f else offset))
            duration = 160; interpolator = AccelerateInterpolator()
        }
    }
    override fun onDestroyView() {
        bound = false; announced = false; focused = false
        surface?.animate()?.cancel(); recordPageStack?.close(); recordPageStack = null
        outer = null; surface = null; host?.get()?.destroyed(this)
        super.onDestroyView()
    }
    companion object {
        const val TAG = "native_download_panel"
        fun create(geometry: DownloadPanelGeometry) = DownloadPanelFragment().apply {
            arguments = Bundle().apply { putInt("width", geometry.width); putInt("height", geometry.height); putInt("gravity", geometry.gravity) }
        }
    }
}
