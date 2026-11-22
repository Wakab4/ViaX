package moe.via.browser.ui.pages

import android.os.Bundle
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import moe.via.browser.storage.preferences.BrowserPreferences
import moe.via.browser.ui.theme.ThemedActivity

/** o8.h's page contract. Feature leaves own their content, state and resource lifetime. */
abstract class RecordPageFragment : NativePageFragment() {
    internal var dragLayout: PageDragLayout? = null
        private set
    private var reveal: PageReveal? = null
    private var systemBack: OnBackPressedCallback? = null
    protected abstract fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    internal open fun themeChanged() = Unit
    internal open fun canNavigateBack() = true
    internal open fun consumeBack() = false
    internal open fun hostPresented() = Unit
    internal open fun applyWindowAppearance(): Boolean = false
    protected open fun draggable() = true
    protected val appliesWindowInsets get() = RecordPageScope.of(this)?.embedded != true
    protected fun closePage() {
        val stack = RecordPageScope.of(this) ?: return
        if (!stack.pop(this)) stack.closeRoot()
    }
    final override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (!draggable()) return createPage(inflater, container, savedInstanceState)
        return PageDragLayout(requireContext(), object : PageDragLayout.Listener {
            override fun begin(direction: Int): Boolean {
                if (!canNavigateBack()) return false
                reveal?.close()
                reveal = RecordPageScope.of(this@RecordPageFragment)?.reveal(this@RecordPageFragment)
                return reveal != null
            }
            override fun progress(fraction: Float) { reveal?.progress(fraction) }
            override fun cancelled() { reveal?.close(); reveal = null }
            override fun completed() {
                reveal?.complete(); reveal?.close(); reveal = null
                RecordPageScope.of(this@RecordPageFragment)?.pop(this@RecordPageFragment)
            }
        }).also { host -> dragLayout = host; host.content(createPage(inflater, container, savedInstanceState)) }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                val unsupported = listOf(Build.BRAND, Build.MANUFACTURER).any { it.equals("HONOR", true) || it.equals("HUAWEI", true) }
                val disabled = BrowserPreferences(requireActivity().getSharedPreferences("browser", 0)).predictiveBackDisabled
                if (Build.VERSION.SDK_INT >= 35 && !unsupported && !disabled && canNavigateBack()) {
                    dragLayout?.beginPredictive()
                }
            }
            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                dragLayout?.predictiveProgress(backEvent.progress)
            }
            override fun handleOnBackCancelled() { dragLayout?.settlePredictive(false) }
            override fun handleOnBackPressed() {
                val drag = dragLayout
                if (drag?.predictiveActive == true) drag.settlePredictive(true)
                else closePage()
            }
        }
        systemBack = callback
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        updateSystemBack()
    }
    private fun updateSystemBack() {
        systemBack?.isEnabled = isResumed && isVisible && RecordPageScope.of(this)?.top === this
    }
    override fun onResume() { super.onResume(); updateSystemBack() }
    override fun onPause() { systemBack?.isEnabled = false; super.onPause() }
    override fun onHiddenChanged(hidden: Boolean) { super.onHiddenChanged(hidden); updateSystemBack() }
    override fun onDestroyView() {
        systemBack = null
        dragLayout?.close(); dragLayout = null; reveal?.close(); reveal = null
        super.onDestroyView()
    }
}
