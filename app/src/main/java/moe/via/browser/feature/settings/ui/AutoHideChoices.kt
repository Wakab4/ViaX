package moe.via.browser.feature.settings.ui

import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

internal class AutoHideChoices(activity: Activity, selected: Int, selectedAction: (Int) -> Unit) : LinearLayout(activity) {
    var onSelected: (Int) -> Unit = selectedAction
    init {
        orientation = HORIZONTAL
        listOf(activity.getString(R.string.settings_show_always), activity.getString(R.string.settings_show_on_swipe), activity.getString(R.string.settings_show_on_tap)).forEachIndexed { index, label ->
            val item = LinearLayout(activity).apply {
                orientation = VERTICAL; gravity = Gravity.CENTER
                setPadding(dp(4), dp(10), dp(4), dp(10)); background = activity.touchFeedback()
                addView(object : View(activity) {
                    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                    override fun onDraw(canvas: Canvas) {
                        val frame = RectF((width - dp(50)) / 2f, 0f, (width + dp(50)) / 2f, dp(90).toFloat())
                        paint.color = if (selected == index) activity.palette.accent else activity.palette.icon; paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(1).toFloat()
                        canvas.drawRoundRect(frame, dp(6).toFloat(), dp(6).toFloat(), paint)
                        if (index < 2) {
                            val inset = dp(if (index == 0) 10 else 6).toFloat()
                            canvas.drawRect(frame.left, frame.top + inset, frame.right, frame.bottom - inset, paint)
                        } else canvas.drawCircle(width / 2f, frame.bottom - dp(10) - dp(10) / 2f, dp(10) / 2f, paint)
                    }
                }, LayoutParams(-1, dp(90)))
                addView(NativeTypography.text(activity).apply { text = label; textSize = 11f; gravity = Gravity.CENTER; setTextColor(if (selected == index) activity.palette.accent else activity.palette.text); if (selected == index) NativeTypography.style(this, android.graphics.Typeface.BOLD); setPadding(0, dp(8), 0, 0) }, LayoutParams(-1, -2))
                setOnClickListener { onSelected(index) }
            }
            addView(item, LayoutParams(0, -2, 1f))
        }
    }
}
