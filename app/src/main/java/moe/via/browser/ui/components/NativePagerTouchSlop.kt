package moe.via.browser.ui.components

import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/** g6.y.J multiplies ViewPager2's RecyclerView touch slop by four. */
internal object NativePagerTouchSlop {
    fun apply(pager: ViewPager2) {
        try {
            val recycler = pager.getChildAt(0) as RecyclerView
            val field = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            field.isAccessible = true; field.setInt(recycler, field.getInt(recycler) * 4)
        } catch (_: Exception) { }
    }
}
