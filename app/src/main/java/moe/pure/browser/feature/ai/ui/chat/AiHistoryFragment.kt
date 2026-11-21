package moe.pure.browser.feature.ai.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.ui.pages.NativePageFragment
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.theme.ThemedActivity

internal class AiHistoryFragment : NativePageFragment() {
    private val owner get() = parentFragment as AiChatDialogFragment
    private var page: AiHistoryPage? = null
    private var dialogs: DialogScope? = null
    private var firstLoad = true
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val conversation = owner.model
        val scope = DialogScope().also { dialogs = it }
        val menus = AiHistoryMenus(activity, scope, conversation) { page?.cancelSelection() }
        return AiHistoryPage(activity, { conversation.selectThread(it); conversation.showPage(1) }, { conversation.newThread(); conversation.showPage(1) },
            { conversation.requestAction(1) }, menus::deleteThreads) { selecting -> conversation.setPaging(!selecting); if (selecting) conversation.stop() }.also { page = it }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        owner.model.threadList.observe(viewLifecycleOwner) { page?.submit(it); firstLoad = false }
        owner.model.historyDirty.observe(viewLifecycleOwner) { firstLoad = true }
        owner.model.backRequests.observe(viewLifecycleOwner) { index -> if (index == 0 && !cancelSelection()) owner.model.dismiss() }
    }
    internal fun cancelSelection() = page?.cancelSelection() == true
    override fun onResume() { super.onResume(); if (firstLoad) owner.model.refreshHistory() }
    override fun onDestroyView() { page?.close(); page = null; dialogs?.dispose(); dialogs = null; super.onDestroyView() }
}
