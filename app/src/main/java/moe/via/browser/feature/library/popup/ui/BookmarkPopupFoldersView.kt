package moe.via.browser.feature.library.popup.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.ui.components.SkinIconBinder
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** The original l.xml's two 48dp header rows, with the list below the optional creation row. */
internal class BookmarkPopupFoldersView(context: Context) : RelativeLayout(context), AutoCloseable {
    private val skin = SkinIconBinder(context)
    val up = button(R.id.bookmark_popup_folders_up, R.string.ref_bookmark_popup_up, R.drawable.ref_ic_back)
    val title = label(R.id.bookmark_popup_folders_title, 16f).apply {
        text = context.getString(R.string.ref_bookmark_popup_select_folder)
        typeface = Typeface.create(typeface, Typeface.BOLD)
    }
    val add = button(R.id.bookmark_popup_folders_add, R.string.ref_bookmark_popup_new, R.drawable.ref_ic_add, true)
    val search = button(R.id.bookmark_popup_folders_search, R.string.ref_bookmark_popup_search, R.drawable.ref_ic_search, true)
    val searchInput = input(R.id.bookmark_popup_folders_query, R.string.ref_bookmark_popup_search).apply {
        textSize = 16f; typeface = Typeface.create(typeface, Typeface.BOLD); visibility = GONE
    }
    val createRow = LinearLayout(context).apply {
        id = R.id.bookmark_popup_folders_create_row
        orientation = LinearLayout.HORIZONTAL
        visibility = GONE
    }
    val newFolder = input(R.id.bookmark_popup_folders_name, R.string.ref_bookmark_popup_new)
    val confirm = label(R.id.bookmark_popup_folders_confirm, 14f).apply {
        text = context.getString(android.R.string.ok)
        gravity = Gravity.CENTER
        minWidth = dp(54)
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = feedback(true)
    }
    val list = RecyclerView(context).apply { id = android.R.id.list }
    val empty = label(R.id.bookmark_popup_folders_empty, 16f).apply {
        text = context.getString(R.string.ref_bookmark_popup_empty)
        setTextColor(context.palette.hint)
        visibility = GONE
    }

    init {
        layoutParams = LayoutParams(-1, -1)
        setPadding(0, dp(6), 0, 0)
        addView(up, LayoutParams(dp(54), dp(48)))
        addView(add, LayoutParams(dp(48), dp(48)).apply { addRule(ALIGN_PARENT_END); addRule(ALIGN_TOP, up.id) })
        addView(search, LayoutParams(dp(48), dp(48)).apply { addRule(START_OF, add.id); addRule(ALIGN_TOP, up.id) })
        addView(title, LayoutParams(0, -2).apply {
            addRule(END_OF, up.id); addRule(START_OF, search.id); addRule(ALIGN_TOP, up.id); addRule(ALIGN_BOTTOM, up.id)
        })
        title.gravity = Gravity.CENTER_VERTICAL
        addView(searchInput, LayoutParams(0, -2).apply {
            addRule(END_OF, up.id); addRule(START_OF, add.id); addRule(ALIGN_TOP, up.id); addRule(ALIGN_BOTTOM, up.id)
        })
        searchInput.gravity = Gravity.CENTER_VERTICAL
        addView(createRow, LayoutParams(-1, dp(48)).apply { addRule(BELOW, up.id); bottomMargin = dp(4) })
        createRow.addView(button(View.NO_ID, R.string.ref_bookmark_popup_new, R.drawable.ref_ic_folder_add),
            LinearLayout.LayoutParams(dp(54), dp(48)))
        createRow.addView(newFolder, LinearLayout.LayoutParams(0, -1, 1f).apply { topMargin = dp(4); bottomMargin = dp(4) })
        createRow.addView(confirm, LinearLayout.LayoutParams(-2, -1))
        addView(list, LayoutParams(-1, -1).apply { addRule(BELOW, createRow.id); alignWithParent = true })
        addView(empty, LayoutParams(-2, -2).apply { addRule(CENTER_IN_PARENT) })
    }

    private fun label(value: Int, size: Float) = NativeTypography.text(context).apply {
        id = value; textSize = size
        setTextColor(context.palette.text)
        textDirection = View.TEXT_DIRECTION_LOCALE
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
    }

    private fun input(value: Int, hint: Int) = NativeTypography.edit(context).apply {
        id = value
        setHint(hint); textSize = 14f
        setTextColor(context.palette.text); setHintTextColor(context.palette.hint)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        setSingleLine(true)
        imeOptions = EditorInfo.IME_ACTION_DONE
        textDirection = View.TEXT_DIRECTION_LOCALE
        setPaddingRelative(0, dp(8), 0, dp(8))
        setBackgroundResource(R.drawable.ref_bookmark_form_input)
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
    }

    private fun button(value: Int, description: Int, image: Int, circular: Boolean = false) = ImageView(context).apply {
        id = value; contentDescription = context.getString(description)
        setPadding(dp(13), dp(13), dp(13), dp(13))
        imageTintList = ColorStateList.valueOf(context.palette.icon)
        background = feedback(true, circular)
        skin.image(this, image)
    }

    private fun feedback(rounded: Boolean, circular: Boolean = false) = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed), GradientDrawable().apply {
            shape = if (circular) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
            setColor(0x30808080); cornerRadius = if (rounded) dp(18).toFloat() else 0f
        })
        addState(intArrayOf(), android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
    }

    fun stopAnimations() {
        listOf(up, title, add, search, searchInput, createRow, newFolder, confirm, list, empty).forEach { it.animate().cancel() }
    }

    override fun close() { stopAnimations(); skin.close() }
}
