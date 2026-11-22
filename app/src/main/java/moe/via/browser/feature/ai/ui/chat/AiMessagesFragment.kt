package moe.via.browser.feature.ai.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentManager
import moe.via.browser.R
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.pages.NativePageFragment
import moe.via.browser.ui.theme.ThemedActivity

internal class AiMessagesFragment : NativePageFragment() {
    private val owner get() = parentFragment as AiChatDialogFragment
    private var page: AiChatPage? = null
    private var dialogs: DialogScope? = null
    private var menus: AiChatMenus? = null
    private val export = AiChatExport(this, { owner.model })
    private val stackChanged = FragmentManager.OnBackStackChangedListener { owner.model.setPaging(childFragmentManager.backStackEntryCount == 0) }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val conversation = owner.model
        val scope = DialogScope().also { dialogs = it }
        val menus = AiChatMenus(activity, scope, conversation, { viewLifecycleOwner.lifecycle }, { message ->
            conversation.setPaging(false)
            childFragmentManager.beginTransaction().setCustomAnimations(R.anim.page_enter, R.anim.ref_ai_preview_stationary,
                R.anim.ref_ai_preview_stationary, R.anim.page_pop_exit)
                .add(R.id.ai_chat_message_surface, AiMessagePreviewFragment::class.java, Bundle().apply { putString("id", message.id) }, null)
                .addToBackStack(null).commit()
        }, { conversation.requestAction(1) }, { export.start(page!!.titleText()) }).also { this.menus = it }
        val content = AiChatPage(activity, { conversation.showPage(0) }, { anchor ->
            page?.hideKeyboard(); anchor.postDelayed({ if (view != null) menus.more(anchor) }, 30)
        }, conversation::send, conversation::stop, conversation::selectPrompt, conversation::removeAttachment, menus::message, conversation::isGenerating)
        page = content
        return FrameLayout(activity).apply { id = R.id.ai_chat_message_surface; addView(content, FrameLayout.LayoutParams(-1, -1)) }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childFragmentManager.addOnBackStackChangedListener(stackChanged)
        owner.model.rowChanged.observe(viewLifecycleOwner) { page?.updateMessage(it) }
        owner.model.topic.observe(viewLifecycleOwner) { page?.showTopic(it) }
        owner.model.display.observe(viewLifecycleOwner) { page?.submitMessages(it) }
        owner.model.busy.observe(viewLifecycleOwner) { value -> if (value != null) page?.showBusy(value) }
        owner.model.promptChoices.observe(viewLifecycleOwner) { page?.submitPrompts(owner.model.attachment.value, it) }
        owner.model.backRequests.observe(viewLifecycleOwner) { index ->
            if (index == 1) { if (childFragmentManager.backStackEntryCount > 0) childFragmentManager.popBackStackImmediate() else owner.model.dismiss() }
        }
    }
    override fun onDestroyView() {
        childFragmentManager.removeOnBackStackChangedListener(stackChanged)
        page?.close(); page = null; menus?.close(); menus = null; dialogs?.dispose(); dialogs = null; super.onDestroyView()
    }
    override fun onDestroy() { export.close(); super.onDestroy() }
}
