package moe.pure.browser.feature.ai.ui.chat

import android.app.Dialog
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import moe.pure.browser.R
import moe.pure.browser.feature.ai.model.AiPageAttachment
import moe.pure.browser.feature.ai.session.AiConversation
import moe.pure.browser.feature.settings.context.SettingsWindowEffects
import moe.pure.browser.ui.components.NativePagerTouchSlop
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.ui.message.MessageContent
import moe.pure.browser.ui.message.MessageOwner

/** b9.g owns the actual e0 ViewModel and initializes each new dialog view in the native order. */
class AiChatDialogFragment : DialogFragment() {
    internal lateinit var model: AiConversation
        private set
    private var pager: ViewPager2? = null
    private var effects: AutoCloseable? = null
    private var notices: MessageOwner? = null
    private var placement = Gravity.BOTTOM
    private val stackChanged = FragmentManager.OnBackStackChangedListener { model.setPaging(childFragmentManager.backStackEntryCount == 0) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setStyle(STYLE_NO_TITLE, R.style.Theme_PureBrowser_Dialog)
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AiConversation(requireContext().applicationContext) as T
        })[AiConversation::class.java]
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = object : Dialog(requireContext(), theme) {
        @Deprecated("Native dialog back dispatch") override fun onBackPressed() { pager?.let { model.requestBack(it.currentItem) } }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ViewPager2(requireContext()).apply { id = R.id.ai_chat_dialog_pager }.also { pager = it }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pages = requireNotNull(pager)
        NativePagerTouchSlop.apply(pages)
        pages.adapter = object : FragmentStateAdapter(this@AiChatDialogFragment) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment = if (position == 0) AiHistoryFragment() else AiMessagesFragment()
        }
        childFragmentManager.addOnBackStackChangedListener(stackChanged)
        model.page.observe(viewLifecycleOwner) { pages.setCurrentItem(it, true) }
        model.paging.observe(viewLifecycleOwner) { pages.isUserInputEnabled = it }
        model.actions.observe(viewLifecycleOwner) { value -> parentFragmentManager.setFragmentResult("result", Bundle().apply { putInt("action", value) }); dismiss() }
        model.dismissRequested.observe(viewLifecycleOwner) { dismiss() }
        model.notice.observe(viewLifecycleOwner) { value ->
            val parent = pages.parent as? FrameLayout
            if (parent != null) {
                val owner = notices ?: MessageOwner(requireActivity(), parent).also { notices = it }
                owner.show(MessageContent(value, timeoutMillis = BrowserMessages.SHORT))
            }
        }
        model.readBackend()
        arguments?.let { placement = it.getInt("gravity", Gravity.BOTTOM) }
        arguments?.getBundle("attachment")?.takeIf { !it.isEmpty && it.getInt("a_type") == 1 }?.let {
            pages.setCurrentItem(1, false)
            model.attachment.value = AiPageAttachment(it.getString("w_url"), it.getString("w_title"), it.getString("w_content"))
        }
        arguments?.getString("id")?.let { pages.setCurrentItem(1, false); model.selectThread(it) }
        model.refreshPrompts()
    }
    override fun onStart() {
        super.onStart(); dialog?.setCanceledOnTouchOutside(true)
        effects?.close(); effects = dialog?.let(SettingsWindowEffects::attachDialog)
    }
    override fun onResume() { super.onResume(); updateGeometry() }
    @Suppress("DEPRECATION") fun updateGeometry() {
        val activity = activity ?: return
        val window = dialog?.window ?: return
        val size = Point(); activity.windowManager.defaultDisplay.getRealSize(size)
        val longSide = maxOf(size.x, size.y)
        val configured = resources.configuration.orientation
        val orientation = if (configured == Configuration.ORIENTATION_UNDEFINED) Resources.getSystem().configuration.orientation else configured
        val landscape = orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = if (landscape) minOf(activity.dp(440), longSide / 2) else -1
        val height = if (landscape) -1 else (longSide / 9) * 5
        val attributes = window.attributes
        if (attributes.width == width && attributes.height == height) return
        val gravity = placement
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = attributes.apply {
            this.width = width; this.height = height; this.gravity = gravity; dimAmount = .2f
            windowAnimations = if (gravity and Gravity.TOP == Gravity.TOP) R.style.ref_ai_chat_top_animation else R.style.ref_ai_chat_bottom_animation
        }
    }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); updateGeometry() }
    override fun onDestroyView() {
        childFragmentManager.removeOnBackStackChangedListener(stackChanged)
        notices?.close(false); notices = null; effects?.close(); effects = null; pager?.adapter = null; pager = null; super.onDestroyView()
    }
    companion object {
        const val TAG = "native_ai_chat"
        fun arguments(id: String?, attachment: AiPageAttachment?, gravity: Int) = Bundle().apply {
            if (id != null) putString("id", id)
            if (attachment != null) putBundle("attachment", Bundle().apply {
                putInt("a_type", 1); putString("w_url", attachment.url); putString("w_title", attachment.title); putString("w_content", attachment.content)
            })
            putInt("gravity", gravity)
        }
    }
}
