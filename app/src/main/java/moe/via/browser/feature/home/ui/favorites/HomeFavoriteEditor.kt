package moe.via.browser.feature.home.ui.favorites

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** s.xml: management editor only. The standalone dialog has its own original layout. */
internal class HomeFavoriteEditor(context: Context) {
    val icons = FavoriteIconViews.create(context)
    val title = FavoriteInputField.create(context, address = false)
    val url = FavoriteInputField.create(context, address = true)
    var onSubmit: () -> Unit = {}
    var onClose: () -> Unit = {}
    val view = RelativeLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(-1, -2)
        val back = ImageView(context).apply {
            id = R.id.home_favorite_back; setImageResource(R.drawable.ref_favorite_back)
            imageTintList = ColorStateList.valueOf(context.palette.icon)
            setBackgroundResource(R.drawable.ref_favorite_feedback)
            setPadding(dp(13), dp(13), dp(13), dp(13))
            contentDescription = context.getString(android.R.string.cancel); setOnClickListener { onClose() }
        }
        val caption = NativeTypography.text(context).apply {
            id = R.id.home_favorite_caption; setText(R.string.ref_favorite_edit); gravity = Gravity.CENTER
            textSize = 16f; NativeTypography.style(this, Typeface.BOLD); setTextColor(context.palette.text)
            maxLines = 1; setLines(1); ellipsize = TextUtils.TruncateAt.END; textDirection = View.TEXT_DIRECTION_LOCALE
        }
        val done = NativeTypography.text(context).apply {
            id = R.id.home_favorite_positive; setText(R.string.ref_favorite_done); gravity = Gravity.CENTER
            textSize = 14f; setTextColor(0xFF6F8DE1.toInt()); minWidth = dp(56)
            setBackgroundResource(R.drawable.ref_favorite_feedback); setOnClickListener { onSubmit() }
        }
        addView(back, RelativeLayout.LayoutParams(dp(48), dp(48)))
        addView(caption, RelativeLayout.LayoutParams(-1, 0).apply {
            addRule(RelativeLayout.END_OF, back.id); addRule(RelativeLayout.START_OF, done.id)
            addRule(RelativeLayout.ALIGN_TOP, back.id); addRule(RelativeLayout.ALIGN_BOTTOM, back.id)
        })
        addView(done, RelativeLayout.LayoutParams(-2, -2).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            addRule(RelativeLayout.ALIGN_TOP, caption.id); addRule(RelativeLayout.ALIGN_BOTTOM, caption.id)
        })
        addView(icons.image, RelativeLayout.LayoutParams(dp(42), dp(42)).apply {
            marginStart = dp(16); leftMargin = dp(16); topMargin = dp(8)
            addRule(RelativeLayout.BELOW, caption.id)
        })
        addView(icons.remove, RelativeLayout.LayoutParams(dp(28), dp(28)).apply {
            topMargin = -dp(8); rightMargin = -dp(8); marginEnd = -dp(8)
            addRule(RelativeLayout.ALIGN_TOP, icons.image.id); addRule(RelativeLayout.ALIGN_END, icons.image.id)
            addRule(RelativeLayout.ALIGN_RIGHT, icons.image.id)
        })
        addView(title, RelativeLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(16), 0, dp(16), dp(16)); marginStart = dp(16); marginEnd = dp(16)
            addRule(RelativeLayout.END_OF, icons.image.id); addRule(RelativeLayout.BELOW, caption.id)
        })
        addView(url, RelativeLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(16), 0, dp(16), dp(16)); marginStart = dp(16); marginEnd = dp(16)
            addRule(RelativeLayout.END_OF, icons.image.id); addRule(RelativeLayout.BELOW, title.id)
        })
    }
}
