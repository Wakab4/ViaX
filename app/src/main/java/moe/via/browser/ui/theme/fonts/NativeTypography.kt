package moe.via.browser.ui.theme.fonts

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.ui.components.toolbar.NativePageToolbar

/** x8.g.d/e/f applies the current face while retaining the widget's requested style. */
internal object NativeTypography {
    fun bind(view: TextView) = style(view, view.typeface?.style ?: 0)
    fun style(view: TextView, style: Int) { view.setTypeface(NativeFontSelection.current(view.context), style) }
    fun text(context: Context, attributes: AttributeSet? = null, defStyle: Int = android.R.attr.textViewStyle,
        defStyleRes: Int = 0): TextView = TextView(context, attributes, defStyle, defStyleRes).also(::bind)
    fun edit(context: Context, attributes: AttributeSet? = null, defStyle: Int = android.R.attr.editTextStyle,
        defStyleRes: Int = 0): EditText = EditText(context, attributes, defStyle, defStyleRes).also(::bind)
    fun tree(view: View) {
        if (view is RecyclerView || view is ListView || view is NativePageToolbar) return
        if (view is TextView) bind(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            val child = view.getChildAt(index)
            if (child is TextView) bind(child)
            if (child is ViewGroup) tree(child)
        }
    }
}
