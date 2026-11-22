package moe.via.browser.feature.ai.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.settings.NativeSettingViews
import moe.via.browser.ui.theme.palette

/** c9.l0/h0: token and description have distinct fixed typography. */
internal class AiVariableAdapter(private val context: Context) : RecyclerView.Adapter<AiVariableAdapter.Holder>() {
    data class Variable(val token: String, val description: String)
    class Holder(view: android.view.View) : RecyclerView.ViewHolder(view)
    private var variables = emptyList<Variable>()
    fun type(type: Int) {
        variables = buildList {
            if (type == 1) { add(Variable("{{webpage_url}}", context.getString(R.string.ref_ai_page_page_url))); add(Variable("{{webpage_title}}", context.getString(R.string.ref_ai_page_page_title))); add(Variable("{{webpage_content}}", context.getString(R.string.ref_ai_page_page_content))) }
            else add(Variable("{{input}}", context.getString(R.string.ref_ai_page_input)))
            add(Variable("{{time}}", context.getString(R.string.ref_ai_page_time)))
        }; notifyDataSetChanged()
    }
    override fun getItemCount() = variables.size + 1
    override fun getItemViewType(position: Int) = if (position == 0) 0 else 1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        if (viewType == 0) return Holder(NativeSettingViews.section(context).apply { layoutParams = RecyclerView.LayoutParams(-1, -2) })
        return Holder(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL; setPaddingRelative(dp(16), dp(12), dp(16), dp(12)); setBackgroundResource(R.drawable.ref_redirect_page_feedback)
            layoutParams = RecyclerView.LayoutParams(-1, -2)
            addView(NativeTypography.text(context).apply {
                typeface = Typeface.MONOSPACE; setTextColor(context.palette.text)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.settings_detail_size).toFloat())
            }, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.CENTER_VERTICAL })
            addView(NativeTypography.text(context).apply {
                gravity = Gravity.END; setTextColor(context.palette.secondary); setPaddingRelative(dp(16), 0, 0, 0)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { gravity = Gravity.CENTER_VERTICAL })
        })
    }
    override fun onBindViewHolder(holder: Holder, position: Int) {
        if (position == 0) { (holder.itemView as TextView).setText(R.string.ref_ai_page_variables); return }
        val variable = variables[position - 1]
        (holder.itemView as LinearLayout).apply {
            (getChildAt(0) as TextView).text = variable.token; (getChildAt(1) as TextView).text = variable.description
            setOnClickListener { context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, variable.token)); BrowserMessages.show(context, context.getString(R.string.ref_password_page_copied)) }
        }
    }
}
