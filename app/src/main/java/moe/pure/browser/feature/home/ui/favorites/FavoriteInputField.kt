package moe.pure.browser.feature.home.ui.favorites

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** The original EditTexts use ordinary IME next/done; neither action submits the form itself. */
internal object FavoriteInputField {
    fun create(context: Context, address: Boolean): EditText = NativeTypography.edit(context).apply {
        id = if (address) R.id.home_favorite_url else R.id.home_favorite_title
        hint = context.getString(if (address) R.string.ref_bookmark_form_url_hint else R.string.ref_bookmark_form_title_hint)
        textSize = 14f; setTextColor(context.palette.text); setHintTextColor(context.palette.secondary)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        imeOptions = if (address) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
        maxLines = 1; setLines(1)
        textDirection = View.TEXT_DIRECTION_LOCALE
        setBackgroundResource(R.drawable.dialog_field_underline); setPadding(0, context.dp(8), 0, context.dp(8))
    }
}
