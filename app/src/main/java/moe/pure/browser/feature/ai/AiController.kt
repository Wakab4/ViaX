package moe.pure.browser.feature.ai

import moe.pure.browser.browser.host.BrowserRootFragment
import moe.pure.browser.feature.ai.data.AiProvider
import moe.pure.browser.feature.ai.ui.chat.AiChatDialogFragment
import moe.pure.browser.feature.ai.ui.settings.AiSettingsEntry
import moe.pure.browser.model.BrowserTab
import moe.pure.browser.ui.chrome.ChromeLayoutPolicy
import moe.pure.browser.ui.theme.ThemedActivity

/** The actual browser root owns the native dialog result; the dialog owns its conversation model. */
class AiController(private val activity: ThemedActivity, private val current: () -> BrowserTab?) {
    private val extractor = AiPageExtractor(activity)
    private val repository = AiProvider.get(activity).repository
    private var disposed = false
    private fun root() = requireNotNull(BrowserRootFragment.find(activity))
    private fun dialog() = root().childFragmentManager.findFragmentByTag(AiChatDialogFragment.TAG) as? AiChatDialogFragment
    fun show() {
        if (disposed || dialog()?.dialog?.isShowing == true) return
        val tab = current() ?: return
        val url = tab.url
        val gravity = ChromeLayoutPolicy.resolve(activity).popupGravity
        extractor.extract(tab) { attachment -> if (!disposed) {
            val root = root()
            val manager = root.childFragmentManager
            manager.setFragmentResultListener("result", root.viewLifecycleOwner) { _, result ->
                manager.clearFragmentResultListener("menu_result")
                if (result.getInt("action", 0) != 0) AiSettingsEntry.open(activity)
            }
            AiChatDialogFragment().apply {
                arguments = AiChatDialogFragment.arguments(repository.threadForUrl(url), attachment, gravity)
            }.show(manager, AiChatDialogFragment.TAG)
        } }
    }
    fun settings() = AiSettingsEntry.open(activity)
    fun rebind() { dialog()?.updateGeometry() }
    fun dispose() { disposed = true }
}
