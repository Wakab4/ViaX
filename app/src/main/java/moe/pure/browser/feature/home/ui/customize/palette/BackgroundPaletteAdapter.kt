package moe.pure.browser.feature.home.ui.customize.palette

import android.content.Context
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** e9.i/f9.a share one palette and change only the previous/new selected rows. */
internal class BackgroundPaletteAdapter(private val context: Context, current: Int, photo: Boolean,
    private val clicked: (BackgroundPaletteEntry, Int) -> Unit) : RecyclerView.Adapter<BackgroundPaletteAdapter.Holder>() {
    class Holder(val frame: FrameLayout, val image: ImageView) : RecyclerView.ViewHolder(frame)
    private val rows = BackgroundPaletteEntry.entries(context)
    var selectedPosition = rows.indexOf(if (photo) BackgroundPaletteEntry.Image else BackgroundPaletteEntry.Color(if (current == -1) 0 else current, "Selected"))
        private set
    val selectedEntry: BackgroundPaletteEntry? get() = rows.getOrNull(selectedPosition)
    fun select(position: Int) {
        val before = selectedPosition
        if (before == position) return
        selectedPosition = position
        if (before >= 0 && before < itemCount) notifyItemChanged(before)
        if (position >= 0 && position < itemCount) notifyItemChanged(position)
    }
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val frame = FrameLayout(context)
        val image = ImageView(context).apply { setPadding(dp(6), dp(6), dp(6), dp(6)) }
        frame.addView(image, FrameLayout.LayoutParams(context.dp(48), context.dp(48), Gravity.CENTER))
        return Holder(frame, image)
    }
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = rows[position]
        holder.frame.layoutParams = ViewGroup.LayoutParams(context.dp(48), -1)
        holder.image.apply {
            when (entry) {
                is BackgroundPaletteEntry.Color -> {
                    setImageDrawable(if (entry.value == 0) BackgroundPaletteDrawables.color(context, 0, context.palette.icon, 1) else BackgroundPaletteDrawables.color(context, entry.value))
                    contentDescription = entry.name
                }
                BackgroundPaletteEntry.Image -> {
                    setImageDrawable(BackgroundPaletteDrawables.image(context))
                    contentDescription = context.getString(R.string.ref_home_palette_image)
                }
            }
            background = if (position == selectedPosition) BackgroundPaletteDrawables.color(context, 0x40808080)
                else context.getDrawable(R.drawable.ref_home_palette_feedback)
        }
        var last: Long? = null
        holder.frame.setOnClickListener {
            val current = holder.bindingAdapterPosition
            val now = SystemClock.elapsedRealtime()
            if (current != RecyclerView.NO_POSITION && (last == null || kotlin.math.abs(now - last!!) > 300)) {
                last = now; clicked(entry, current)
            }
        }
    }
}
