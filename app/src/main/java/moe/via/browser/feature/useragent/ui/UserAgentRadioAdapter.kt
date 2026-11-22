package moe.via.browser.feature.useragent.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.os.SystemClock
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.feature.useragent.data.UserAgent
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** a6.i/j leading checkbox rows. Exactly one checked value is a controller decision. */
internal class UserAgentRadioAdapter(private val activity: Activity, private val select: (Int) -> Unit,
    private val longClick: (View, Int, UserAgent) -> Unit) : RecyclerView.Adapter<UserAgentRadioAdapter.Holder>() {
    private data class Row(val agent: UserAgent, val checked: Boolean)
    class Holder(context: Activity) : RecyclerView.ViewHolder(RelativeLayout(context)) {
        private val row = itemView as RelativeLayout
        val check = CheckBox(context).apply {
            id = View.generateViewId(); isClickable = false; isFocusable = false
            buttonDrawable = context.getDrawable(R.drawable.dialog_checkbox); buttonTintList = null
        }
        val title = NativeTypography.text(context).apply {
            setTextColor(context.palette.text); textSize = 14f; setLines(1); maxLines = 1
            ellipsize = TextUtils.TruncateAt.END; textDirection = View.TEXT_DIRECTION_LOCALE
        }
        init {
            row.setPaddingRelative(row.dp(16), row.dp(20), row.dp(16), row.dp(20))
            row.setBackgroundResource(R.drawable.ref_redirect_page_feedback)
            row.addView(check, RelativeLayout.LayoutParams(-2, row.dp(20)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START); addRule(RelativeLayout.CENTER_VERTICAL); marginEnd = row.dp(18)
            })
            row.addView(title, RelativeLayout.LayoutParams(-1, -2).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END); addRule(RelativeLayout.END_OF, check.id)
            })
            row.layoutParams = RecyclerView.LayoutParams(-1, -2)
        }
    }
    private var rows = emptyList<Row>()
    fun submit(agents: List<UserAgent>, selectedId: Int) {
        val next = agents.map { Row(it, it.id == selectedId) }; val previous = rows
        val changes = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = next.size
            override fun areItemsTheSame(old: Int, new: Int) = previous[old].agent.id == next[new].agent.id
            override fun areContentsTheSame(old: Int, new: Int) =
                previous[old].agent.name == next[new].agent.name && previous[old].checked == next[new].checked
            override fun getChangePayload(old: Int, new: Int): Any = Unit
        })
        rows = next; changes.dispatchUpdatesTo(this)
    }
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(activity)
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val value = rows[position]
        holder.title.text = UserAgentLabels.label(activity, value.agent); holder.check.isChecked = value.checked
        var last: Long? = null
        holder.itemView.setOnClickListener {
            val current = holder.bindingAdapterPosition
            val now = SystemClock.elapsedRealtime()
            if (current != RecyclerView.NO_POSITION && (last == null || kotlin.math.abs(now - last!!) > 300)) {
                last = now; select(current)
            }
        }
        holder.itemView.setOnLongClickListener {
            val current = holder.bindingAdapterPosition
            if (current != RecyclerView.NO_POSITION) rows.getOrNull(current)?.agent?.let { agent ->
                if (agent.id > 0) longClick(holder.itemView, current, agent)
            }
            true
        }
    }
}
