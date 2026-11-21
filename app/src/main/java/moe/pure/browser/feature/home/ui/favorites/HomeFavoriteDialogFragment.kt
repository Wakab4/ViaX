package moe.pure.browser.feature.home.ui.favorites

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import moe.pure.browser.core.documents.NativeImagePickerContract
import moe.pure.browser.R
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.feature.home.data.HomeFavoriteInput
import moe.pure.browser.feature.home.data.HomePreferences
import moe.pure.browser.feature.home.data.NativeHomeFavorites
import moe.pure.browser.feature.home.model.FavoriteDesign
import moe.pure.browser.feature.home.model.HomeFavorite
import moe.pure.browser.ui.message.MessageContent
import moe.pure.browser.ui.message.MessageOwner
import moe.pure.browser.ui.tasks.NativeViewJobs

/** qa.h0 loads after the original dialog is created and reports fav_result before dismissal. */
class HomeFavoriteDialogFragment : DialogFragment() {
    private lateinit var design: FavoriteDesign
    private var jobs: NativeViewJobs? = null
    private var icon: FavoriteIconEditor? = null
    private var title: EditText? = null
    private var url: EditText? = null
    private var item = HomeFavorite()
    private val input = FavoriteFormInput()
    private var effects: AutoCloseable? = null
    private val picker = registerForActivityResult(NativeImagePickerContract()) { uri -> if (uri != null) icon?.picked(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_PureBrowser_Dialog)
        design = HomePreferences(requireContext()).favoriteDesign
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.home_favorite_dialog, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        NativeTypography.tree(view)
        val owner = NativeViewJobs().also { jobs = it }
        title = view.findViewById(R.id.home_favorite_title); url = view.findViewById(R.id.home_favorite_url)
        view.findViewById<View>(R.id.home_favorite_positive).setOnClickListener { save() }
        view.findViewById<View>(R.id.home_favorite_negative).setOnClickListener { dismiss() }
        val preferences = HomePreferences(requireContext())
        icon = FavoriteIconEditor(requireContext(), FavoriteIconViews.bind(view), design, owner, ::pickImage) {
            preferences.markDirty(html = true, style = false)
        }
        item = HomeFavorite()
        val repository = NativeHomeFavorites.get(requireContext())
        val caption = view.findViewById<TextView>(R.id.home_favorite_caption)
        val id = arguments?.getInt("id", -1) ?: -1
        if (id >= 0) owner.launch({ repository.get(id) }, failed = { NativeLog.error(null, it) }) { loaded(it!!) }
        else {
            val address = arguments?.getString("url")
            if (address.isNullOrEmpty()) caption.setText(R.string.ref_favorite_form_add_to)
            else {
                val label = arguments?.getString("title")
                owner.launch({ repository.find(address) ?: HomeFavorite(title = label, url = address) }, failed = { NativeLog.error(null, it) }) { value ->
                    if (value.id <= 0) caption.setText(R.string.ref_favorite_form_add)
                    loaded(value)
                }
            }
        }
    }

    private fun loaded(value: HomeFavorite) {
        item = value; title?.setText(value.title); url?.setText(value.url); icon?.load(value.url)
    }
    private fun save() {
        if (!input.begin()) return
        val titleField = checkNotNull(title); val urlField = checkNotNull(url)
        val label = titleField.text.toString()
        val raw = urlField.text.toString().trim { it <= ' ' }
        val messages = MessageOwner(requireActivity())
        if (label.isEmpty()) { messages.show(MessageContent(input.required(titleField))); return }
        if (raw.isEmpty()) { messages.show(MessageContent(input.required(urlField))); return }
        item.rawTitle = label; item.rawUrl = HomeFavoriteInput.resolve(requireContext(), raw)
        val request = item
        val repository = NativeHomeFavorites.get(requireContext())
        val iconOwner = checkNotNull(icon)
        jobs?.launch({ repository.save(request).also { if (it > 0) iconOwner.apply(request.url) } }, failed = { NativeLog.error(null, it) }) { result ->
            if (result < 0) messages.show(MessageContent(getString(R.string.ref_bookmark_document_failure)))
            else {
                HomePreferences(requireContext()).markDirty(html = true, style = false)
                requireActivity().supportFragmentManager.setFragmentResult(RESULT, Bundle().apply {
                    putInt("result_id", if (result > 0) result else item.id)
                })
                dismiss()
            }
        }
    }

    private fun pickImage() { try { picker.launch(null) } catch (error: ActivityNotFoundException) { NativeLog.error(null, error) } }
    override fun onResume() { super.onResume(); effects?.close(); effects = dialog?.let(FavoriteDialogWindow::bind) }
    override fun onDestroyView() {
        jobs?.close(); jobs = null; icon = null; title = null; url = null
        effects?.close(); effects = null
        super.onDestroyView()
    }
    companion object {
        const val TAG = "home_favorite_form"
        const val RESULT = "fav_result"
        fun create(url: String?, title: String?, id: Int = -1) = HomeFavoriteDialogFragment().apply {
            arguments = Bundle().apply { putString("url", url); putString("title", title); putInt("id", id) }
        }
    }
}
