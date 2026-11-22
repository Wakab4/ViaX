package moe.via.browser.feature.library.ui.recycler

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

/** Full bookmarks use layout/x.xml; history follows b6.f.f. These are distinct row geometries. */
internal class LibraryItemView(context: Context, val bookmark: Boolean, history: Boolean = false) : RelativeLayout(context) {
    val icon = ImageView(context).apply { id = View.generateViewId(); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
    val title = label(14f, context.palette.text)
    val subtitle = label(12f, context.palette.secondary)
    val check = CheckBox(context).apply {
        id = View.generateViewId(); isClickable = false; isFocusable = false
        buttonDrawable = context.getDrawable(R.drawable.dialog_checkbox); buttonTintList = null
        setPadding(0, 0, 0, 0); minimumWidth = 0; minimumHeight = 0; background = null
    }
    val handle = ImageView(context).apply {
        id = View.generateViewId(); setImageResource(R.drawable.ref_library_drag_handle)
        imageTintList = ColorStateList.valueOf(context.palette.icon)
        setPadding(dp(15), dp(15), dp(15), dp(15)); contentDescription = context.getString(R.string.ref_library_drag_description)
    }
    private val hiddenMarker = if (bookmark) ImageView(context).apply {
        id = View.generateViewId()
        setImageResource(R.drawable.ref_ai_eye_off)
        imageTintList = ColorStateList.valueOf(context.palette.secondary)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        visibility = View.GONE
    } else null

    init {
        layoutParams = RecyclerView.LayoutParams(-1, -2)
        background = context.touchFeedback()
        setPadding(dp(16), if (bookmark) 0 else dp(10), dp(16), if (bookmark) 0 else dp(10))
        addView(check, LayoutParams(dp(16), if (bookmark) -2 else dp(16)).apply {
            addRule(ALIGN_PARENT_END); addRule(CENTER_VERTICAL); setMargins(dp(2), dp(2), dp(2), dp(2))
        })
        if (bookmark) {
            minimumHeight = dp(54)
            icon.scaleType = ImageView.ScaleType.CENTER_CROP
            addView(icon, LayoutParams(dp(22), dp(22)).apply { addRule(CENTER_VERTICAL); marginEnd = dp(16) })
            addView(handle, LayoutParams(dp(48), -1).apply { addRule(START_OF, check.id); setMargins(dp(16), 0, dp(16), 0) })
            val marker = requireNotNull(hiddenMarker)
            addView(marker, LayoutParams(dp(16), dp(16)).apply {
                addRule(START_OF, handle.id)
                addRule(CENTER_VERTICAL)
                alignWithParent = true
                marginStart = dp(8)
            })
            val labels = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
                addView(title, LinearLayout.LayoutParams(-2, -2))
                addView(subtitle, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(2) })
            }
            addView(labels, LayoutParams(-2, -2).apply {
                addRule(END_OF, icon.id); addRule(START_OF, marker.id); addRule(CENTER_VERTICAL); alignWithParent = true
            })
        } else if (history) {
            handle.visibility = View.GONE
            addView(icon, LayoutParams(dp(20), 0).apply { addRule(ALIGN_TOP, title.id); addRule(ALIGN_BOTTOM, subtitle.id) })
            addView(title, LayoutParams(-1, -2).apply { addRule(END_OF, icon.id); addRule(START_OF, check.id); marginStart = dp(16) })
            addView(subtitle, LayoutParams(-1, -2).apply { addRule(BELOW, title.id); addRule(ALIGN_START, title.id); addRule(ALIGN_END, title.id); topMargin = dp(2) })
        } else {
            minimumHeight = dp(56); setPadding(dp(16), dp(12), dp(16), dp(12)); handle.visibility = View.GONE
            addView(icon, LayoutParams(dp(24), dp(24)).apply { addRule(CENTER_VERTICAL) })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(title); addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })
            }, LayoutParams(-1, -2).apply { addRule(END_OF, icon.id); addRule(START_OF, check.id); addRule(CENTER_VERTICAL); marginStart = dp(16) })
        }
    }
    fun markHidden(hidden: Boolean) {
        hiddenMarker?.visibility = if (hidden) View.VISIBLE else View.GONE
    }

    private fun label(size: Float, color: Int) = NativeTypography.text(context).apply {
        id = View.generateViewId(); textSize = size; setTextColor(color)
        setSingleLine(); ellipsize = null; isHorizontalFadingEdgeEnabled = true; setFadingEdgeLength(dp(24))
        textDirection = View.TEXT_DIRECTION_LOCALE
    }
}
