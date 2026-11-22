package moe.via.browser.feature.site.ui.common

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.feature.useragent.data.UserAgent
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.theme.palette
import java.util.Locale

/** Reusable Native pickers. They report values and never persist a site configuration. */
class NativeSiteEntryDialogs(private val activity: Activity) : AutoCloseable {
    private val scope = DialogScope()
    private var current: BrowserDialog? = null

    fun choices(title: CharSequence, labels: List<CharSequence>, selected: Int, onSelect: (Int) -> Unit,
        onDelete: (() -> Unit)? = null, positive: Boolean = true) {
        val dialog = BrowserDialog(activity, title).singleChoice(labels, selected, action = onSelect)
        if (positive) dialog.positive(activity.getString(android.R.string.ok))
        onDelete?.let { callback -> dialog.neutral(activity.getString(R.string.ref_site_delete)) { callback(); true } }
        show(dialog)
    }

    fun userAgent(title: CharSequence, choices: List<UserAgent>, selectedId: Int, customValue: String?,
        onSelect: (UserAgent) -> Unit, onDelete: (() -> Unit)? = null, positive: Boolean = true) {
        this.choices(title, choices.map { NativeSiteTexts.userAgentLabel(activity, it) }, choices.indexOfFirst { it.id == selectedId }, { index ->
            val value = choices[index]
            if (value.id != -999) onSelect(value.copy(name = NativeSiteTexts.userAgentLabel(activity, value)))
            else {
                val dialog = BrowserDialog(activity, activity.getString(R.string.ref_site_custom))
                dialog.setCanceledOnTouchOutside(false)
                val input = dialog.field(customValue.orEmpty(), activity.getString(R.string.ref_site_user_agent_title), 3)
                show(dialog.positive(activity.getString(android.R.string.ok)) {
                    val text = input.text.toString()
                    if (text.isNotEmpty()) onSelect(UserAgent(-999, text, text))
                    true
                }.negative(activity.getString(android.R.string.cancel)))
            }
        }, onDelete, positive)
    }

    fun font(title: CharSequence?, current: Int, highlight: Int, onSelect: (Int) -> Unit,
        onDelete: (() -> Unit)? = null, onReset: (() -> Unit)? = null) {
        val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val sample = NativeTypography.text(activity).apply {
            text = NativeSiteEntryDialogs.sample(); gravity = Gravity.CENTER; setTextColor(activity.palette.text)
            setPadding(dp(12), dp(16), dp(12), dp(16))
        }
        column.addView(sample, LinearLayout.LayoutParams(-1, activity.dp(76)))
        val slider = NativeSiteSeekBar(activity).apply {
            max = 30; highlightProgress = (highlight - 50) / 5; minimumHeight = dp(2)
            setPaddingRelative(dp(16), 0, dp(16), 0)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    sample.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                        (activity.resources.displayMetrics.scaledDensity * 14 * (progress * 5 + 50) / 100).toInt().toFloat())
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
            progress = ((current - 50) / 5).coerceIn(0, 30)
        }
        sample.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
            (activity.resources.displayMetrics.scaledDensity * 14 * (slider.progress * 5 + 50) / 100).toInt().toFloat())
        column.addView(slider, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = activity.dp(16)
        })
        val dialog = BrowserDialog(activity, title).custom(column)
            .positive(activity.getString(android.R.string.ok)) { onSelect(slider.progress * 5 + 50); true }
            .negative(activity.getString(android.R.string.cancel))
        if (onDelete != null) dialog.neutral(activity.getString(R.string.ref_site_delete)) { onDelete(); true }
        else if (onReset != null) dialog.neutral(activity.getString(R.string.ref_site_reset)) { onReset(); true }
        show(dialog)
    }

    internal fun domain(description: String, normalized: Boolean, askOption: Boolean = false,
        save: (String, Boolean) -> Unit) {
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_site_add), description)
        val input = dialog.field("", "www.example.com")
        val ask = if (askOption) dialog.checkbox(activity.getString(R.string.ref_site_ask), false) else null
        show(dialog.positive(activity.getString(android.R.string.ok)) {
            NativeSiteKeys.input(input.text.toString(), normalized)?.let { save(it, ask?.isChecked == true) }
            true
        }.negative(activity.getString(android.R.string.cancel)))
    }

    private fun show(dialog: BrowserDialog) {
        current?.dismiss(); current = dialog
        scope.show(dialog) { if (current === dialog) current = null }
    }
    fun back(): Boolean { val dialog = current ?: return false; dialog.dismiss(); return true }
    override fun close() { scope.dispose(); current = null }

    companion object {
        fun sample(): String = if (Locale.getDefault().country.equals("CN", true))
            "莫忧世事兼身事，须著人间比梦间。" else "Less is more."
    }
}
