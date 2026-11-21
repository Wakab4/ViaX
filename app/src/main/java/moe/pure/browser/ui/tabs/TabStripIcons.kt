package moe.pure.browser.ui.tabs

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.widget.ImageView
import moe.pure.browser.R
import moe.pure.browser.feature.library.data.NativeFaviconCache
import moe.pure.browser.feature.settings.skin.NativeSkinIcons

/** Favicon and skin lookups share their process stores; this helper only binds visible images. */
internal class TabStripIcons(private val context: Context) {
    private val skin = NativeSkinIcons(context)

    fun site(view: ImageView, url: String, live: Bitmap?, tint: Int, readFile: Boolean) {
        val bitmap = live?.takeUnless { it.isRecycled } ?: if (readFile) NativeFaviconCache.bitmap(context, url) else null
        if (bitmap != null) {
            view.imageTintList = null
            view.setImageBitmap(bitmap)
        } else control(view, R.drawable.ref_ic_webpage, tint)
    }

    fun control(view: ImageView, resource: Int, tint: Int) {
        view.imageTintList = ColorStateList.valueOf(tint)
        view.setImageDrawable((skin.resolve(resource) ?: context.getDrawable(resource))?.mutate())
    }

}
