package moe.via.browser.feature.scripts.ui.resource

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import moe.via.browser.R
import moe.via.browser.core.files.NativeTextFiles
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.scroll.NativeScrollView
import moe.via.browser.ui.pages.RecordPageFragment
import moe.via.browser.ui.pages.RecordPageScope
import moe.via.browser.ui.tasks.NativeLifecycleJobs
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.palette
import java.io.File

/** k8.k accepts a file_path, independently of script URL/cache lookup. It only presents plain text. */
class ScriptResourceViewerFragment : RecordPageFragment() {
    private var page: PageScaffold? = null
    private var scroll: NativeScrollView? = null
    private var text: TextView? = null
    private var progress: ProgressBar? = null
    private var jobs: NativeLifecycleJobs? = null
    private val begin = Runnable { load() }
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val page = PageScaffold(activity, getString(R.string.ref_script_resource_view), ::closePage, appliesWindowInsets).also { this.page = it }
        val input = NativeTypography.text(activity).apply {
            setTextIsSelectable(true); typeface = Typeface.MONOSPACE; setTextColor(activity.palette.text); textDirection = View.TEXT_DIRECTION_LTR
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.settings_detail_size).toFloat()); setPadding(dp(16), dp(16), dp(16), dp(16))
        }.also { text = it }
        val scroll = NativeScrollView(activity).apply { addView(input, FrameLayout.LayoutParams(-1, -2)) }.also { this.scroll = it }
        val bar = ProgressBar(activity).apply { isIndeterminate = false }.also { progress = it }
        page.showContent(FrameLayout(activity).apply { addView(bar, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER)); addView(scroll, FrameLayout.LayoutParams(-2, -2)) })
        NativeToolbarTap.bind(page.toolbar) { scroll.smoothScrollTo(0, 0) }
        return page.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); jobs = NativeLifecycleJobs { viewLifecycleOwner.lifecycle }; view.postDelayed(begin, 200) }
    private fun load() {
        if (!isVisible) return
        progress!!.visibility = View.VISIBLE; scroll!!.visibility = View.GONE
        val path = arguments?.getString("file_path")
        jobs?.launch({ val file = path?.takeIf(String::isNotEmpty)?.let(::File); file?.name to NativeTextFiles.read(file) }) { value ->
            progress!!.visibility = View.GONE; scroll!!.visibility = View.VISIBLE
            value.first?.takeIf(String::isNotEmpty)?.let { page!!.titleView.text = it }
            text!!.text = value.second ?: "Cannot read file at: $path"
        }
    }
    override fun onDestroyView() { view?.removeCallbacks(begin); jobs?.close(); jobs = null; scroll?.dispose(); scroll = null; page = null; text = null; progress = null; super.onDestroyView() }
    companion object {
        internal fun open(owner: Fragment, path: String) { RecordPageScope.of(owner)?.push(ScriptResourceViewerFragment::class.java, Bundle().apply { putString("file_path", path) }) }
    }
}
