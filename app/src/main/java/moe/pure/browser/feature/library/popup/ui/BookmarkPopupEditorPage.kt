package moe.pure.browser.feature.library.popup.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.animation.ObjectAnimator
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import moe.pure.browser.R
import moe.pure.browser.feature.library.popup.BookmarkPopupModel
import moe.pure.browser.feature.library.popup.BookmarkPopupState
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.BrowserMessage
import moe.pure.browser.ui.dialog.DialogField
import moe.pure.browser.ui.theme.palette

/** b8.h/layout f: editor and directory picker share the measured editor height. */
class BookmarkPopupEditorPage : Fragment() {
    private val model get() = ViewModelProvider(requireParentFragment())[BookmarkPopupModel::class.java]
    private var column: LinearLayout? = null
    private lateinit var heading: TextView
    private lateinit var title: EditText
    private lateinit var address: EditText
    private lateinit var folder: TextView
    private lateinit var favorite: CheckBox
    private lateinit var delete: TextView
    private var binding = false
    private var messages: BrowserMessage? = null
    private var shake: ObjectAnimator? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
            layoutParams = ViewGroup.LayoutParams(-1, -1)
        }
        heading = NativeTypography.text(context).apply {
            textSize = 16f; setTypeface(typeface, Typeface.BOLD); setTextColor(context.palette.text)
            setSingleLine(); ellipsize = TextUtils.TruncateAt.END; textDirection = View.TEXT_DIRECTION_LOCALE
            setPadding(0, dp(8), 0, dp(8))
            setText(R.string.ref_bookmark_popup_add)
        }
        root.addView(heading, LinearLayout.LayoutParams(-1, -2).apply { setMargins(context.dp(16), 0, context.dp(16), context.dp(6)) })
        fun field(hint: Int, id: Int, ime: Int): EditText {
            val edit = DialogField.append(requireActivity(), root, "", getString(hint), 1).apply {
                this.id = id; imeOptions = ime; textDirection = View.TEXT_DIRECTION_LOCALE
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setSelectAllOnFocus(false); ellipsize = null
            }
            val wrapper = root.getChildAt(root.childCount - 1)
            wrapper.layoutParams = (wrapper.layoutParams as LinearLayout.LayoutParams).apply { bottomMargin = context.dp(16) }
            return edit
        }
        title = field(R.string.ref_bookmark_popup_title_hint, R.id.bookmark_popup_title, EditorInfo.IME_ACTION_NEXT)
        address = field(R.string.ref_bookmark_popup_url_hint, R.id.bookmark_popup_url, EditorInfo.IME_ACTION_DONE)
        folder = NativeTypography.text(context).apply {
            textSize = 14f; setTextColor(context.palette.text); setHintTextColor(context.palette.secondary)
            setHint(R.string.ref_bookmark_popup_folder_hint)
            setSingleLine(); ellipsize = TextUtils.TruncateAt.END; textDirection = View.TEXT_DIRECTION_LOCALE
            setPadding(0, dp(8), 0, dp(8)); setBackgroundResource(R.drawable.ref_bookmark_popup_folder)
            isFocusable = true; setOnClickListener { model.showFolders() }
        }
        root.addView(folder, LinearLayout.LayoutParams(-1, -2).apply { setMargins(context.dp(16), 0, context.dp(16), context.dp(16)) })
        favorite = CheckBox(context).apply {
            id = R.id.bookmark_popup_favorite
            textSize = 12f; setTextColor(context.palette.secondary); setText(R.string.ref_bookmark_popup_favorite)
            textDirection = View.TEXT_DIRECTION_LOCALE
            minimumWidth = 0; minimumHeight = 0; setPadding(dp(8), dp(8), dp(8), dp(8))
            buttonDrawable = context.getDrawable(R.drawable.dialog_checkbox); buttonTintList = null
        }
        root.addView(favorite, LinearLayout.LayoutParams(-1, -2).apply { setMargins(context.dp(16), 0, context.dp(16), context.dp(12)) })
        val footer = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        delete = button(R.string.ref_bookmark_popup_delete).apply {
            setTextColor(0xFFD64146.toInt()); visibility = View.GONE; setOnClickListener { model.delete() }
        }
        footer.addView(delete)
        footer.addView(android.widget.Space(context), LinearLayout.LayoutParams(0, 0, 1f))
        footer.addView(button(android.R.string.cancel).apply { setOnClickListener { model.cancel() } })
        footer.addView(button(android.R.string.ok).apply { setOnClickListener { save() } })
        root.addView(footer, LinearLayout.LayoutParams(-1, -2))
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { draft() }
            override fun afterTextChanged(s: Editable?) = Unit
        }
        title.addTextChangedListener(watcher); address.addTextChangedListener(watcher)
        favorite.setOnCheckedChangeListener { _, _ -> draft() }
        return root.also { column = it }
    }

    private fun button(label: Int) = NativeTypography.text(requireContext()).apply {
        textSize = 14f; setTextColor(context.palette.accent); setText(label)
        gravity = Gravity.CENTER; setSingleLine(); ellipsize = TextUtils.TruncateAt.END
        minWidth = dp(56); setPadding(dp(16), dp(16), dp(16), dp(16))
        setBackgroundResource(R.drawable.dialog_button_feedback)
        layoutParams = LinearLayout.LayoutParams(-2, -2)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.state.observe(viewLifecycleOwner, ::render)
        messages = BrowserMessage(requireActivity())
        view.post { measureEditor() }
    }

    private fun render(state: BookmarkPopupState) {
        if (!state.loaded || column == null) return
        binding = true
        if (title.text.toString() != state.draftTitle) title.setText(state.draftTitle)
        if (address.text.toString() != state.draftUrl) address.setText(state.draftUrl)
        favorite.isChecked = state.favorite
        val existing = state.entry?.id?.isNotEmpty() == true
        heading.setText(if (existing) R.string.ref_bookmark_popup_edit else R.string.ref_bookmark_popup_add)
        delete.visibility = if (existing) View.VISIBLE else View.GONE
        folder.text = state.currentFolder?.let {
            if (it.id.isEmpty()) getString(R.string.ref_bookmark_popup_root) else it.title
        }.orEmpty()
        binding = false
        column?.post { measureEditor() }
    }

    private fun draft() {
        if (!binding && column != null) model.setDraft(title.text.toString(), address.text.toString(), favorite.isChecked)
    }

    private fun save() {
        val name = title.text.toString().trim { it <= ' ' }
        val url = address.text.toString().trim { it <= ' ' }
        val empty = if (name.isEmpty()) title else if (url.isEmpty()) address else null
        if (empty != null) {
            shake?.cancel()
            shake = ObjectAnimator.ofFloat(empty, "translationX", 0f, empty.dp(24).toFloat(), 0f).apply {
                repeatCount = 2; repeatMode = android.animation.ValueAnimator.RESTART; duration = 280L
                interpolator = android.view.animation.PathInterpolator(.2f, .2f, .8f, .8f)
                start()
            }
            messages?.show(getString(R.string.ref_bookmark_form_required, empty.hint))
            return
        }
        model.save(url, name, favorite.isChecked)
    }

    private fun measureEditor() {
        val root = column ?: return
        if (root.width <= 0) return
        root.measure(View.MeasureSpec.makeMeasureSpec(root.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        model.setHeight(root.measuredHeight)
    }

    override fun onDestroyView() {
        shake?.cancel(); shake = null
        messages?.close(); messages = null; column = null
        super.onDestroyView()
    }
}
