package moe.pure.browser.feature.readaloud.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** Recycles only paragraph views; selection changes never rebuild or reposition the list. */
internal class ReadAloudParagraphAdapter(private val context: Context) : BaseAdapter() {
    private var paragraphs: List<String> = emptyList()
    var currentIndex = -1
        private set

    fun replace(values: List<String>) {
        paragraphs = values
        notifyDataSetChanged()
    }

    fun select(index: Int, list: ListView) {
        val previous = currentIndex
        currentIndex = index
        for (position in list.firstVisiblePosition..list.lastVisiblePosition) {
            if (position == previous || position == index) {
                (list.getChildAt(position - list.firstVisiblePosition) as? TextView)?.let {
                    style(it, position)
                }
            }
        }
    }

    override fun getCount() = paragraphs.size
    override fun getItem(position: Int) = paragraphs[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val text = convertView as? TextView ?: NativeTypography.text(context).apply {
            layoutParams = android.widget.AbsListView.LayoutParams(-1, -2)
            gravity = Gravity.CENTER
            setSingleLine(false)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setTextSize(TypedValue.COMPLEX_UNIT_PX,
                resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
        }
        text.text = getItem(position)
        style(text, position)
        return text
    }

    private fun style(text: TextView, position: Int) {
        val current = position == currentIndex
        text.isSelected = current
        NativeTypography.style(text, if (current) Typeface.BOLD else Typeface.NORMAL)
        text.setTextColor(if (current) context.palette.accent else context.palette.hint)
    }
}
