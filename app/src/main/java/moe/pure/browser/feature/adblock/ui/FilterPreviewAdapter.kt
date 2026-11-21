package moe.pure.browser.feature.adblock.ui

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

internal class FilterPreviewAdapter(private val activity: Activity, private val clicked: (String) -> Unit) : RecyclerView.Adapter<FilterPreviewAdapter.Holder>() {
    class Holder(val text: TextView) : RecyclerView.ViewHolder(text)
    private var lines: List<String> = emptyList()
    fun submit(value: List<String>) { lines = value; notifyDataSetChanged() }
    override fun getItemCount() = lines.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(NativeTypography.text(activity).apply {
        layoutParams = RecyclerView.LayoutParams(-1, -2); setPaddingRelative(dp(16), dp(16), dp(16), dp(16))
        setBackgroundResource(R.drawable.ref_redirect_page_feedback); setTextColor(activity.palette.text)
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.ref_redirect_page_body_size).toFloat())
        gravity = Gravity.CENTER_VERTICAL; setSingleLine(true); ellipsize = null
        setFadingEdgeLength(dp(24)); isHorizontalFadingEdgeEnabled = true; textDirection = View.TEXT_DIRECTION_LOCALE
    }).also { holder -> holder.text.setOnClickListener {
        val index = holder.bindingAdapterPosition
        if (index != RecyclerView.NO_POSITION) clicked(lines[index])
    } }
    override fun onBindViewHolder(holder: Holder, position: Int) { holder.text.text = lines[position] }
}
