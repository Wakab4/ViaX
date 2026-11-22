package moe.via.browser.feature.library.ui

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import moe.via.browser.R
import moe.via.browser.ui.theme.skin.NativeSkinIcons
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** r0's editor presentation and y.P clear affordance. Query ownership stays with its caller. */
class NativeLibrarySearch(private val editor: EditText, initiallyEnabled: Boolean = true) : AutoCloseable {
    private val context = editor.context
    private val originalBackground = editor.background
    private val originalPadding = intArrayOf(editor.paddingStart, editor.paddingTop, editor.paddingEnd, editor.paddingBottom)
    private val originalInput = editor.inputType
    private val originalIme = editor.imeOptions
    private val originalDirection = editor.textDirection
    private val originalDrawables = editor.compoundDrawablesRelative
    private val originalDrawablePadding = editor.compoundDrawablePadding
    private val originalHint = editor.hint
    private val originalTextSize = editor.textSize
    private val originalColors = editor.textColors
    private val originalHintColors = editor.hintTextColors
    private val originalMaxLines = editor.maxLines
    private val originalMinLines = editor.minLines
    private val originalTransformation = editor.transformationMethod
    private val originalLocaleLineHeight = if (Build.VERSION.SDK_INT >= 35) editor.isLocalePreferredLineHeightForMinimumUsed else null
    private val originalHeight = editor.layoutParams?.height
    private val originalMargins = (editor.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
        intArrayOf(it.marginStart, it.topMargin, it.marginEnd, it.bottomMargin)
    }
    private var native = false
    private var closed = false
    private var clearPressed = false
    private var clear: Drawable? = null
    private val skins = NativeSkinIcons(context)
    private val watcher = object : TextWatcher {
        override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(value: Editable?) { if (native && !closed) showIcon() }
    }
    init { editor.addTextChangedListener(watcher); enabled(initiallyEnabled) }

    fun enabled(value: Boolean) {
        if (closed || value == native) return
        native = value; clearPressed = false
        if (value) {
            editor.hint = context.getString(R.string.ref_closed_tabs_search)
            editor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            editor.setTextColor(context.palette.text); editor.setHintTextColor(context.palette.hint)
            editor.inputType = InputType.TYPE_CLASS_TEXT
            editor.setSingleLine(true)
            editor.imeOptions = EditorInfo.IME_ACTION_DONE
            editor.textDirection = View.TEXT_DIRECTION_LOCALE
            if (Build.VERSION.SDK_INT >= 35) editor.setLocalePreferredLineHeightForMinimumUsed(false)
            editor.background = GradientDrawable().apply { setColor(0x20808080); cornerRadius = context.dp(18).toFloat() }
            editor.setPaddingRelative(context.dp(16), context.dp(10), context.dp(16), context.dp(10))
            editor.compoundDrawablePadding = context.dp(12)
            resize(ViewGroup.LayoutParams.WRAP_CONTENT, intArrayOf(context.dp(14), context.dp(10), context.dp(14), context.dp(10)))
            updateIcon()
            editor.setOnTouchListener { _, event -> clearTouch(event) }
        } else {
            editor.background = originalBackground
            editor.setSingleLine(originalMaxLines == 1)
            editor.inputType = originalInput; editor.maxLines = originalMaxLines; editor.minLines = originalMinLines
            editor.transformationMethod = originalTransformation
            editor.imeOptions = originalIme; editor.textDirection = originalDirection
            editor.hint = originalHint
            editor.setTextSize(TypedValue.COMPLEX_UNIT_PX, originalTextSize)
            editor.setTextColor(originalColors); editor.setHintTextColor(originalHintColors)
            if (Build.VERSION.SDK_INT >= 35 && originalLocaleLineHeight != null) editor.setLocalePreferredLineHeightForMinimumUsed(originalLocaleLineHeight)
            editor.setPaddingRelative(originalPadding[0], originalPadding[1], originalPadding[2], originalPadding[3])
            editor.compoundDrawablePadding = originalDrawablePadding
            editor.setCompoundDrawablesRelative(originalDrawables[0], originalDrawables[1], originalDrawables[2], originalDrawables[3])
            editor.setOnTouchListener(null)
            originalHeight?.let { resize(it, originalMargins) }
        }
    }
    private fun resize(height: Int, margins: IntArray?) {
        val params = editor.layoutParams ?: return
        params.height = height
        if (params is ViewGroup.MarginLayoutParams && margins != null) {
            params.marginStart = margins[0]; params.topMargin = margins[1]; params.marginEnd = margins[2]; params.bottomMargin = margins[3]
        }
        editor.layoutParams = params
    }
    private fun updateIcon() {
        clear = (skins.resolve(R.drawable.ref_ic_close) ?: context.getDrawable(R.drawable.ref_ic_close))?.mutate()?.apply {
            setTint(context.palette.icon); setBounds(0, 0, context.dp(18), context.dp(18))
        }
        showIcon()
    }
    private fun showIcon() { editor.setCompoundDrawablesRelative(null, null, if (editor.text.isNullOrEmpty()) null else clear, null) }
    private fun clearTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> clearPressed = !editor.text.isNullOrEmpty() &&
                if (editor.layoutDirection == View.LAYOUT_DIRECTION_RTL) event.x <= editor.compoundPaddingLeft
                else event.x >= editor.measuredWidth - editor.compoundPaddingRight
            MotionEvent.ACTION_UP -> if (clearPressed) editor.setText("")
        }
        return clearPressed
    }
    fun refresh() { if (!closed && native) updateIcon() }
    override fun close() {
        if (closed) return
        closed = true; clearPressed = false
        editor.removeTextChangedListener(watcher); editor.setOnTouchListener(null); clear = null
    }
}
