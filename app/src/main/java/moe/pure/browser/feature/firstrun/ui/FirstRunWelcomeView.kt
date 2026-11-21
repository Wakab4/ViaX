package moe.pure.browser.feature.firstrun.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.feature.firstrun.FirstRunDocument
import moe.pure.browser.ui.components.applySafeArea
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** Native p.xml proportions; the short body identifies the owner of the reference documents. */
internal class FirstRunWelcomeView(
    private val activity: Activity,
    openDocument: (FirstRunDocument) -> Unit,
    agree: () -> Unit,
    disagree: () -> Unit
) {
    val root = ScrollView(activity).apply {
        isFillViewport = true
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        setBackgroundColor(activity.palette.canvas)
        applySafeArea()
    }
    private val content = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private val heading = text(R.string.ref_first_run_welcome, 22f)
    private val summary = text(0, 14f).apply {
        gravity = Gravity.CENTER
        setLineSpacing(0f, 1.2f)
        movementMethod = LinkMovementMethod.getInstance()
        linksClickable = true
    }
    private val agreeButton = text(R.string.ref_first_run_agree, 14f).apply {
        gravity = Gravity.CENTER
        minWidth = activity.dp(216)
        setSingleLine()
        ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(activity.dp(16), activity.dp(12), activity.dp(16), activity.dp(12))
        setTextColor(Color.WHITE)
        val pressed = if (activity.palette.night) 0xFF48598B.toInt() else 0xFF5B73B5.toInt()
        background = states(activity.palette.accent, pressed)
        setOnClickListener { agree() }
    }
    private val disagreeButton = text(R.string.ref_first_run_disagree, 14f).apply {
        gravity = Gravity.CENTER
        minWidth = activity.dp(80)
        setPadding(activity.dp(16), activity.dp(8), activity.dp(16), activity.dp(8))
        setTextColor(activity.palette.secondary)
        background = states(Color.TRANSPARENT, 0x30808080)
        setOnClickListener { disagree() }
    }

    init {
        root.addView(content, android.widget.FrameLayout.LayoutParams(-1, -2))
        content.addView(Space(activity), LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(ImageView(activity).apply {
            setImageResource(R.drawable.ref_first_run_logo)
            contentDescription = activity.getString(R.string.ref_first_run_logo_description)
        }, centered(activity.dp(120), activity.dp(120)))
        content.addView(Space(activity), LinearLayout.LayoutParams(-1, 0, 1f))
        content.addView(heading, centered(-2, -2))
        content.addView(summary, centered(activity.dp(260), -2).apply {
            topMargin = activity.dp(16)
            bottomMargin = activity.dp(16)
        })
        content.addView(agreeButton, centered(-2, -2).apply { bottomMargin = activity.dp(4) })
        content.addView(disagreeButton, centered(-2, -2).apply { bottomMargin = activity.dp(16) })
        heading.nextFocusDownId = summary.id
        summary.nextFocusUpId = heading.id
        summary.nextFocusDownId = agreeButton.id
        agreeButton.nextFocusUpId = summary.id
        agreeButton.nextFocusDownId = disagreeButton.id
        disagreeButton.nextFocusUpId = agreeButton.id
        summary.setOnKeyListener { view, keyCode, event ->
            val before = summary.selectionEnd
            when (event.action) {
                KeyEvent.ACTION_DOWN -> view.onKeyDown(keyCode, event)
                KeyEvent.ACTION_UP -> view.onKeyUp(keyCode, event)
            }
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN && before == summary.selectionEnd) {
                agreeButton.requestFocus()
            }
            true
        }
        val terms = activity.getString(FirstRunDocument.TERMS.title)
        val privacy = activity.getString(FirstRunDocument.PRIVACY.title)
        val body = activity.getString(R.string.ref_first_run_reference_summary, activity.getString(R.string.app_name), terms, privacy)
        summary.text = SpannableString(body).apply {
            fun link(label: String, document: FirstRunDocument) {
                val start = body.indexOf(label)
                if (start < 0) return
                setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) = openDocument(document)
                    override fun updateDrawState(paint: TextPaint) {
                        paint.color = activity.palette.accent
                        paint.isUnderlineText = true
                    }
                }, start, start + label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            link(terms, FirstRunDocument.TERMS)
            link(privacy, FirstRunDocument.PRIVACY)
        }
    }

    fun accepting() { agreeButton.isEnabled = false }

    private fun text(resource: Int, size: Float) = NativeTypography.text(activity).apply {
        id = View.generateViewId()
        textSize = size
        textDirection = View.TEXT_DIRECTION_LOCALE
        setTextColor(activity.palette.text)
        if (resource != 0) setText(resource)
    }

    private fun centered(width: Int, height: Int) = LinearLayout.LayoutParams(width, height).apply {
        gravity = Gravity.CENTER_HORIZONTAL
    }

    private fun states(normal: Int, pressed: Int) = StateListDrawable().apply {
        fun shape(color: Int) = GradientDrawable().apply {
            setColor(color)
            cornerRadius = activity.dp(18).toFloat()
        }
        addState(intArrayOf(android.R.attr.state_pressed), shape(pressed))
        addState(intArrayOf(android.R.attr.state_focused), shape(pressed))
        addState(intArrayOf(), shape(normal))
    }
}
