package moe.via.browser.feature.site.ui.data

import android.content.Context
import android.text.format.Formatter
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import moe.via.browser.R
import moe.via.browser.feature.site.data.SharedSiteData
import moe.via.browser.ui.components.DividerLayout
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.theme.palette

internal class SiteDataFooterView(
    context: Context,
    sort: () -> Unit,
    clearSelected: () -> Unit,
) : LinearLayout(context) {
    private val details = NativeTypography.text(context).apply {
        textSize = 14f
        setTextColor(context.palette.secondary)
        setPadding(dp(16), dp(12), dp(16), dp(12))
        visibility = GONE
    }
    private val sortAction = button(R.string.site_data_order_name, sort)
    private val sharedAction = button(R.string.site_data_shared_storage, ::toggleShared)
    private val clearSelectedAction = button(R.string.site_data_clear_selected, clearSelected)

    init {
        orientation = VERTICAL
        addView(details, LayoutParams(-1, -2))
        addView(DividerLayout(context, atBottom = false).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(sortAction)
            addView(View(context), LayoutParams(0, 1, 1f))
            addView(sharedAction)
            addView(clearSelectedAction)
        }, LayoutParams(-1, -2))
    }

    fun render(
        sortBySize: Boolean, selecting: Boolean,
        hasSelection: Boolean, clearing: Boolean, shared: SharedSiteData?,
    ) {
        sortAction.setText(if (sortBySize) R.string.site_data_order_size else R.string.site_data_order_name)
        sortAction.contentDescription = context.getString(if (sortBySize) R.string.site_data_sort_name else R.string.site_data_sort_size)
        sortAction.isEnabled = !clearing
        clearSelectedAction.visibility = if (selecting) VISIBLE else GONE
        clearSelectedAction.isEnabled = !clearing && hasSelection
        sharedAction.visibility = if (selecting) GONE else VISIBLE
        sharedAction.isEnabled = !clearing && shared != null
        if (selecting || shared == null) details.visibility = GONE
        details.text = shared?.let { usage ->
            listOf(
                storageSize(R.string.site_data_http_cache_size, usage.httpCache),
                storageSize(R.string.site_data_local_storage_size, usage.localStorage),
                storageSize(R.string.site_data_session_storage_size, usage.sessionStorage),
            ).joinToString("\n")
        }
        sharedAction.isSelected = details.visibility == VISIBLE
        listOf(sortAction, sharedAction, clearSelectedAction).forEach {
            it.alpha = if (it.isEnabled) 1f else .45f
        }
    }

    private fun toggleShared() {
        details.visibility = if (details.visibility == VISIBLE) GONE else VISIBLE
        sharedAction.isSelected = details.visibility == VISIBLE
    }

    private fun storageSize(label: Int, bytes: Long) = context.getString(label, Formatter.formatShortFileSize(context, bytes))

    private fun button(label: Int, action: () -> Unit) = NativeTypography.text(context).apply {
        setText(label)
        textSize = 14f
        gravity = Gravity.CENTER
        setTextColor(context.palette.text)
        minimumHeight = dp(48)
        setPadding(dp(12), 0, dp(12), 0)
        background = context.touchFeedback()
        setOnClickListener { action() }
        layoutParams = LayoutParams(-2, -2)
    }
}
