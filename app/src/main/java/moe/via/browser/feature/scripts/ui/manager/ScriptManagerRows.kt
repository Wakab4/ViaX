package moe.via.browser.feature.scripts.ui.manager

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextUtils
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.ui.components.recycler.bindNativeItemClick
import moe.via.browser.feature.scripts.model.UserScript
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

internal sealed interface ScriptManagerRow {
    data class Global(val enabled: Boolean) : ScriptManagerRow
    data class Interval(val label: String) : ScriptManagerRow
    data object Section : ScriptManagerRow
    data class Script(val data: UserScript, val global: Boolean) : ScriptManagerRow
}

internal class ScriptManagerAdapter(private val host: Host) : RecyclerView.Adapter<ScriptManagerAdapter.Holder>() {
    interface Host {
        fun global(enabled: Boolean)
        fun interval()
        fun open(script: UserScript)
        fun toggle(script: UserScript, enabled: Boolean)
        fun menu(script: UserScript, anchor: View)
    }
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    private var rows = emptyList<ScriptManagerRow>()
    fun position(id: Long) = rows.indexOfFirst { it is ScriptManagerRow.Script && it.data.id == id }
    fun submit(value: List<ScriptManagerRow>) {
        val previous = rows
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = value.size
            override fun areItemsTheSame(old: Int, new: Int): Boolean {
                val first = previous[old]; val second = value[new]
                return when {
                    first is ScriptManagerRow.Script && second is ScriptManagerRow.Script -> first.data.id == second.data.id
                    else -> first.javaClass == second.javaClass
                }
            }
            override fun areContentsTheSame(old: Int, new: Int): Boolean {
                val first = previous[old]; val second = value[new]
                return if (first is ScriptManagerRow.Script && second is ScriptManagerRow.Script)
                    first.global == second.global && first.data.enabled == second.data.enabled && first.data.name == second.data.name &&
                        first.data.version == second.data.version && first.data.content.isEmpty() == second.data.content.isEmpty()
                else first == second
            }
            override fun getChangePayload(old: Int, new: Int): Any = Unit
        })
        rows = value; diff.dispatchUpdatesTo(this)
    }
    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = when (rows[position]) {
        is ScriptManagerRow.Global -> 0; is ScriptManagerRow.Interval -> 1; ScriptManagerRow.Section -> 2; is ScriptManagerRow.Script -> 3
    }
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): Holder {
        val context = parent.context
        val view = when (type) {
            0, 3 -> CheckRow(context)
            1 -> LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(20), dp(16), dp(20)); background = context.touchFeedback()
                addView(label(context), LinearLayout.LayoutParams(-1, -2))
                addView(label(context, true), LinearLayout.LayoutParams(-2, -2))
            }
            else -> label(context, true).apply {
                setTextColor(context.palette.accent); typeface = Typeface.create(typeface, Typeface.BOLD)
                setPadding(dp(16), dp(12), dp(16), dp(4))
            }
        }
        view.layoutParams = RecyclerView.LayoutParams(-1, -2)
        return Holder(view)
    }
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        when (row) {
            is ScriptManagerRow.Global -> (holder.itemView as CheckRow).apply {
                title.setText(R.string.ref_script_popup_global); detail.visibility = View.GONE
                isEnabled = true; title.alpha = 1f; check.alpha = 1f
                check.setOnCheckedChangeListener(null); check.isChecked = row.enabled
                check.isClickable = false; check.isFocusable = false; check.isEnabled = true
                bindNativeItemClick { host.global(!row.enabled) }; setOnLongClickListener(null)
            }
            is ScriptManagerRow.Interval -> (holder.itemView as LinearLayout).apply {
                (getChildAt(0) as TextView).setText(R.string.ref_script_popup_auto_update)
                (getChildAt(1) as TextView).text = row.label
                bindNativeItemClick(host::interval); setOnLongClickListener(null)
            }
            ScriptManagerRow.Section -> (holder.itemView as TextView).setText(R.string.ref_script_popup_title)
            is ScriptManagerRow.Script -> (holder.itemView as CheckRow).apply {
                title.text = row.data.name; detail.text = row.data.version
                detail.visibility = if (row.data.version.isNullOrEmpty()) View.GONE else View.VISIBLE
                val alpha = if (!row.global || row.data.content.isEmpty()) .5f else 1f
                title.alpha = alpha; detail.alpha = alpha; check.alpha = alpha; isEnabled = row.global
                check.setOnCheckedChangeListener(null); check.isChecked = row.data.enabled
                // sa.h1 leaves the independent checkbox active even when its row/global switch is disabled.
                check.isEnabled = true; check.isClickable = true; check.isFocusable = true; check.contentDescription = row.data.name
                check.setOnCheckedChangeListener { _, checked -> host.toggle(row.data, checked) }
                bindNativeItemClick { if (row.global) host.open(row.data) }
                setOnLongClickListener { if (row.global) { host.menu(row.data, it); true } else false }
                post { val bounds = Rect(); check.getHitRect(bounds); bounds.inset(-dp(24), -dp(24)); touchDelegate = TouchDelegate(bounds, check) }
            }
        }
    }
    private class CheckRow(context: Context) : RelativeLayout(context) {
        val title = label(context).apply { id = View.generateViewId() }
        val detail = label(context, true)
        val check = CheckBox(context).apply {
            id = View.generateViewId(); buttonDrawable = context.getDrawable(R.drawable.dialog_checkbox); buttonTintList = null
            minimumWidth = 0; minimumHeight = 0; setPadding(0, 0, 0, 0); background = null
        }
        init {
            setPadding(dp(16), dp(20), dp(16), dp(20)); background = context.touchFeedback()
            addView(title, LayoutParams(-1, -2).apply { addRule(ALIGN_PARENT_START); addRule(START_OF, check.id) })
            addView(detail, LayoutParams(-1, -2).apply { addRule(ALIGN_PARENT_START); addRule(START_OF, check.id); addRule(BELOW, title.id) })
            addView(check, LayoutParams(-2, dp(20)).apply { addRule(ALIGN_PARENT_END); addRule(CENTER_VERTICAL); marginStart = dp(12) })
        }
    }
    private companion object {
        fun label(context: Context, detail: Boolean = false) = NativeTypography.text(context).apply {
            setTextColor(if (detail) context.palette.secondary else context.palette.text)
            textDirection = View.TEXT_DIRECTION_LOCALE
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(if (detail) R.dimen.settings_detail_size else R.dimen.dialog_body_size).toFloat())
            maxLines = if (detail) 3 else 1; if (!detail) setSingleLine(true); ellipsize = TextUtils.TruncateAt.END
        }
    }
}
