package moe.via.browser.feature.search.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.feature.search.model.SearchEngine
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

internal class SearchInputBar(
    private val activity: Activity,
    incognito: Boolean,
    private val changed: (String) -> Unit,
    chooseEngine: (View) -> Unit,
    clear: () -> Unit,
    expand: () -> Unit,
    submit: () -> Unit,
    clearEngine: () -> Unit
) : LinearLayout(activity) {
    val input = SearchAddressEditText(activity)
    private val badge = NativeTypography.text(activity)
    private val clearButton = icon(R.drawable.ref_search_clear, "清除文本", clear)
    private val engineButton = icon(R.drawable.ref_search_search, "搜索引擎") { chooseEngine(getChildAt(0)) }
    private val expandButton = icon(R.drawable.ref_search_expand, "展开文本", expand)
    private val goButton = icon(R.drawable.ref_search_go, "搜索", submit)
    private var muted = false
    private var textColor = activity.palette.text

    init {
        id = View.generateViewId()
        gravity = Gravity.CENTER_VERTICAL
        orientation = HORIZONTAL
        addView(engineButton, LayoutParams(dp(48), dp(48)))
        badge.apply {
            gravity = Gravity.CENTER
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimensionPixelSize(R.dimen.ref_search_secondary_size).toFloat())
            setPadding(dp(6), dp(2), dp(6), dp(2))
            visibility = View.GONE
        }
        addView(badge, LayoutParams(-2, -2).apply { marginStart = dp(4) })
        input.apply {
            inputType = 524289
            imeOptions = EditorInfo.IME_ACTION_GO or if (incognito) 17301504 else 0
            hint = "搜索"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimensionPixelSize(R.dimen.ref_search_input_size).toFloat())
            gravity = Gravity.CENTER_VERTICAL
            setSingleLine(true)
            textDirection = View.TEXT_DIRECTION_LOCALE
            setSelectAllOnFocus(true)
            isSaveEnabled = false
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            setBackgroundColor(Color.TRANSPARENT)
            if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
            deleteAtStart = clearEngine
            setOnKeyListener { _, code, event ->
                when {
                    code == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN &&
                        selectionStart == 0 && selectionEnd == 0 -> { clearEngine(); true }
                    code == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP -> { submit(); true }
                    else -> false
                }
            }
            setOnEditorActionListener { _, action, event ->
                if (action == EditorInfo.IME_ACTION_GO && event == null) { submit(); true } else false
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(text: Editable?) {
                    clearButton.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
                    if (!muted) changed(text?.toString().orEmpty())
                }
            })
        }
        addView(HorizontalScrollView(activity).apply {
            isFillViewport = true
            isHorizontalScrollBarEnabled = false
            isHorizontalFadingEdgeEnabled = true
            setFadingEdgeLength(dp(12))
            addView(input, FrameLayout.LayoutParams(-2, dp(48), Gravity.CENTER_VERTICAL))
        }, LayoutParams(0, dp(48), 1f))
        addView(clearButton, LayoutParams(dp(48), dp(48)))
        addView(expandButton, LayoutParams(dp(48), dp(48)))
        addView(goButton, LayoutParams(dp(48), dp(48)))
        applyColors(null)
    }

    fun setText(value: String, selectAll: Boolean = false, notify: Boolean = true) {
        muted = !notify
        try {
            input.setText(value)
            if (selectAll) input.selectAll() else input.setSelection(input.length())
        } finally { muted = false }
    }

    fun setEngine(engine: SearchEngine?) {
        badge.visibility = if (engine == null) View.GONE else View.VISIBLE
        badge.text = engine?.name?.let { if (it.length > 16) it.take(16) + "..." else it }
    }

    fun applyColors(background: Int?) {
        val custom = background != null
        val light = background?.let { Color.red(it) * .299 + Color.green(it) * .587 + Color.blue(it) * .114 >= 192 } ?: false
        textColor = if (custom) { if (light) 0xDE000000.toInt() else Color.WHITE } else activity.palette.text
        val iconColor = if (custom) { if (light) 0xFF232323.toInt() else Color.WHITE } else activity.palette.icon
        input.setTextColor(textColor)
        input.setHintTextColor(textColor)
        badge.setTextColor(textColor)
        badge.background = GradientDrawable().apply {
            cornerRadius = dp(4).toFloat()
            setColor(Color.TRANSPARENT)
            setStroke(dp(1), textColor)
        }
        listOf(engineButton, clearButton, expandButton, goButton).forEach {
            it.imageTintList = ColorStateList.valueOf(iconColor)
        }
    }

    private fun icon(resource: Int, label: String, action: () -> Unit) = ImageView(activity).apply {
        setImageResource(resource)
        contentDescription = label
        setPadding(dp(13), dp(13), dp(13), dp(13))
        setBackgroundResource(R.drawable.ref_search_round_feedback)
        isFocusable = true
        setOnClickListener { action() }
    }
}
