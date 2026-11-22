package moe.via.browser.feature.download.open

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.InsetDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.ui.components.recycler.bindNativeItemClick
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

/** layout/a2 and r1: 32dp app images and two 11sp lines, without tinting third-party artwork. */
internal class FileOpenGrid(private val context: Context, private val clicked: (FileOpenChoice) -> Unit) : RecyclerView.Adapter<FileOpenGrid.Cell>() {
    private var entries = emptyList<FileOpenChoice>()
    init { setHasStableIds(true) }
    fun submit(entries: List<FileOpenChoice>) { this.entries = entries; notifyDataSetChanged() }
    override fun getItemCount() = entries.size
    override fun getItemId(position: Int) = entries[position].id
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): Cell {
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(12), 0, dp(4)); background = context.touchFeedback()
            layoutParams = RecyclerView.LayoutParams(-1, -2)
        }
        val image = ImageView(context).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        body.addView(image, LinearLayout.LayoutParams(context.dp(32), context.dp(32)))
        val label = NativeTypography.text(context).apply {
            textSize = 11f; gravity = Gravity.CENTER; ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(4), 0, dp(4), 0); minLines = 2; maxLines = 2
            textDirection = View.TEXT_DIRECTION_LOCALE; textAlignment = View.TEXT_ALIGNMENT_CENTER
            setLineSpacing(context.dp(4).toFloat(), 1f); setTextColor(context.palette.text)
        }
        body.addView(label, LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(8) })
        return Cell(body, image, label)
    }
    override fun onBindViewHolder(holder: Cell, position: Int) {
        val choice = entries[position]
        holder.label.text = choice.label
        holder.image.imageTintList = null
        holder.image.setImageDrawable(if (choice.kind == 2) choice.icon?.constantState?.newDrawable(context.resources)?.mutate()?.let {
            it.setTint(context.palette.icon)
            // Native lb.a rejects the metadata key "Via" because it does not begin with ic_.
            InsetDrawable(it, (context.resources.displayMetrics.density * 4).toInt())
        } else choice.icon)
        holder.itemView.bindNativeItemClick { clicked(choice) }
    }
    class Cell(view: View, val image: ImageView, val label: TextView) : RecyclerView.ViewHolder(view)
}

internal class FileOpenGridLayout(context: Context) : GridLayoutManager(context, 1) {
    var maximumHeight = 0
    override fun setMeasuredDimension(widthSize: Int, heightSize: Int) =
        super.setMeasuredDimension(widthSize, if (maximumHeight > 0) minOf(heightSize, maximumHeight) else heightSize)
}

internal class FileOpenGridSpacing(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) { outRect.set(spacing, spacing, spacing, spacing) }
}
