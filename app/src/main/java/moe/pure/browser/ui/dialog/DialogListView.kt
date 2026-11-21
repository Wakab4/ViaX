package moe.pure.browser.ui.dialog

import android.content.Context
import android.util.AttributeSet
import android.widget.ListView

class DialogListView(context: Context, attrs: AttributeSet? = null) : ListView(context, attrs) {
    var heightLimit = Int.MAX_VALUE
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val limit = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) heightLimit else minOf(heightLimit, MeasureSpec.getSize(heightMeasureSpec))
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(limit, MeasureSpec.AT_MOST))
    }
}
