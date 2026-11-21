package moe.pure.browser.feature.home.ui.customize

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.core.documents.NativeImagePickerContract
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.feature.home.background.NativeBackgroundFiles
import moe.pure.browser.feature.home.model.HomeDraftKind
import moe.pure.browser.feature.home.model.LogoMode
import moe.pure.browser.feature.home.ui.customize.image.HomeImageActions
import moe.pure.browser.feature.home.ui.customize.palette.BackgroundPaletteAdapter
import moe.pure.browser.feature.home.ui.customize.palette.BackgroundPaletteEntry
import moe.pure.browser.R
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.editor.NativeTextEditorFragment
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.pages.NativePageFragment
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.ui.theme.ThemedActivity

/** The five tool entries are replaced in q's child container; photo results belong to that child. */
class HomeDesignToolsFragment : NativePageFragment() {
    private val owner get() = requireParentFragment() as HomeCustomizeFragment
    private val model get() = owner.model
    private val page get() = DesignPage.valueOf(requireArguments().getString(PAGE)!!)
    private var panel: DesignToolPanel? = null
    private var tools: HomeDesignPanels? = null
    private var dialogs: HomeCustomizeDialogs? = null
    private var scope: DialogScope? = null
    private var images: HomeImageActions? = null
    private var paletteOpen = false
    private var palette: BackgroundPaletteAdapter? = null
    private val picker = registerForActivityResult(NativeImagePickerContract()) { uri -> images?.importImage(uri, page == DesignPage.LOGO) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        DesignToolPanel(inflater.context, ::applyTool).also { panel = it }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState); paletteOpen = false
        val activity = requireActivity() as ThemedActivity
        val scope = DialogScope().also { this.scope = it }
        if (page == DesignPage.LOGO || page == DesignPage.BACKGROUND) images = HomeImageActions(this, model, ::refresh) { position ->
            palette?.select(position); paletteOpen = false; refresh()
        }
        tools = HomeDesignPanels(activity, page, model, object : HomeDesignPanels.Commands {
            override fun chooseLogo() { dialogs!!.logo() }
            override fun chooseBackground() = showPalette()
            override fun editCss() = editCode(html = false)
            override fun clearIcons() { dialogs!!.clearIcons() }
            override fun reset() { dialogs!!.reset() }
        }) { palette?.selectedEntry }
        dialogs = HomeCustomizeDialogs(activity, model, ::pickImage, { editCode(html = true) }, ::refresh, { model.reset() },
            { model.cacheCleared() }, { BrowserMessages.show(activity, it) }, scope)
        if (page == DesignPage.BACKGROUND) {
            palette = BackgroundPaletteAdapter(activity, model.preferences.accentColor, !model.preferences.backgroundPath.isNullOrEmpty(), ::selectBackground)
            panel?.bindBackgroundTouch()
        }
        if (page == DesignPage.BACKGROUND && model.preferences.backgroundPath.isNullOrEmpty()) showPalette(deferred = true) else refresh()
        panel?.focusTools()
    }

    internal fun refresh() {
        tools?.let {
            val items = it.tools()
            if (paletteOpen) panel?.updateTools(items) else panel?.render(items)
        }
    }
    private fun applyTool(tool: DesignTool, position: Int) { tools?.apply(tool) { panel?.changed(position) } }
    private fun editCode(html: Boolean) {
        val manager = owner.parentFragmentManager
        manager.setFragmentResultListener(NativeTextEditorFragment.RESULT, this) { _, result ->
            result.getString(NativeTextEditorFragment.TEXT)?.let { text ->
                if (html) {
                    model.preferences.setHtmlLogo(text); refresh(); model.preferences.markDirty(html = true, style = false)
                } else model.preferences.additionalStyleSheet = text
                model.documentChanged()
                if (html) model.preferences.rememberDraft(HomeDraftKind.HTML, text)
            }
            manager.clearFragmentResultListener(NativeTextEditorFragment.RESULT)
        }
        val title = getString(if (html) R.string.ref_text_editor_html else R.string.ref_text_editor_css)
        val text = if (html) model.preferences.logoContent.takeIf { model.preferences.logoMode == LogoMode.HTML && !it.isNullOrEmpty() }
            ?: model.preferences.readDraft(HomeDraftKind.HTML) else model.preferences.additionalStyleSheet
        RecordPageScope.of(owner)?.push(NativeTextEditorFragment::class.java, NativeTextEditorFragment.arguments(title, text, title, true))
    }
    private fun showPalette(deferred: Boolean = false) {
        paletteOpen = true
        panel?.showPalette(palette!!, deferred)
    }
    private fun selectBackground(entry: BackgroundPaletteEntry, position: Int) {
        if (entry == BackgroundPaletteEntry.Image) { pickImage(); return }
        if (position == palette!!.selectedPosition) return
        val color = (entry as BackgroundPaletteEntry.Color).value
        NativeBackgroundFiles.clear(requireContext())
        model.preferences.setBackgroundColor(if (color == 0) -1 else color)
        model.backgroundDesign.apply { overrideLight = false; opacity = 0 }; model.saveBackground(notifyDocument = false)
        model.preferences.markDirty()
        model.backgroundChanged(); model.documentChanged()
        palette!!.select(position); refresh()
    }
    private fun pickImage() {
        try { picker.launch(null) } catch (error: ActivityNotFoundException) { NativeLog.error(null, error) }
    }
    override fun onDestroyView() {
        images?.close(); images = null; scope?.dispose(); scope = null
        dialogs = null; tools = null; palette = null; panel = null
        super.onDestroyView()
    }
    companion object {
        private const val PAGE = "home_design_tool"
        internal fun create(page: DesignPage) = HomeDesignToolsFragment().apply {
            arguments = Bundle().apply { putString(PAGE, page.name) }
        }
    }
}
