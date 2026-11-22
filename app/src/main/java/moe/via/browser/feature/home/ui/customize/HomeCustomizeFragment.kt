package moe.via.browser.feature.home.ui.customize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import moe.via.browser.R
import moe.via.browser.feature.home.ui.customize.preview.HomePreview
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.pages.RecordPageFragment
import moe.via.browser.ui.theme.ThemedActivity

/** d9.q owns its preview and replaces real child tool Fragments without a child back stack. */
class HomeCustomizeFragment : RecordPageFragment() {
    internal val model by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeCustomizeState(requireContext().applicationContext) as T
        })[HomeCustomizeState::class.java]
    }
    private var workspace: CustomizeWorkspace? = null
    private var preview: HomePreview? = null
    private var windowStyle: HomeCustomizeWindow? = null
    private var selected: DesignPage? = null
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); model.preferences.markDirty() }
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val page = CustomizeWorkspace(activity, ::selectPage, ::closePage, {
            workspace?.dismissHomepageHint(); model.preferences.homepage = "about:home"
            GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.WEB_SETTINGS)
        }, appliesWindowInsets)
        workspace = page
        NativeToolbarTap.bind(page.toolbar) { preview?.view?.scrollTo(0, 0) }
        return page.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        windowStyle = HomeCustomizeWindow(requireActivity()).also { it.apply() }
        preview = HomePreview(this, requireActivity(), workspace!!.previewHost, model)
        workspace?.homepageHint(model.preferences.homepage != "about:home")
        preview?.load()
        model.document.observe(viewLifecycleOwner) { preview?.refresh() }
        model.background.observe(viewLifecycleOwner) { preview?.backgroundChanged() }
        model.cache.observe(viewLifecycleOwner) { preview?.clearCache() }
        model.close.observe(viewLifecycleOwner) { selected = null; workspace?.showPage(null) }
        preview?.postBackground()
    }
    private fun selectPage(page: DesignPage) {
        if (page == selected) { selected = null; workspace?.showPage(null); workspace?.clearSelection() }
        else {
            selected = page; workspace?.showPage(page)
            childFragmentManager.beginTransaction().replace(R.id.home_design_tools, HomeDesignToolsFragment.create(page)).commit()
        }
    }
    internal override fun applyWindowAppearance(): Boolean {
        val style = windowStyle ?: return false
        style.apply(); return true
    }
    internal override fun themeChanged() {
        parentFragmentManager.beginTransaction().detach(this).commit()
        parentFragmentManager.beginTransaction().attach(this).commit()
    }
    override fun onResume() {
        preview?.resume(); super.onResume()
    }
    override fun onPause() { preview?.pause(); super.onPause() }
    override fun onDestroyView() {
        super.onDestroyView()
        windowStyle?.close(); windowStyle = null
    }
    override fun onDestroy() {
        model.preferences.markDirty(html = true, style = false)
        preview?.dispose(); preview = null
        super.onDestroy()
    }
}
