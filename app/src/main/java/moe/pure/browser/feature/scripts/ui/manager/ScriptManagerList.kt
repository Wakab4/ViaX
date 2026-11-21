package moe.pure.browser.feature.scripts.ui.manager

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import moe.pure.browser.R
import moe.pure.browser.feature.library.ui.recycler.LibraryRecyclerView
import moe.pure.browser.feature.scripts.model.UserScript
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** sa.d1 uses a real list, with a one-shot Native mark position after its first data set. */
internal class ScriptManagerList(context: Context, private val host: ScriptManagerAdapter.Host,
    private val selectedId: Long?, alreadyMarked: Boolean) : AutoCloseable {
    private val adapter = ScriptManagerAdapter(host)
    private val list = LibraryRecyclerView(context).apply {
        this.adapter = this@ScriptManagerList.adapter
        clipToPadding = false
    }
    val insetTarget: View get() = list
    private val empty = NativeTypography.text(context).apply {
        text = "¯\\_(ツ)_/¯"; contentDescription = context.getString(R.string.ref_script_popup_empty)
        textSize = 22f; setTextColor(context.palette.secondary); gravity = android.view.Gravity.CENTER; visibility = View.GONE
        setPadding(0, dp(20), 0, dp(20))
    }
    val view = FrameLayout(context).apply { addView(list, FrameLayout.LayoutParams(-1, -1)); addView(empty, FrameLayout.LayoutParams(-1, -1)) }
    private var first = alreadyMarked
    val markConsumed get() = first
    private var animation: ObjectAnimator? = null
    private var closed = false
    fun submit(scripts: List<UserScript>, enabled: Boolean, interval: String) {
        if (closed) return
        val rows = if (scripts.isEmpty()) emptyList() else buildList {
            add(ScriptManagerRow.Global(enabled))
            if (scripts.any { it.downloadUrl != null }) add(ScriptManagerRow.Interval(interval))
            add(ScriptManagerRow.Section)
            scripts.forEach { add(ScriptManagerRow.Script(it, enabled)) }
        }
        adapter.submit(rows)
        empty.visibility = if (scripts.isEmpty()) View.VISIBLE else View.GONE
        list.visibility = if (scripts.isEmpty()) View.GONE else View.VISIBLE
        if (!first) {
            first = true
            val position = selectedId?.let(adapter::position) ?: -1
            if (position >= 0) locate(position)
        }
    }
    private fun locate(position: Int) {
        list.post {
            if (closed) return@post
            (list.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(position, 20)
            list.post highlight@{
                if (closed) return@highlight
                val row = list.findViewHolderForAdapterPosition(position)?.itemView ?: return@highlight
                animation?.cancel()
                animation = ObjectAnimator.ofFloat(row, View.TRANSLATION_X, 0f, row.dp(24).toFloat(), 0f).apply {
                    repeatCount = 2; repeatMode = ObjectAnimator.RESTART; duration = 280L
                    interpolator = PathInterpolator(.2f, .2f, .8f, .8f); start()
                }
            }
        }
    }
    fun revealTop() = list.scrollToPosition(0)
    override fun close() { closed = true; animation?.cancel(); animation = null; list.adapter = null; list.dispose() }
}
