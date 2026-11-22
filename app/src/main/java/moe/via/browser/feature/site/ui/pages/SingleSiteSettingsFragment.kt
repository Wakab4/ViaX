package moe.via.browser.feature.site.ui.pages

import android.os.Bundle
import android.widget.ImageView
import moe.via.browser.R
import moe.via.browser.feature.site.ui.editor.SingleSiteController
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.theme.ThemedActivity

/** jb.u4 commits and publishes even an empty result Bundle when the Fragment is destroyed. */
class SingleSiteSettingsFragment : SiteRecordPage() {
    private var editor: SingleSiteController? = null
    override fun createContent(activity: ThemedActivity, back: ImageView) = SingleSiteController(activity, repository, settings, agents,
        arguments?.getString("domain", "www.example.com") ?: "www.example.com", arguments?.getInt("flags", 0) ?: 0, ::closePage).also { editor = it }
    override fun configure(page: PageScaffold) {
        page.addTextAction(getString(R.string.ref_site_reset)) { editor?.reset() }
    }
    override fun onDestroy() {
        editor?.commit()?.let { result ->
            parentFragmentManager.setFragmentResult(RESULT, Bundle().apply { if (result.flags != 0) putInt("changed", result.flags) })
        }
        editor = null; super.onDestroy()
    }
    companion object { const val RESULT = "result" }
}
