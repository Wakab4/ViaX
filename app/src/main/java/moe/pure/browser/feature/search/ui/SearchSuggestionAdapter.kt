package moe.pure.browser.feature.search.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.content.res.ColorStateList
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.feature.search.model.SearchSuggestion
import moe.pure.browser.feature.search.model.SearchSuggestionType
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

internal class SearchSuggestionAdapter(
    private val activity: Activity,
    private val top: Boolean,
    private val fill: (SearchSuggestion) -> Unit
) : BaseAdapter() {
    private var items: List<SearchSuggestion> = emptyList()
    fun replace(values: List<SearchSuggestion>) {
        items = if (top) values.toList() else values.asReversed().toList()
        notifyDataSetChanged()
    }

    override fun getCount() = items.size
    override fun getItem(position: Int) = items[position]
    override fun getItemId(position: Int) = position.toLong()

    private class Row(val root: LinearLayout, val icon: ImageView, val title: TextView,
        val url: TextView, val fill: ImageView)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val row = convertView?.tag as? Row ?: createRow()
        val item = getItem(position)
        row.title.text = truncate(item.title)
        row.title.visibility = if (item.title.isEmpty()) View.GONE else View.VISIBLE
        row.url.text = truncate(item.url)
        row.url.visibility = if (item.url.isEmpty()) View.GONE else View.VISIBLE
        row.icon.setImageResource(when (item.type) {
            SearchSuggestionType.FAVORITE -> R.drawable.ref_search_favorite
            SearchSuggestionType.BOOKMARK -> R.drawable.ref_search_bookmark
            SearchSuggestionType.TAB -> R.drawable.ref_search_tab
            SearchSuggestionType.HISTORY, SearchSuggestionType.SEARCH_HISTORY -> R.drawable.ref_search_history
            else -> R.drawable.ref_search_search
        })
        row.fill.setOnClickListener { fill(item) }
        return row.root
    }

    private fun createRow(): Row {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
            setBackgroundResource(R.drawable.ref_search_row_feedback)
            layoutParams = android.widget.AbsListView.LayoutParams(-1, -2)
        }
        fun icon() = ImageView(activity).apply {
            setPadding(dp(13), dp(13), dp(13), dp(13))
            imageTintList = ColorStateList.valueOf(activity.palette.icon)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        fun text(secondary: Boolean) = NativeTypography.text(activity).apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimensionPixelSize(if (secondary) R.dimen.ref_search_secondary_size else R.dimen.dialog_body_size).toFloat())
            setTextColor(if (secondary) activity.palette.secondary else activity.palette.text)
            setSingleLine(true)
            textDirection = View.TEXT_DIRECTION_LOCALE
            ellipsize = TextUtils.TruncateAt.END
        }
        val icon = icon()
        val title = text(false)
        val url = text(true)
        val labels = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(title, LinearLayout.LayoutParams(-1, -2))
            addView(url, LinearLayout.LayoutParams(-1, -2))
        }
        val fill = icon().apply {
            setImageResource(R.drawable.ref_search_fill)
            contentDescription = "填入输入框"
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            if (!this@SearchSuggestionAdapter.top) rotation = -90f
            setBackgroundResource(R.drawable.ref_search_round_feedback)
        }
        root.addView(icon, LinearLayout.LayoutParams(activity.dp(48), activity.dp(48)))
        root.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(fill, LinearLayout.LayoutParams(activity.dp(48), activity.dp(48)))
        return Row(root, icon, title, url, fill).also { root.tag = it }
    }

    private fun truncate(value: String) = if (value.length > 256) value.take(256) + "..." else value
}

internal class SearchSuggestionList(activity: Activity) : ListView(activity) {
    var heightLimit = Int.MAX_VALUE
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maximum = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) heightLimit
            else minOf(heightLimit, MeasureSpec.getSize(heightMeasureSpec))
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maximum.coerceAtLeast(0), MeasureSpec.AT_MOST))
    }
}
