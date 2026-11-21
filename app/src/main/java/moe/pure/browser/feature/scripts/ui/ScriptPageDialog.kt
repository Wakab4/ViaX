package moe.pure.browser.feature.scripts.ui

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import moe.pure.browser.R
import moe.pure.browser.feature.scripts.popup.model.ScriptPopupAction
import moe.pure.browser.feature.scripts.popup.model.ScriptPopupModel
import moe.pure.browser.feature.scripts.popup.ui.ScriptPopupActionsPage
import moe.pure.browser.feature.scripts.popup.ui.ScriptPopupListPage
import moe.pure.browser.feature.settings.context.SettingsWindowEffects
import moe.pure.browser.ui.components.dp

/** ua.y/k8.a: framework restoration builds a data-free shell before the startup gate binds it. */
class ScriptPageDialog : DialogFragment() {
    internal fun interface Host { fun perform(action: ScriptPopupAction) }
    private var host: Host? = null
    private var pager: ViewPager2? = null
    private var effects: AutoCloseable? = null
    private val model get() = ViewModelProvider(this)[ScriptPopupModel::class.java]
    val sourceIdentity get() = arguments?.getString(SOURCE)
    val sourcePid get() = arguments?.getString(PID)
    val sourceUrl get() = arguments?.getString(URL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_PureBrowser_Dialog)
        model.restore(savedInstanceState?.getBundle(MODEL))
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        model.arguments(sourceUrl)
        return ViewPager2(requireContext()).apply {
            id = R.id.script_popup_pager; layoutParams = ViewGroup.LayoutParams(-1, -1)
            isUserInputEnabled = false; offscreenPageLimit = 1
            adapter = object : FragmentStateAdapter(this@ScriptPageDialog) {
                override fun getItemCount() = 2
                override fun createFragment(position: Int): Fragment = if (position == 0) ScriptPopupListPage() else ScriptPopupActionsPage()
            }
        }.also { pager = it }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.state.observe(viewLifecycleOwner) { state -> state.page?.let { page ->
            pager?.takeIf { it.currentItem != page }?.setCurrentItem(page, true)
        } }
        model.event.observe(viewLifecycleOwner) { dispatch() }
    }
    override fun onStart() {
        super.onStart()
        val activity = activity ?: return
        val metrics = resources.displayMetrics
        dialog?.window?.apply {
            setLayout(minOf(activity.dp(384), minOf(metrics.widthPixels, metrics.heightPixels) - activity.dp(72)), -2)
            setGravity(android.view.Gravity.CENTER)
            attributes = attributes.apply { dimAmount = .4f; windowAnimations = R.style.Animation_PureBrowser_Dialog }
        }
        dialog?.setCanceledOnTouchOutside(true)
        dialog?.setOnKeyListener { _, key, event ->
            if (event.action != KeyEvent.ACTION_DOWN) false else {
                val direction = when (key) { KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP; KeyEvent.KEYCODE_DPAD_DOWN -> View.FOCUS_DOWN
                    KeyEvent.KEYCODE_DPAD_LEFT -> View.FOCUS_LEFT; KeyEvent.KEYCODE_DPAD_RIGHT -> View.FOCUS_RIGHT; else -> 0 }
                val root = childFragmentManager.fragments.firstOrNull { it.isVisible }?.view as? ViewGroup
                val focused = root?.findFocus()
                when {
                    direction == 0 || root == null -> false
                    focused == null -> { root.requestFocus(); true }
                    focused is EditText && (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) -> false
                    else -> focused.focusSearch(direction)?.let { it.requestFocus(); true } ?: false
                }
            }
        }
        effects?.close(); effects = dialog?.let(SettingsWindowEffects::attachDialog)
    }
    internal fun bind(context: Context, owner: Host) {
        host = owner
        model.bind(context.applicationContext, sourceUrl)
        dispatch()
    }
    internal fun isBound(owner: Host) = host === owner
    internal fun commands(value: List<moe.pure.browser.feature.scripts.runtime.UserScriptBridge.MenuCommand>) = model.commands(value)
    internal fun release(owner: Host, dismiss: Boolean) {
        if (host !== owner) return
        host = null; model.unbind()
        if (dismiss) dismissAllowingStateLoss()
    }
    private fun dispatch() {
        if (!isAdded || parentFragmentManager.isStateSaved) return
        val owner = host ?: return
        val event = model.event.value ?: return
        if (event.consume()) { dismiss(); owner.perform(event.action) }
    }
    override fun onResume() { super.onResume(); dispatch() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putBundle(MODEL, model.save()); super.onSaveInstanceState(outState) }
    override fun onStop() { effects?.close(); effects = null; super.onStop() }
    override fun onDestroyView() {
        host = null; model.unbind(); pager?.adapter = null; pager = null
        effects?.close(); effects = null; super.onDestroyView()
    }
    companion object {
        const val TAG = "native_script_pages"
        private const val URL = "entry_url"
        private const val SOURCE = "source_identity"
        private const val PID = "source_pid"
        private const val MODEL = "script_pages_model"
        fun create(tabId: String, source: String, pid: String?, url: String?) = ScriptPageDialog().apply {
            arguments = Bundle().apply { putString(URL, url); putString(SOURCE, source); putString(PID, pid); putString("source_tab_id", tabId) }
        }
    }
}
