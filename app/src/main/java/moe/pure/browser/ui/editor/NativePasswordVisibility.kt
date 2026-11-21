package moe.pure.browser.ui.editor

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import moe.pure.browser.R
import moe.pure.browser.feature.settings.skin.NativeSkinIcons
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

internal object NativePasswordVisibility {
    @SuppressLint("ClickableViewAccessibility")
    fun attach(editor: EditText) {
        val icons = NativeSkinIcons(editor.context)
        val shown = (icons.resolve(R.drawable.ref_ai_eye_on, "ic_eye_on") ?: editor.context.getDrawable(R.drawable.ref_ai_eye_on))?.mutate()?.apply { setTint(editor.context.palette.icon); setBounds(0, 0, editor.dp(22), editor.dp(22)) }
        val hidden = (icons.resolve(R.drawable.ref_ai_eye_off, "ic_eye_off") ?: editor.context.getDrawable(R.drawable.ref_ai_eye_off))?.mutate()?.apply { setTint(editor.context.palette.icon); setBounds(0, 0, editor.dp(22), editor.dp(22)) }
        fun update() { editor.setCompoundDrawablesRelative(null, null, if (editor.inputType and 144 == 144) shown else hidden, null) }
        update(); var touched = false
        editor.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) touched = if (editor.layoutDirection == View.LAYOUT_DIRECTION_RTL)
                event.x <= editor.compoundPaddingLeft else event.x >= editor.measuredWidth - editor.compoundPaddingRight
            else if (event.actionMasked == MotionEvent.ACTION_UP && touched) {
                val cursor = editor.selectionStart
                editor.inputType = if (editor.inputType and 144 == 144) (editor.inputType and -145) or 128 else (editor.inputType and -129) or 144
                update(); editor.setSelection(cursor)
            }
            touched
        }
    }
}

