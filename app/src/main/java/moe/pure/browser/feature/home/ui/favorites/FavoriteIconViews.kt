package moe.pure.browser.feature.home.ui.favorites

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp

/** Both original layouts share the icon ids; g.xml displays 36dp, s.xml displays 42dp. */
internal class FavoriteIconViews private constructor(val image: ImageView, val remove: ImageView) {
    companion object {
        fun bind(root: View) = FavoriteIconViews(root.findViewById(R.id.home_favorite_icon), root.findViewById(R.id.home_favorite_remove))
        fun create(context: Context) = FavoriteIconViews(ImageView(context).apply {
            id = R.id.home_favorite_icon; contentDescription = context.getString(R.string.ref_favorite_icon_label)
        }, ImageView(context).apply {
            id = R.id.home_favorite_remove; setImageResource(R.drawable.ref_home_remove)
            imageTintList = ColorStateList.valueOf(0x55FFFFFF)
            setPadding(context.dp(5), context.dp(5), context.dp(5), context.dp(5))
            alpha = .8f; visibility = View.GONE
            contentDescription = context.getString(R.string.ref_favorite_remove)
        })
    }
}
