package moe.pure.browser.ui.dialog

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.ui.theme.palette

enum class DialogChoiceMode { ACTION, SINGLE, MULTIPLE, HIGHLIGHT_SINGLE, HIGHLIGHT_MULTIPLE }

class DialogChoices(private val activity: Activity, private val labels: List<CharSequence>,
    val mode: DialogChoiceMode, selected: Set<Int> = emptySet(), private val accent: Boolean = false) : BaseAdapter() {
    private val checked = selected.filterTo(mutableSetOf()) { it in labels.indices }
    val selection get() = checked.toSortedSet()
    val multiple get() = mode == DialogChoiceMode.MULTIPLE || mode == DialogChoiceMode.HIGHLIGHT_MULTIPLE
    private val indicators get() = mode == DialogChoiceMode.SINGLE || mode == DialogChoiceMode.MULTIPLE
    fun select(index: Int) {
        if (mode == DialogChoiceMode.ACTION) return
        if (multiple) { if (!checked.add(index)) checked.remove(index) }
        else { checked.clear(); checked.add(index) }
        notifyDataSetChanged()
    }
    override fun getCount() = labels.size
    override fun getItem(position: Int) = labels[position]
    override fun getItemId(position: Int) = position.toLong()
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(activity).inflate(if (indicators) R.layout.dialog_choice else R.layout.dialog_item, parent, false)
            .also { NativeTypography.tree(it) }
        val text = if (indicators) view.findViewById<CheckBox>(R.id.dialog_choice_text) else view as TextView
        text.text = labels[position]
        text.setTextColor(if (accent && mode == DialogChoiceMode.ACTION) activity.palette.accent else activity.palette.text)
        if (indicators) (text as CheckBox).isChecked = position in checked
        if (mode != DialogChoiceMode.ACTION) {
            text.isEnabled = false; text.isFocusable = false; text.isClickable = false
        }
        if (mode == DialogChoiceMode.HIGHLIGHT_SINGLE || mode == DialogChoiceMode.HIGHLIGHT_MULTIPLE) {
            text.gravity = Gravity.CENTER
            text.setBackgroundColor(if (position in checked) 0x30808080 else 0)
        }
        return view
    }
}
