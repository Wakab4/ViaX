package moe.via.browser.feature.ai.ui.settings

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect

internal class AiPromptFields(activity: Activity) {
    val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, 0) }
    val title = AiLineField(activity, R.string.ref_search_page_title_hint, 1, 8)
    val content = AiFields.editor(activity, R.string.ref_ai_page_content, 655361).apply {
        setLines(7); minLines = 7; maxLines = 7; setHorizontallyScrolling(false); gravity = Gravity.TOP; textDirection = View.TEXT_DIRECTION_LTR
        setPaddingRelative(paddingStart, dp(8), paddingEnd, dp(8)); setBackgroundResource(R.drawable.ref_ai_field_underline)
    }
    val variables = RecyclerView(activity).apply { layoutManager = LinearLayoutManager(activity); NativeRecyclerEdgeEffect.install(this) }
    init { root.addView(title, AiFields.margin(activity)); root.addView(content, AiFields.margin(activity, 0)); root.addView(variables, LinearLayout.LayoutParams(-1, -2).apply { topMargin = activity.dp(18) }) }
    fun close() { variables.stopScroll(); variables.itemAnimator?.endAnimations(); (variables.edgeEffectFactory as? NativeRecyclerEdgeEffect)?.close(); variables.adapter = null }
}
