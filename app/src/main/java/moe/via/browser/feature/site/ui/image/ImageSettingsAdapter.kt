package moe.via.browser.feature.site.ui.image

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.ui.theme.skin.NativeSkinIcons
import moe.via.browser.ui.settings.NativeSettingViews
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

internal class ImageSettingsAdapter(private val activity: Activity, private val clicked: (ImageSettingRow) -> Unit) :
    RecyclerView.Adapter<ImageSettingsAdapter.Holder>(), AutoCloseable {
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    private var rows = emptyList<ImageSettingRow>()
    private var closed = false
    private val skin = NativeSkinIcons(activity)

    fun submit(value: List<ImageSettingRow>) {
        if (closed) return
        val previous = rows
        val changes = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = value.size
            override fun areItemsTheSame(old: Int, new: Int): Boolean {
                val first = previous[old]; val second = value[new]
                return when {
                    first is ImageSettingRow.Global && second is ImageSettingRow.Global -> true
                    first === ImageSettingRow.Add && second === ImageSettingRow.Add -> true
                    first is ImageSettingRow.Section && second is ImageSettingRow.Section -> true
                    first is ImageSettingRow.Site && second is ImageSettingRow.Site -> first.key.hashCode() == second.key.hashCode()
                    else -> false
                }
            }
            override fun areContentsTheSame(old: Int, new: Int) = previous[old] == value[new]
            override fun getChangePayload(old: Int, new: Int): Any = Unit
        })
        rows = value; changes.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = when (rows[position]) {
        is ImageSettingRow.Global, is ImageSettingRow.Site -> 0
        ImageSettingRow.Add -> 1
        is ImageSettingRow.Section -> 2
    }
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): Holder {
        val view = when (type) {
            1 -> NativeSettingViews.add(activity)
            2 -> NativeSettingViews.section(activity)
            else -> NativeSettingViews.site(activity)
        }
        view.layoutParams = RecyclerView.LayoutParams(-1, -2)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        when (row) {
            is ImageSettingRow.Global -> (holder.itemView as LinearLayout).apply {
                (getChildAt(0) as TextView).setText(R.string.ref_image_page_title)
                (getChildAt(1) as TextView).apply {
                    setText(when { row.wifiOnly -> R.string.ref_image_page_wifi_detail
                        row.enabled -> R.string.ref_image_page_show_detail
                        else -> R.string.ref_image_page_blocked })
                    visibility = View.VISIBLE
                }
            }
            ImageSettingRow.Add -> (holder.itemView as TextView).apply { setText(R.string.ref_image_page_add); icon(this) }
            is ImageSettingRow.Section -> (holder.itemView as TextView).text = row.title
            is ImageSettingRow.Site -> (holder.itemView as LinearLayout).apply {
                (getChildAt(0) as TextView).text = row.key; getChildAt(1).visibility = View.GONE
            }
        }
        if (row is ImageSettingRow.Section) holder.itemView.setOnClickListener(null)
        else {
            var last: Long? = null
            holder.itemView.setOnClickListener {
                val now = SystemClock.elapsedRealtime()
                if (!closed && holder.bindingAdapterPosition != RecyclerView.NO_POSITION &&
                    (last == null || kotlin.math.abs(now - last!!) > 300)) {
                    last = now; clicked(row)
                }
            }
        }
    }

    private fun icon(view: TextView) {
        val drawable = (skin.resolve(R.drawable.ref_ic_add) ?: activity.getDrawable(R.drawable.ref_ic_add))?.mutate()
        drawable?.apply { setTint(activity.palette.text); setBounds(0, 0, activity.dp(18), activity.dp(18)) }
        view.setCompoundDrawablesRelative(drawable, null, null, null)
    }
    override fun close() { closed = true; rows = emptyList() }
}
