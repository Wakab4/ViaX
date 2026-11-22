package moe.via.browser.feature.site.ui.cookies

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.ui.settings.NativeSettingViews

internal data class CookieSettingRow(val id: Int, val checked: Boolean, val disabled: Boolean = false)

/** jb.i0 uses a6.n/p: both rows retain their value while the second can be disabled. */
internal class CookieSettingsAdapter(private val activity: Activity, private val clicked: (CookieSettingRow) -> Unit) :
    RecyclerView.Adapter<CookieSettingsAdapter.Holder>() {
    class Holder(val row: NativeSettingViews.Global) : RecyclerView.ViewHolder(row)
    private var rows = emptyList<CookieSettingRow>()

    fun submit(value: List<CookieSettingRow>) {
        val previous = rows
        val changes = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = value.size
            override fun areItemsTheSame(old: Int, new: Int) = previous[old].id == value[new].id
            override fun areContentsTheSame(old: Int, new: Int) = previous[old] == value[new]
            override fun getChangePayload(old: Int, new: Int): Any = Unit
        })
        rows = value; changes.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(NativeSettingViews.Global(activity).apply {
        layoutParams = RecyclerView.LayoutParams(-1, -2)
    })

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val value = rows[position]
        holder.row.apply {
            title.setText(if (value.id == 1) R.string.ref_cookie_page_title else R.string.ref_cookie_page_block_third_party)
            if (value.id == 1) detail.setText(if (value.checked)
                R.string.ref_cookie_page_enabled_detail else R.string.ref_cookie_page_blocked)
            detail.visibility = if (value.id == 1) View.VISIBLE else View.GONE
            check.isChecked = value.checked
            val alpha = if (value.disabled) .5f else 1f
            title.alpha = alpha; detail.alpha = alpha; check.alpha = alpha
            isEnabled = !value.disabled
            var last: Long? = null
            setOnClickListener {
                val now = SystemClock.elapsedRealtime()
                if (isEnabled && holder.bindingAdapterPosition != RecyclerView.NO_POSITION &&
                    (last == null || kotlin.math.abs(now - last!!) > 300)) {
                    last = now; clicked(value)
                }
            }
        }
    }
}
