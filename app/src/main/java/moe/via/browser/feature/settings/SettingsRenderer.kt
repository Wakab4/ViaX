package moe.via.browser.feature.settings

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.ui.settings.NativeSettingViews

/** Values/actions and shared a6 widgets are separate from the list container. */
class SettingsRenderer(private val activity: Activity) {
    internal fun kind(row: SettingRow): Int = when (row) {
        is SettingRow.Link -> 0
        is SettingRow.Toggle -> 1
        is SettingRow.Radio -> 2
        is SettingRow.Section -> 3
        is SettingRow.Custom -> 4
    }
    internal fun empty(kind: Int): View = when (kind) {
        0 -> NativeSettingViews.site(activity)
        1 -> NativeSettingViews.Global(activity)
        2 -> NativeSettingViews.Radio(activity)
        3 -> NativeSettingViews.section(activity)
        else -> android.widget.FrameLayout(activity)
    }
    fun create(row: SettingRow): View = if (row is SettingRow.Custom) row.create()
        else empty(kind(row)).also { bind(it, row); clicks(it, row) }
    fun render(parent: LinearLayout, rows: List<SettingRow>) {
        parent.removeAllViews()
        rows.forEach { parent.addView(create(it), LinearLayout.LayoutParams(-1, -2)) }
    }
    internal fun bind(view: View, row: SettingRow) {
        when (row) {
            is SettingRow.Link -> (view as LinearLayout).apply {
                if (row.highlighted) setBackgroundColor(0x30808080) else setBackgroundResource(moe.via.browser.R.drawable.ref_redirect_page_feedback)
                (getChildAt(0) as TextView).apply { text = row.title; alpha = if (row.enabled) 1f else .5f }
                (getChildAt(1) as TextView).apply {
                    text = row.detail; visibility = if (row.detail.isEmpty()) View.GONE else View.VISIBLE
                    alpha = if (row.enabled) 1f else .5f
                }
                isEnabled = row.enabled; alpha = 1f
            }
            is SettingRow.Toggle -> (view as NativeSettingViews.Global).apply {
                title.text = row.title; detail.text = row.detail
                detail.visibility = if (row.detail.isEmpty()) View.GONE else View.VISIBLE
                check.isChecked = row.checked
                val opacity = if (row.enabled) 1f else .5f
                title.alpha = opacity; detail.alpha = opacity; check.alpha = opacity
                isEnabled = row.enabled; alpha = 1f
            }
            is SettingRow.Radio -> (view as NativeSettingViews.Radio).apply {
                title.text = row.title; title.alpha = if (row.dimmed) .5f else 1f
                moe.via.browser.ui.theme.fonts.NativeTypography.style(title, android.graphics.Typeface.NORMAL)
                row.typeface?.let { title.typeface = it }
                detail.text = row.detail
                detail.visibility = if (row.detail.isEmpty()) View.GONE else View.VISIBLE
                check.isChecked = row.checked
                isEnabled = row.enabled; alpha = if (row.enabled) 1f else .5f
            }
            is SettingRow.Section -> (view as TextView).text = row.title
            is SettingRow.Custom -> (view as android.widget.FrameLayout).apply {
                removeAllViews(); addView(row.create(), android.widget.FrameLayout.LayoutParams(-1, -2))
            }
        }
    }
    internal fun activate(view: View, row: SettingRow) {
        when (row) {
            is SettingRow.Link -> row.onClick()
            is SettingRow.Radio -> row.onClick()
            is SettingRow.Toggle -> (view as NativeSettingViews.Global).check.let {
                it.isChecked = !it.isChecked; row.onChanged(it.isChecked)
            }
            else -> Unit
        }
    }
    internal fun longClick(row: SettingRow): Boolean {
        val action = when (row) { is SettingRow.Link -> row.onLongClick; is SettingRow.Radio -> row.onLongClick; else -> null }
        action?.invoke(); return action != null
    }
    private fun clicks(view: View, row: SettingRow) {
        if (row is SettingRow.Section || row is SettingRow.Custom) return
        var last: Long? = null
        view.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            if (last == null || kotlin.math.abs(now - last!!) > 300) { last = now; activate(it, row) }
        }
        view.setOnLongClickListener { longClick(row) }
    }
}
