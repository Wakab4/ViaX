package moe.via.browser.ui.dialog

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

class DialogScrollView(context: Context, attrs: AttributeSet? = null) : ScrollView(context, attrs) {
    var heightLimit = Int.MAX_VALUE
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val limit = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) heightLimit else minOf(heightLimit, MeasureSpec.getSize(heightMeasureSpec))
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(limit, MeasureSpec.AT_MOST))
    }
}
