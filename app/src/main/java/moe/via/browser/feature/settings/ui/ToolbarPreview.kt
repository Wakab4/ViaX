package moe.via.browser.feature.settings.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import moe.via.browser.R
import moe.via.browser.feature.settings.ToolbarPreviewState
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.theme.palette
import moe.via.browser.ui.theme.window.ChromeMode

internal class ToolbarPreview(activity: Activity, state: ToolbarPreviewState) : FrameLayout(activity) {
    private val previewPalette = activity.palette
    private val address = when (state.address) {
        1 -> "https://example.com/"
        2 -> "example.com"
        else -> SAMPLE_TITLE
    }

    init {
        val shell = column().apply {
            background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(previewPalette.panel)
                setStroke(dp(2), previewPalette.border)
            }
            scaleX = .8f
            scaleY = .8f
            clipToOutline = true
        }
        val page = View(activity).apply { setBackgroundColor(0x18808080) }
        val pageHeight = LinearLayout.LayoutParams(-1, 0, 1f)
        val mode = ChromeMode.entries[state.layout]
        when (mode) {
            ChromeMode.TRADITIONAL -> {
                shell.addView(addressBar(pill = false), barParams())
                shell.addView(page, pageHeight)
                shell.addView(navigationBar(), barParams())
            }
            ChromeMode.TOP -> {
                shell.addView(combinedBar(), barParams())
                if (state.tabs) shell.addView(tabStrip(), barParams(36))
                shell.addView(page, pageHeight)
            }
            ChromeMode.BOTTOM -> {
                shell.addView(page, pageHeight)
                if (state.tabs) shell.addView(tabStrip(), barParams(36))
                shell.addView(combinedBar(), barParams())
            }
            ChromeMode.DOUBLE_BOTTOM -> {
                shell.addView(page, pageHeight)
                shell.addView(addressBar(pill = true, endInset = 12), barParams())
                shell.addView(navigationBar(), barParams())
            }
        }
        addView(shell, LayoutParams(-1, dp(300), Gravity.CENTER))
        isEnabled = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    private fun addressBar(pill: Boolean, endInset: Int = 0): View {
        val contents = row().apply {
            addView(icon(R.drawable.ref_ic_search), LinearLayout.LayoutParams(dp(40), -1))
            addView(label(address, 16), LinearLayout.LayoutParams(0, -1, 1f))
            addView(icon(R.drawable.ref_ic_menu_reload), LinearLayout.LayoutParams(dp(40), -1))
            if (pill) background = GradientDrawable().apply {
                cornerRadius = dp(18).toFloat()
                setColor(previewPalette.input)
            }
        }
        return FrameLayout(context).apply {
            addView(contents, LayoutParams(-1, if (pill) dp(42) else -1, Gravity.CENTER_VERTICAL).apply {
                if (pill) {
                    marginStart = dp(12)
                    marginEnd = dp(endInset)
                }
            })
        }
    }

    private fun navigationBar() = row().apply {
        addView(icon(R.drawable.ref_ic_back), slot())
        addView(icon(R.drawable.ref_ic_forward), slot())
        addView(icon(R.drawable.ref_ic_home), slot())
        addView(tabButton(), slot())
        addView(icon(R.drawable.ref_ic_menu), slot())
    }

    private fun combinedBar(): View {
        val addressSlot = slot(4f)
        val back = icon(R.drawable.ref_ic_back)
        val forward = icon(R.drawable.ref_ic_forward)
        return object : LinearLayout(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val columns = toolbarColumns(MeasureSpec.getSize(widthMeasureSpec))
                val arrows = columns > 8
                back.visibility = if (arrows) View.VISIBLE else View.GONE
                forward.visibility = back.visibility
                // Set visibility and weights before LinearLayout allocates the current row's width.
                addressSlot.weight = (columns - if (arrows) 5 else 3).coerceAtLeast(1).toFloat()
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        }.apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(addressBar(pill = true), addressSlot)
            addView(back, slot())
            addView(forward, slot())
            addView(icon(R.drawable.ref_ic_home), slot())
            addView(tabButton(), slot())
            addView(icon(R.drawable.ref_ic_menu), slot())
        }
    }

    private fun tabStrip(): View {
        val tabParams = LinearLayout.LayoutParams(dp(160), -1)
        val addParams = LinearLayout.LayoutParams(dp(48), -1)
        val sample = row().apply {
            addView(icon(R.drawable.ref_ic_webpage, size = 17, tint = previewPalette.accent), LinearLayout.LayoutParams(dp(40), -1))
            addView(label(SAMPLE_TITLE, 13), LinearLayout.LayoutParams(0, -1, 1f))
            addView(icon(R.drawable.ref_ic_close, size = 16), LinearLayout.LayoutParams(dp(36), -1))
        }
        return object : LinearLayout(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val width = MeasureSpec.getSize(widthMeasureSpec)
                val buttonWidth = width / toolbarColumns(width).coerceAtLeast(2)
                val available = width - buttonWidth
                addParams.width = buttonWidth
                tabParams.width = (available / 2).coerceIn(dp(100), dp(180)).coerceAtMost(available)
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            }
        }.apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(sample, tabParams)
            addView(View(context), slot())
            addView(icon(R.drawable.ref_ic_add, size = 20), addParams)
        }
    }

    private fun tabButton() = icon(R.drawable.ref_ic_tabs).apply {
        addView(label("1", 11).apply {
            gravity = Gravity.CENTER
            NativeTypography.style(this, Typeface.BOLD)
        }, LayoutParams(-1, -1))
    }

    private fun icon(resource: Int, size: Int = 21, tint: Int = previewPalette.icon) = FrameLayout(context).apply {
        addView(ImageView(context).apply {
            setImageResource(resource)
            imageTintList = ColorStateList.valueOf(tint)
        }, LayoutParams(dp(size), dp(size), Gravity.CENTER))
    }

    private fun label(value: String, size: Int) = NativeTypography.text(context).apply {
        text = value
        textSize = size.toFloat()
        gravity = Gravity.CENTER_VERTICAL
        setSingleLine()
        ellipsize = TextUtils.TruncateAt.END
        setTextColor(previewPalette.text)
    }

    private fun toolbarColumns(width: Int): Int {
        val widthDp = (width / resources.displayMetrics.density).toInt()
        val unit = when {
            widthDp < 420 -> (widthDp / 7).coerceAtLeast(1)
            widthDp > 900 -> 72
            else -> 60
        }
        return widthDp / unit
    }

    private fun row() = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
    private fun column() = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private fun slot(weight: Float = 1f) = LinearLayout.LayoutParams(0, -1, weight)
    private fun barParams(height: Int = 48) = LinearLayout.LayoutParams(-1, dp(height))

    private companion object {
        const val SAMPLE_TITLE = "网页"
    }
}
