package moe.via.browser.feature.find

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.text.Editable
import android.text.TextWatcher
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.theme.window.ChromeLayoutPolicy
import moe.via.browser.ui.theme.palette
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.surface
import moe.via.browser.ui.components.touchFeedback

class FindInPageController(private val activity: Activity, private val parent: FrameLayout) {
    val isShowing get() = bar != null
    private var bar: View? = null
    private var webView: WebView? = null

    fun show(view: WebView, initialText: String? = null) {
        close()
        webView = view
        val input = NativeTypography.edit(activity).apply {
            hint = "页内查找"
            textSize = 14f
            setSingleLine(true)
            background = null
            setPadding(dp(12), 0, dp(4), 0)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setOnEditorActionListener { _, _, _ -> view.findNext(true); true }
        }
        val count = NativeTypography.text(activity).apply { textSize = 11f; setTextColor(activity.palette.secondary); gravity = Gravity.CENTER }
        val row = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = activity.surface()
            elevation = dp(3).toFloat()
        }
        row.addView(input, LinearLayout.LayoutParams(0, -1, 1f))
        row.addView(count, LinearLayout.LayoutParams(activity.dp(44), -1))
        row.addView(button(R.drawable.ref_ic_next, "上一个", 180f) { view.findNext(false) })
        row.addView(button(R.drawable.ref_ic_next, "下一个") { view.findNext(true) })
        row.addView(button(R.drawable.ref_ic_close, "关闭查找") { close() })
        view.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (isDoneCounting) count.text = if (input.text.isEmpty()) "" else "${if (numberOfMatches == 0) 0 else activeMatchOrdinal + 1}/$numberOfMatches"
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { view.findAllAsync(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        parent.addView(row, FrameLayout.LayoutParams(ChromeLayoutPolicy.resolve(activity).popupWidth(activity), activity.dp(48), Gravity.TOP or Gravity.END))
        bar = row
        if (initialText != null) { input.setText(initialText); input.setSelection(input.length()) }
        input.requestFocus()
        input.post { (activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(input, InputMethodManager.SHOW_IMPLICIT) }
    }

    fun close(): Boolean {
        val existing = bar ?: return false
        webView?.apply { setFindListener(null); clearMatches() }
        (activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(existing.windowToken, 0)
        parent.removeView(existing)
        bar = null
        webView = null
        return true
    }

    fun refreshTheme() {
        val row = bar as? LinearLayout ?: return
        row.background = activity.surface()
        for (index in 0 until row.childCount) {
            when (val child = row.getChildAt(index)) {
                is EditText -> { child.setTextColor(activity.palette.text); child.setHintTextColor(activity.palette.hint) }
                is TextView -> child.setTextColor(activity.palette.secondary)
                is ImageView -> { child.imageTintList = android.content.res.ColorStateList.valueOf(activity.palette.icon); child.background = activity.touchFeedback() }
            }
        }
    }

    private fun button(icon: Int, label: String, angle: Float = 0f, action: () -> Unit) = ImageView(activity).apply {
        setImageResource(icon)
        imageTintList = android.content.res.ColorStateList.valueOf(activity.palette.icon)
        contentDescription = label
        rotation = angle
        setPadding(dp(8), dp(13), dp(8), dp(13))
        layoutParams = LinearLayout.LayoutParams(dp(36), -1)
        background = activity.touchFeedback()
        setOnClickListener { action() }
    }
}
