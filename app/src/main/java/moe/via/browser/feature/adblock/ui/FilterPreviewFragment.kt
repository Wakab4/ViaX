package moe.via.browser.feature.adblock.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.via.browser.R
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.theme.ThemedActivity

class FilterPreviewFragment : AdblockRecordPage() {
    private var page: AdblockListPage? = null
    private var adapter: FilterPreviewAdapter? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val title = arguments?.getString("title") ?: getString(R.string.ref_adblock_page_preview)
        return AdblockListPage(activity, title, appliesWindowInsets, ::closePage).also {
            page = it; adapter = FilterPreviewAdapter(activity) { line ->
                dialogs.show(BrowserDialog(activity, getString(R.string.ref_adblock_page_preview), line).positive(getString(android.R.string.ok)))
            }; it.list.adapter = adapter
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val path = arguments?.getString("path")
        work({ FilterPreviewLines.read(path) }) { lines ->
            adapter?.submit(lines); NativeLog.debug(null, "count: ${lines.size}"); page?.empty(lines.isEmpty())
        }
    }
    override fun onDestroyView() { page?.close(); page = null; adapter = null; super.onDestroyView() }
}
