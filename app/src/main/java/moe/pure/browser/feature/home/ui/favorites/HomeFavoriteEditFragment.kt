package moe.pure.browser.feature.home.ui.favorites

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import moe.pure.browser.core.documents.NativeImagePickerContract
import moe.pure.browser.R
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.feature.home.data.HomeFavoriteInput
import moe.pure.browser.feature.home.data.HomeFavoritesRepository
import moe.pure.browser.feature.home.data.HomePreferences
import moe.pure.browser.feature.home.data.NativeHomeFavorites
import moe.pure.browser.feature.home.model.FavoriteDesign
import moe.pure.browser.feature.home.model.HomeFavorite
import moe.pure.browser.ui.message.MessageContent
import moe.pure.browser.ui.message.MessageOwner
import moe.pure.browser.ui.tasks.NativeViewJobs

/** qa.o owns its editor, image result and view-scoped IO; the browser only opens the parent. */
class HomeFavoriteEditFragment : Fragment() {
    private val state get() = ViewModelProvider(requireParentFragment())[HomeFavoritesPanelState::class.java]
    private lateinit var repository: HomeFavoritesRepository
    private lateinit var design: FavoriteDesign
    private var fields: HomeFavoriteEditor? = null
    private var selected: HomeFavorite? = null
    private var jobs: NativeViewJobs? = null
    private var icon: FavoriteIconEditor? = null
    private val input = FavoriteFormInput()
    private val picker = registerForActivityResult(NativeImagePickerContract()) { uri -> if (uri != null) icon?.picked(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = NativeHomeFavorites.get(requireContext())
        design = HomePreferences(requireContext()).favoriteDesign
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val form = HomeFavoriteEditor(inflater.context)
        form.onSubmit = ::save; form.onClose = { state.grid() }; fields = form
        return form.view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val owner = NativeViewJobs().also { jobs = it }
        val form = checkNotNull(fields)
        icon = FavoriteIconEditor(requireContext(), form.icons, design, owner, ::pickImage)
        state.selected.observe(viewLifecycleOwner) { item ->
            selected = item; form.title.setText(item.title); form.url.setText(item.url); icon?.load(item.url)
        }
    }

    private fun save() {
        val form = fields ?: return
        if (!input.begin()) return
        val title = form.title.text.toString()
        val raw = form.url.text.toString().trim { it <= ' ' }
        val messages = MessageOwner(requireActivity())
        if (title.isEmpty()) { messages.show(MessageContent(input.required(form.title))); return }
        if (raw.isEmpty()) { messages.show(MessageContent(input.required(form.url))); return }
        val item = selected ?: return
        val url = HomeFavoriteInput.resolve(requireContext(), raw)
        val iconOwner = checkNotNull(icon)
        item.rawTitle = title; item.rawUrl = url
        jobs?.launch({ repository.save(item).also { if (it > 0) iconOwner.apply(item.url) } }, failed = { NativeLog.error(null, it) }) { result ->
            if (result < 0) messages.show(MessageContent(getString(R.string.ref_bookmark_document_failure)))
            else { HomePreferences(requireContext()).markDirty(html = true, style = false); state.grid(selected) }
        }
    }

    private fun pickImage() {
        if (selected == null) return
        try { picker.launch(null) } catch (error: ActivityNotFoundException) { NativeLog.error(null, error) }
    }
    override fun onDestroyView() {
        jobs?.close(); jobs = null; icon = null; fields = null
        super.onDestroyView()
    }
}
