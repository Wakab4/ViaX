package moe.pure.browser.feature.scripts.ui.details

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.feature.scripts.ui.ScriptRowViews
import moe.pure.browser.feature.scripts.ui.resource.ScriptResourcePresentation
import moe.pure.browser.ui.settings.NativeSettingViews
import java.io.File

internal class ScriptDetailsAdapter(private val activity: Activity,
    private val clicked: (ScriptDetailsItem, Int) -> Unit) : RecyclerView.Adapter<ScriptDetailsAdapter.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    private var rows: List<ScriptDetailsItem> = emptyList()

    fun submit(next: List<ScriptDetailsItem>) {
        val before = rows
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = before.size
            override fun getNewListSize() = next.size
            override fun areItemsTheSame(old: Int, new: Int): Boolean {
                val a = before[old]; val b = next[new]
                if (a.javaClass != b.javaClass) return false
                return when (a) {
                    is ScriptDetailsItem.Setting -> a.id == (b as ScriptDetailsItem.Setting).id
                    is ScriptDetailsItem.Section -> true
                    is ScriptDetailsItem.Add -> a.match == (b as ScriptDetailsItem.Add).match
                    is ScriptDetailsItem.Scope -> a.value.pattern == (b as ScriptDetailsItem.Scope).value.pattern
                    is ScriptDetailsItem.Resource -> a.value.url == (b as ScriptDetailsItem.Resource).value.url
                }
            }
            override fun areContentsTheSame(old: Int, new: Int) = before[old] == next[new]
            override fun getChangePayload(old: Int, new: Int): Any? = if (before[old].javaClass == next[new].javaClass) Any() else null
        })
        rows = next; diff.dispatchUpdatesTo(this)
    }
    fun firstMissingResource() = rows.indexOfFirst { it is ScriptDetailsItem.Resource && !it.value.cached }
    fun changed(position: Int) = notifyItemChanged(position)
    fun downloaded(url: String) {
        val position = rows.indexOfFirst { it is ScriptDetailsItem.Resource && it.value.url == url }
        if (position == -1) return
        val value = (rows[position] as ScriptDetailsItem.Resource).value
        value.cached = File(value.path!!).exists()
        notifyItemChanged(position)
    }
    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = when (rows[position]) {
        is ScriptDetailsItem.Setting -> 0
        is ScriptDetailsItem.Section -> 1
        is ScriptDetailsItem.Add -> 2
        is ScriptDetailsItem.Scope -> 3
        is ScriptDetailsItem.Resource -> 4
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(when (viewType) {
        1 -> NativeSettingViews.section(activity)
        2 -> ScriptRowViews.add(activity, "") {}
        else -> NativeSettingViews.site(activity)
    }.apply { layoutParams = RecyclerView.LayoutParams(-1, -2) })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        val view = holder.itemView
        when (row) {
            is ScriptDetailsItem.Setting -> (view as LinearLayout).apply {
                (getChildAt(0) as TextView).apply { text = row.title; alpha = 1f }
                (getChildAt(1) as TextView).apply { text = row.detail; visibility = if (row.detail.isNullOrEmpty()) View.GONE else View.VISIBLE }
            }
            is ScriptDetailsItem.Section -> (view as TextView).text = row.title
            is ScriptDetailsItem.Add -> (view as TextView).text = row.title
            is ScriptDetailsItem.Scope -> bindLabel(view, row.value.pattern, 1f)
            is ScriptDetailsItem.Resource -> bindLabel(view, ScriptResourcePresentation.url(row.value.url), if (row.value.cached) 1f else .5f)
        }
        var last: Long? = null
        view.setOnClickListener(if (row is ScriptDetailsItem.Section) null else View.OnClickListener {
            val current = holder.bindingAdapterPosition
            val now = SystemClock.elapsedRealtime()
            if (current != RecyclerView.NO_POSITION && (last == null || kotlin.math.abs(now - last!!) > 300)) {
                last = now; clicked(row, current)
            }
        })
    }
    private fun bindLabel(view: View, text: String, opacity: Float) {
        (view as LinearLayout).apply {
            (getChildAt(0) as TextView).apply { this.text = text; alpha = opacity }
            getChildAt(1).visibility = View.GONE
        }
    }
}
