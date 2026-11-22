package moe.via.browser.feature.download.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.via.browser.ui.pages.RecordPageFragment
import moe.via.browser.ui.pages.RecordPageScope

/** b1's list and its child launchers belong to this record page. */
class DownloadManagerFragment : RecordPageFragment() {
    private val state = DownloadPanelState()
    private var content: DownloadPageContent? = null
    private var pendingState: Bundle? = null
    private var rebuilding = false
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rebuilding = false
        val restored = pendingState ?: savedInstanceState
        pendingState = null
        return DownloadPageContent(requireActivity(), state, childFragmentManager, restored,
            visible = { isAdded && !isHidden && !isRemoving && RecordPageScope.of(this)?.isPresented != false },
            onBack = ::closePage, editingChanged = { editing -> dragLayout?.gestures(!editing) },
            applyInsets = appliesWindowInsets).also { content = it }.page.root
    }
    override fun draggable() = arguments?.getBoolean("draggable", true) != false
    internal override fun hostPresented() { if (isResumed && !rebuilding) content?.resume() }
    internal override fun canNavigateBack() = content?.canNavigateBack ?: true
    internal override fun consumeBack() = content?.consumeBack() == true
    override fun onResume() { super.onResume(); content?.resume() }
    override fun onPause() { content?.pause(); super.onPause() }
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) content?.covered() else if (isResumed) content?.resume()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        (content?.save() ?: pendingState)?.let(outState::putAll)
        super.onSaveInstanceState(outState)
    }
    internal override fun themeChanged() = rebuild()
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); rebuild() }
    private fun rebuild() {
        if (rebuilding || view == null || parentFragmentManager.isStateSaved) return
        pendingState = content?.save(); rebuilding = true
        // Native o8.a.b0 rebuilds page views via detach/attach; the Fragment and child models survive.
        parentFragmentManager.beginTransaction().detach(this).attach(this).commit()
    }
    override fun onDestroyView() { content?.close(); content = null; super.onDestroyView() }
}
