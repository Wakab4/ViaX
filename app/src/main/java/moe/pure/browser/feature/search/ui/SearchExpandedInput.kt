package moe.pure.browser.feature.search.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

internal class SearchExpandedInput(
    private val activity: Activity,
    text: String,
    start: Int,
    end: Int,
    private val finished: (String, Int, Int, Boolean) -> Unit
) {
    val view = FrameLayout(activity).apply {
        setBackgroundColor(activity.palette.canvas)
        isClickable = true
    }
    val input: EditText = NativeTypography.edit(activity).apply {
        hint = "搜索"
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
            resources.getDimensionPixelSize(R.dimen.ref_search_input_size).toFloat())
        inputType = 655360
        imeOptions = EditorInfo.IME_ACTION_GO
        maxLines = 999
        setSingleLine(false)
        setHorizontallyScrolling(false)
        textDirection = View.TEXT_DIRECTION_LOCALE
        setSelectAllOnFocus(true)
        gravity = Gravity.TOP
        setTextColor(activity.palette.text)
        setHintTextColor(activity.palette.text)
        setBackgroundColor(Color.TRANSPARENT)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setLineSpacing(lineSpacingExtra, 1.2f)
        isSaveEnabled = false
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
        setText(text)
        val initialStart = start.takeIf { it >= 0 }?.coerceAtMost(length()) ?: length()
        setSelection(initialStart, end.coerceIn(initialStart, length()))
        setOnKeyListener { _, code, event ->
            if (code == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) { finish(true); true } else false
        }
        setOnEditorActionListener { _, action, event ->
            if (action == EditorInfo.IME_ACTION_GO && event == null) { finish(true); true } else false
        }
    }
    val scroll = ScrollView(activity).apply {
        isFillViewport = true
        addView(input, FrameLayout.LayoutParams(-1, -2))
    }

    init {
        view.addView(scroll, FrameLayout.LayoutParams(-1, -1).apply { bottomMargin = activity.dp(48) })
        val controls = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(icon(R.drawable.ref_search_collapse, "折叠文本") { finish(false) }, cell())
        controls.addView(Space(activity), LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(icon(R.drawable.ref_search_go, "搜索") { finish(true) }, cell())
        view.addView(controls, FrameLayout.LayoutParams(-1, activity.dp(48), Gravity.BOTTOM))
    }

    private fun finish(go: Boolean): Unit = finished(input.text.toString(), input.selectionStart, input.selectionEnd, go)

    private fun icon(resource: Int, label: String, action: () -> Unit) = ImageView(activity).apply {
        setImageResource(resource)
        imageTintList = ColorStateList.valueOf(activity.palette.icon)
        contentDescription = label
        setPadding(dp(13), dp(13), dp(13), dp(13))
        setBackgroundResource(R.drawable.ref_search_round_feedback)
        setOnClickListener { action() }
    }

    private fun cell() = LinearLayout.LayoutParams(activity.dp(48), activity.dp(48)).apply {
        setMargins(activity.dp(3), 0, activity.dp(3), 0)
    }
}
