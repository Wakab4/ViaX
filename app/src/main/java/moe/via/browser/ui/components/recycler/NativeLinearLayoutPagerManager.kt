package moe.via.browser.ui.components.recycler

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Native LinearLayoutPagerManager sizes each generated child from the current manager viewport. */
internal class NativeLinearLayoutPagerManager(context: Context, direction: Int, private var pageCount: Int,
    reverse: Boolean = false) : LinearLayoutManager(context, direction, reverse) {
    private fun itemSize() = (if (orientation == HORIZONTAL) width else height) / pageCount
    fun setCount(value: Int) { if (pageCount != value) { pageCount = value; requestLayout() } }
    private fun sized(params: RecyclerView.LayoutParams): RecyclerView.LayoutParams {
        if (orientation == HORIZONTAL) params.width = itemSize() else params.height = itemSize()
        return params
    }
    override fun generateDefaultLayoutParams() = sized(super.generateDefaultLayoutParams())
    override fun generateLayoutParams(params: ViewGroup.LayoutParams) = sized(super.generateLayoutParams(params))
    override fun checkLayoutParams(params: RecyclerView.LayoutParams) = super.checkLayoutParams(params) && params.width == itemSize()
}
