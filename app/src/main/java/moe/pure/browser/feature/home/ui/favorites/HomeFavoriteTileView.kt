package moe.pure.browser.feature.home.ui.favorites

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.feature.home.model.HomeFavorite
import moe.pure.browser.feature.home.model.HomepageConfig
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.surface
import moe.pure.browser.ui.theme.palette

/** Original a0.xml: the remove control is anchored to the icon, not the grid column edge. */
internal class HomeFavoriteTileView(context: Context) : RelativeLayout(context) {
    var removeClick: () -> Unit = {}
    private val icon = NativeTypography.text(context).apply {
        id = View.generateViewId(); gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)
    }
    private val title = NativeTypography.text(context).apply {
        gravity = Gravity.CENTER; maxLines = 1
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10f)
        setLineSpacing(context.dp(4).toFloat(), 1f)
        setTextColor(context.palette.text)
    }
    private val remove = ImageView(context).apply {
        setImageResource(R.drawable.ref_home_remove)
        setPadding(context.dp(5), context.dp(5), context.dp(5), context.dp(5))
        imageTintList = ColorStateList.valueOf(0x55FFFFFF); alpha = .8f
        setOnClickListener { removeClick() }
    }

    init {
        setBackgroundResource(R.drawable.ref_favorite_feedback)
        addView(icon, LayoutParams(-1, context.dp(36)).apply {
            setMargins(context.dp(9), context.dp(4), context.dp(9), 0)
            addRule(CENTER_HORIZONTAL)
        })
        addView(remove, LayoutParams(context.dp(28), context.dp(28)).apply {
            topMargin = -context.dp(8); rightMargin = -context.dp(8); marginEnd = -context.dp(8)
            addRule(ALIGN_TOP, icon.id); addRule(ALIGN_RIGHT, icon.id); addRule(ALIGN_END, icon.id)
        })
        addView(title, LayoutParams(-1, context.dp(20)).apply {
            topMargin = context.dp(2); bottomMargin = context.dp(4)
            addRule(BELOW, icon.id); addRule(CENTER_HORIZONTAL)
        })
    }

    fun bind(item: HomeFavorite, config: HomepageConfig, bitmap: Bitmap?, color: Int) {
        val width = context.dp(config.favoriteItemWidth)
        icon.layoutParams = icon.layoutParams.apply { this.width = width; height = context.dp(config.favoriteItemHeight) }
        title.layoutParams = title.layoutParams.apply { this.width = width }
        title.text = item.title
        icon.setTextColor(if (config.favoriteColorDisabled) context.palette.text else -1)
        if (bitmap == null) {
            if (item.title.isNotEmpty()) icon.text = String(Character.toChars(item.title.codePointAt(0)))
            if (!config.favoriteColorDisabled) icon.background = context.surface(color, config.favoriteItemRadius)
        } else {
            icon.text = ""; icon.background = BitmapDrawable(resources, bitmap)
        }
        remove.contentDescription = context.getString(R.string.ref_favorite_remove)
    }
}
