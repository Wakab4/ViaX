package moe.pure.browser.feature.ai.ui.settings

import android.content.Context
import android.content.res.ColorStateList
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.feature.settings.skin.NativeSkinIcons
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette
import kotlin.math.abs

internal class AiModelAdapter(private val context: Context, private val models: MutableList<String>,
    private val remove: (Int) -> Unit) : RecyclerView.Adapter<AiModelAdapter.Holder>() {
    class Holder(val root: FrameLayout, val title: TextView, val remove: ImageView) : RecyclerView.ViewHolder(root)
    override fun getItemCount() = models.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val root = FrameLayout(context).apply { setBackgroundResource(R.drawable.ref_redirect_page_feedback); layoutParams = RecyclerView.LayoutParams(-1, -2) }
        val title = NativeTypography.text(context).apply {
            setTextColor(context.palette.text); setSingleLine(); maxLines = 1; setLines(1); ellipsize = TextUtils.TruncateAt.END
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
            setPaddingRelative(dp(16), dp(12), 0, dp(12))
        }
        val button = ImageView(context).apply {
            setImageDrawable(NativeSkinIcons(context).resolve(R.drawable.ref_ic_close, "ic_close") ?: context.getDrawable(R.drawable.ref_ic_close))
            imageTintList = ColorStateList.valueOf(context.palette.icon); setPaddingRelative(dp(9), 0, dp(9), 0)
            setBackgroundResource(R.drawable.dialog_button_feedback); contentDescription = context.getString(R.string.ref_search_page_delete)
        }
        root.addView(title, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_VERTICAL).apply { marginEnd = context.dp(40) })
        root.addView(button, FrameLayout.LayoutParams(context.dp(40), context.dp(40), Gravity.END or Gravity.CENTER_VERTICAL).apply { marginEnd = context.dp(10) })
        return Holder(root, title, button)
    }
    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.title.text = models[position]; var last: Long? = null
        holder.remove.setOnClickListener { val at = holder.bindingAdapterPosition; val now = SystemClock.elapsedRealtime()
            if (at >= 0 && (last == null || abs(now - last!!) > 300)) { last = now; remove(at) }
        }
    }
}
