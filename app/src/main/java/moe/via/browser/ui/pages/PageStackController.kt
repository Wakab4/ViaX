package moe.via.browser.ui.pages

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.fragment.app.FragmentManager
import moe.via.browser.R
import moe.via.browser.ui.theme.ThemedActivity
import java.util.WeakHashMap
import java.lang.ref.WeakReference
import androidx.fragment.app.Fragment

/** A shared Fragment stack above the existing activity page, preserving the actual underlying view. */
internal class PageStackController(private val activity: ThemedActivity,
    private val manager: FragmentManager = activity.supportFragmentManager,
    private val target: (() -> FrameLayout?)? = null,
    private val containerId: Int = R.id.record_page_stack,
    val embedded: Boolean = false,
    private val presented: () -> Boolean = { true },
    private val rootBack: (() -> Unit)? = null) : AutoCloseable {
    private var container: FrameLayout? = null
    private var base: View? = null
    private var baseFragment: WeakReference<Fragment>? = null
    private var presentationAllowed = true
    private var closed = false
    private var showing = false
    private var registration: AutoCloseable? = null
    val isPresented get() = !closed && presentationAllowed && presented()
    private val dispatched = WeakHashMap<Any, Long>()
    private val listener = FragmentManager.OnBackStackChangedListener { synchronize() }
    private val callbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(fm: FragmentManager, fragment: androidx.fragment.app.Fragment, view: View, savedInstanceState: Bundle?) {
            if (fragment is RecordPageFragment) synchronize()
        }
    }
    init {
        manager.addOnBackStackChangedListener(listener); manager.registerFragmentLifecycleCallbacks(callbacks, false)
        if (embedded) registration = activity.registerRecordPageStack(this)
    }
    private fun pages() = manager.fragments.filterIsInstance<RecordPageFragment>().filter { it.isAdded && !it.isRemoving }
    val top get() = pages().lastOrNull { !it.isHidden }
    fun attach(baseView: View? = null) {
        if (closed) return
        val content = if (target != null) target.invoke() else activity.findViewById<FrameLayout>(android.R.id.content)
        content ?: return
        var layer = container
        if (layer == null) layer = FrameLayout(activity).apply { id = containerId; layoutParams = FrameLayout.LayoutParams(-1, -1) }.also { container = it }
        layer.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) { parent.post { synchronize() } }
            override fun onChildViewRemoved(parent: View, child: View) {
                parent.post { synchronize() }
            }
        })
        if (baseView != null) base = baseView
        else if (base?.parent !== content) base = (0 until content.childCount).map(content::getChildAt).firstOrNull { it !== layer }
        if (layer.parent !== content) {
            (layer.parent as? android.view.ViewGroup)?.removeView(layer)
            content.addView(layer)
        }
        // Process messages are separate children above this layer; pushing a page must not cover them.
        synchronize()
    }
    private fun synchronize() {
        if (closed) return
        val records = pages()
        if (!presentationAllowed) {
            container?.visibility = View.GONE
            base?.apply { visibility = View.VISIBLE; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO }
            return
        }
        val shown = records.isNotEmpty()
        container?.let { it.visibility = if (shown || it.childCount > 0) View.VISIBLE else View.GONE }
        base?.let {
            it.visibility = if (shown && baseFragment?.get() == null) View.GONE else View.VISIBLE
            it.importantForAccessibility = if (shown) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            // FragmentManager animates a registered base; animating its ancestor repeats that transition.
            if (showing && !shown && baseFragment?.get() == null && !activity.isFinishing)
                it.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.page_pop_enter))
        }
        if (!embedded && shown != showing) activity.recordPageVisibilityChanged(shown)
        showing = shown
    }
    fun push(type: Class<out RecordPageFragment>, arguments: Bundle?, root: Boolean = false): Boolean {
        if (closed || activity.isFinishing || activity.isDestroyed || manager.isDestroyed || manager.isStateSaved || !root && !isPresented) return false
        attach()
        val before = top
        if (!root && (before?.isVisible == false || before == null && base?.isShown != true)) return false
        val key = before ?: baseFragment?.get() ?: base ?: activity
        val now = SystemClock.elapsedRealtime()
        if (dispatched[key]?.let { now - it <= 300L } == true) return false
        dispatched[key] = now
        val transaction = manager.beginTransaction().setReorderingAllowed(true)
        if (!embedded || !root) transaction.setCustomAnimations(R.anim.page_enter, R.anim.page_exit, R.anim.page_pop_enter, R.anim.page_pop_exit)
        transaction.add(containerId, type, arguments, type.simpleName)
        if (!root) {
            (before ?: baseFragment?.get()?.takeIf { it.isAdded })?.let(transaction::hide)
        }
        if (before == null && !root && baseFragment?.get() == null)
            base?.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.page_exit))
        if (!root) transaction.addToBackStack(null)
        transaction.commit()
        return true
    }
    fun pop(owner: RecordPageFragment? = null): Boolean {
        val current = top ?: return false
        if (closed || owner != null && owner !== current) return false
        if (manager.isStateSaved) return true
        if (current.consumeBack()) return true
        if (manager.backStackEntryCount == 0) return false
        manager.popBackStack(); return true
    }
    /** s6.a0 clears the browser's record stack before dispatching an explicit browser entry. */
    fun returnToBase(): Boolean {
        if (closed || manager.isStateSaved) return false
        manager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        manager.executePendingTransactions(); dispatched.clear(); return true
    }
    fun handleBack(): Boolean {
        if (!isPresented || top == null) return false
        if (pop()) return true
        val close = rootBack ?: return false
        close(); return true
    }
    fun bindBase(fragment: Fragment) { baseFragment = WeakReference(fragment); synchronize() }
    fun presentation(allowed: Boolean) { presentationAllowed = allowed; synchronize() }
    fun closeRoot() { rootBack?.invoke() ?: activity.finish() }
    fun hostPresented() { if (isPresented) top?.hostPresented() }
    fun reveal(owner: RecordPageFragment): PageReveal? {
        if (closed || manager.isStateSaved || manager.backStackEntryCount == 0 || top !== owner || !owner.isVisible) return null
        val preceding = pages().takeWhile { it !== owner }.lastOrNull { it.view != null }
        val previousFragment = preceding ?: baseFragment?.get()
        val view = previousFragment?.view ?: base ?: return null
        return PageReveal(view) { previousFragment?.isHidden ?: (top === owner) }
    }
    fun themeChanged() { pages().forEach { if (it.view != null) it.themeChanged() } }
    override fun close() {
        closed = true; manager.removeOnBackStackChangedListener(listener); manager.unregisterFragmentLifecycleCallbacks(callbacks)
        registration?.close(); registration = null
        container = null; base = null; dispatched.clear()
        baseFragment = null
    }
}
