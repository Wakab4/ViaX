package moe.via.browser.feature.scripts.popup.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import moe.via.browser.R
import moe.via.browser.feature.library.ui.recycler.LibraryRecyclerView
import moe.via.browser.feature.scripts.popup.model.ScriptPopupModel
import moe.via.browser.ui.components.SkinIconBinder
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette
import moe.via.browser.webview.compat.NativePageAddress

/** ua.t/i remain independent child Fragments and preserve their own RecyclerView hierarchy state. */
abstract class ScriptPopupPage : Fragment() {
    protected abstract val detail: Boolean
    private var skin: SkinIconBinder? = null
    private var list: LibraryRecyclerView? = null
    private val model get() = ViewModelProvider(requireParentFragment())[ScriptPopupModel::class.java]

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val header = ScriptPopupViews.Header(context, detail).apply { id = HEADER }
        val recycler = LibraryRecyclerView(context).apply {
            id = android.R.id.list; layoutManager = ScriptPopupLayoutManager(context, detail)
            layoutParams = RelativeLayout.LayoutParams(-1, -2).apply { addRule(RelativeLayout.BELOW, header.id) }
        }
        list = recycler
        val empty = NativeTypography.text(context).apply {
            id = EMPTY; text = "¯\\_(ツ)_/¯"; contentDescription = getString(R.string.ref_script_popup_empty)
            textSize = 22f; gravity = android.view.Gravity.CENTER; setTextColor(context.palette.text); visibility = View.GONE
        }
        val root = RelativeLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(-1, -1)
            addView(header, RelativeLayout.LayoutParams(-1, -2))
            addView(recycler)
            if (!detail) addView(empty, RelativeLayout.LayoutParams(-1, dp(100)).apply { addRule(RelativeLayout.BELOW, header.id) })
        }
        skin = SkinIconBinder(context).also { it.image(header.action, if (detail) R.drawable.ref_ic_back else R.drawable.ref_ic_add) }
        header.action.contentDescription = getString(if (detail) R.string.ref_script_popup_back else R.string.ref_script_popup_add)
        header.action.setOnClickListener { if (detail) model.back() else model.add() }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val header = view.findViewById<ScriptPopupViews.Header>(HEADER)
        val recycler = checkNotNull(list)
        if (detail) {
            val adapter = ScriptPopupActionsAdapter(model); recycler.adapter = adapter
            model.state.observe(viewLifecycleOwner) { state ->
                header.title.text = state.selected?.name ?: getString(R.string.ref_script_popup_title)
                header.action.isEnabled = state.bound
                adapter.submit(requireContext(), state)
            }
        } else {
            val adapter = ScriptPopupListAdapter(model); recycler.adapter = adapter
            model.state.observe(viewLifecycleOwner) { state ->
                val host = NativePageAddress.authority(state.url)
                header.title.text = if (host.isEmpty()) getString(R.string.ref_script_popup_title) else getString(R.string.ref_script_popup_for_site, host)
                header.action.isEnabled = state.bound
                adapter.submit(state)
                val isEmpty = state.loaded && state.scripts.isEmpty()
                view.findViewById<View>(EMPTY).visibility = if (isEmpty) View.VISIBLE else View.GONE
                recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
            }
        }
    }
    override fun onStart() { super.onStart(); view?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO; view?.requestLayout() }
    override fun onPause() { view?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS; super.onPause() }
    override fun onDestroyView() { list?.adapter = null; list?.dispose(); list = null; skin?.close(); skin = null; super.onDestroyView() }
    private companion object { val HEADER = R.id.script_popup_header; val EMPTY = R.id.script_popup_empty }
}

class ScriptPopupListPage : ScriptPopupPage() { override val detail = false }
class ScriptPopupActionsPage : ScriptPopupPage() { override val detail = true }
