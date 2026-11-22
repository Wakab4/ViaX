package moe.via.browser.feature.sync.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import moe.via.browser.R
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.feature.sync.data.SyncRepository
import moe.via.browser.feature.sync.model.SyncKind
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.recycler.NativeRecyclerView
import moe.via.browser.ui.components.toolbar.NativeToolbarAction
import moe.via.browser.ui.theme.ThemedActivity

/** hb.a7 replaces real child Fragments and exposes only the opposite-page action. */
class SyncSettingsFragment : SettingsRecordPage() {
    private val repository by lazy { SyncRepository(requireContext().applicationContext) }
    private var page: PageScaffold? = null
    private var host: FrameLayout? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val content = FrameLayout(activity).apply { id = R.id.sync_page_content }
        host = content
        return PageScaffold(activity, getString(R.string.ref_sync_page_title), ::closePage, appliesWindowInsets).also {
            page = it; it.showContent(content)
            it.toolbar.addAction(NativeToolbarAction(R.id.sync_webdav_action, 1, null, getString(R.string.ref_sync_page_webdav))) { select(SyncKind.WEBDAV) }
            it.toolbar.addAction(NativeToolbarAction(R.id.sync_cloud_action, 1, null, getString(R.string.ref_sync_page_cloud))) { select(SyncKind.VIA_CLOUD) }
            NativeToolbarTap.bind(it.toolbar) { (childFragmentManager.findFragmentById(R.id.sync_page_content) as? SyncChildFragment)?.scrollToTop() }
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) select(repository.selectedKind) else actions(repository.selectedKind)
    }
    private fun actions(kind: SyncKind) {
        page?.toolbar?.setItemVisible(R.id.sync_cloud_action, kind != SyncKind.VIA_CLOUD)
        page?.toolbar?.setItemVisible(R.id.sync_webdav_action, kind == SyncKind.VIA_CLOUD)
    }
    private fun select(kind: SyncKind) {
        repository.selectedKind = kind; actions(kind)
        childFragmentManager.beginTransaction().replace(R.id.sync_page_content,
            if (kind == SyncKind.VIA_CLOUD) CloudSyncFragment::class.java else WebDavSyncFragment::class.java, null).commit()
    }
    internal fun bottom(list: NativeRecyclerView) { host?.let { page?.showContent(it, bottomInsets = list) } }
    override fun onDestroyView() { page = null; host = null; super.onDestroyView() }
}
