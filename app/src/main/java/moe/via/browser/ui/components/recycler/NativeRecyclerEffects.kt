package moe.via.browser.ui.components.recycler

import android.graphics.Canvas
import android.graphics.Paint
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.ui.components.dp

internal object NativeRecyclerEffects {
    fun install(view: RecyclerView, topDivider: Boolean = false) {
        view.layoutManager = LinearLayoutManager(view.context)
        view.itemAnimator = DefaultItemAnimator()
        NativeRecyclerEdgeEffect.install(view)
        view.isVerticalScrollBarEnabled = false
        if (topDivider) view.addItemDecoration(TopDivider())
    }

    /** c6.c: a single onDrawOver divider; full bookmark rows do not use this decoration. */
    private class TopDivider : RecyclerView.ItemDecoration() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x30808080 }
        override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val child = parent.getChildAt(0) ?: return
            if (parent.getChildAdapterPosition(child) > 0 || child.top < 0)
                canvas.drawRect(parent.paddingLeft.toFloat(), 0f, (parent.width - parent.paddingRight).toFloat(), parent.dp(1).toFloat(), paint)
        }
    }
}
