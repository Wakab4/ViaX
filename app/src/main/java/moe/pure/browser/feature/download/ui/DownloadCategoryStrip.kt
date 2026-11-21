package moe.pure.browser.feature.download.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.RadioButton
import android.widget.RadioGroup
import moe.pure.browser.R
import moe.pure.browser.feature.download.DownloadCategory
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** com.tuyafeng.support.widget.b: no radio glyph, 11sp labels, original outlined chips. */
internal class DownloadCategoryStrip(context: Context, initial: DownloadCategory,
    changed: (DownloadCategory) -> Unit) : HorizontalScrollView(context) {
    private val group = RadioGroup(context).apply {
        id = R.id.download_manager_category_group
        orientation = RadioGroup.HORIZONTAL
        setPadding(dp(12), dp(2), dp(12), dp(2))
    }
    private var binding = false

    init {
        id = R.id.download_manager_categories
        isHorizontalScrollBarEnabled = false; isVerticalScrollBarEnabled = false
        addView(group, LayoutParams(-1, -1))
        for (category in DownloadCategory.entries) group.addView(RadioButton(context).apply {
            id = categoryId(category); tag = category
            setText(category.titleRes); textSize = 11f
            setSingleLine(true); maxLines = 1; buttonDrawable = null
            gravity = Gravity.CENTER; setPadding(dp(8), 0, dp(8), 0)
            minWidth = dp(48); minHeight = dp(28)
            setTextColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_selected),
                intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked), intArrayOf()),
                intArrayOf(context.palette.accent, context.palette.accent, context.palette.secondary)))
            background = backgroundShape()
            textDirection = View.TEXT_DIRECTION_LOCALE
            if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
            isChecked = category == initial
        }, RadioGroup.LayoutParams(-2, -2).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) })
        group.setOnCheckedChangeListener { parent, id ->
            if (!binding) (parent.findViewById<RadioButton>(id)?.tag as? DownloadCategory)?.let(changed)
        }
    }

    private fun categoryId(category: DownloadCategory) = when (category) {
        DownloadCategory.ALL -> R.id.download_category_all
        DownloadCategory.DOCUMENT -> R.id.download_category_document
        DownloadCategory.ARCHIVE -> R.id.download_category_archive
        DownloadCategory.APK -> R.id.download_category_apk
        DownloadCategory.IMAGE -> R.id.download_category_image
        DownloadCategory.VIDEO -> R.id.download_category_video
        DownloadCategory.AUDIO -> R.id.download_category_audio
        DownloadCategory.OTHER -> R.id.download_category_other
    }

    fun render(category: DownloadCategory, empty: Boolean) {
        binding = true
        for (index in 0 until group.childCount) (group.getChildAt(index) as RadioButton).isChecked = group.getChildAt(index).tag == category
        binding = false
        // Only the child group disappears; the 36dp scroll-strip remains in the layout.
        group.visibility = if (empty && category == DownloadCategory.ALL) GONE else VISIBLE
    }

    private fun backgroundShape(): StateListDrawable {
        fun shape(fill: Int, stroke: Int = Color.TRANSPARENT, width: Int = 0) = GradientDrawable().apply {
            setColor(fill); cornerRadius = dp(18).toFloat(); if (width > 0) setStroke(width, stroke)
        }
        return StateListDrawable().apply {
            addState(intArrayOf(-android.R.attr.state_pressed, -android.R.attr.state_selected, -android.R.attr.state_checked,
                -android.R.attr.state_focused), shape(Color.TRANSPARENT, 0x30808080, dp(1)))
            addState(intArrayOf(-android.R.attr.state_pressed, android.R.attr.state_checked, -android.R.attr.state_focused),
                shape(Color.TRANSPARENT, context.palette.accent, dp(1)))
            addState(intArrayOf(-android.R.attr.state_pressed, android.R.attr.state_selected, -android.R.attr.state_focused),
                shape(Color.TRANSPARENT, context.palette.accent, dp(1)))
            addState(intArrayOf(android.R.attr.state_pressed), shape(0x30808080))
            addState(intArrayOf(android.R.attr.state_focused), shape(Color.TRANSPARENT, 0x60808080, dp(3)))
        }
    }
}
