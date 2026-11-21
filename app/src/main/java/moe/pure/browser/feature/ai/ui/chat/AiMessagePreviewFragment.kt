package moe.pure.browser.feature.ai.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.feature.ai.data.AiProvider
import moe.pure.browser.ui.pages.NativePageFragment
import moe.pure.browser.ui.pages.PageDragLayout
import moe.pure.browser.ui.tasks.NativeLifecycleJobs

/** q0 is an ordinary child Fragment, with its own 200dp widget.y drag over the live chat surface. */
internal class AiMessagePreviewFragment : NativePageFragment() {
    private var page: AiMessagePreview? = null
    private var drag: PageDragLayout? = null
    private var jobs: NativeLifecycleJobs? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val content = AiMessagePreview(requireContext()) { parentFragmentManager.popBackStack() }.also { page = it }
        return PageDragLayout(requireContext(), object : PageDragLayout.Listener {
            override fun begin(direction: Int) = isVisible && !parentFragmentManager.isStateSaved
            override fun progress(fraction: Float) = Unit
            override fun cancelled() = Unit
            override fun completed() { if (!isDetached) parentFragmentManager.popBackStackImmediate() }
        }).also { it.content(content); drag = it }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val id = arguments?.getString("id")
        if (id == null) { page?.missing(); return }
        val provider = AiProvider.get(requireContext())
        val markdown = provider.markdown
        jobs = NativeLifecycleJobs { viewLifecycleOwner.lifecycle }.also { work -> work.launch({
            val message = provider.repository.messages.get(id)
            if (message == null || message.content.isEmpty()) null to null else {
                val body = message.content.trim { it <= ' ' }
                val source = if (message.reasoning.isNullOrEmpty()) body
                    else "> " + message.reasoning.trim { it <= ' ' }.replace("\n", "\n> ") + "\n\n" + body
                source to markdown.render(source)
            }
        }) { (raw, formatted) -> if (raw == null) page?.missing() else page?.show(raw, formatted) } }
    }
    override fun onDestroyView() { jobs?.close(); jobs = null; drag?.close(); drag = null; page = null; super.onDestroyView() }
}
