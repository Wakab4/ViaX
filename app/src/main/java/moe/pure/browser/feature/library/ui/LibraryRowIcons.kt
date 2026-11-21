package moe.pure.browser.feature.library.ui

import android.content.Context
import android.content.res.ColorStateList
import android.widget.ImageView
import moe.pure.browser.R
import moe.pure.browser.feature.library.LibraryEntry
import moe.pure.browser.feature.library.LibraryKind
import moe.pure.browser.feature.library.data.NativeFaviconCache
import moe.pure.browser.feature.settings.skin.NativeSkinIcons
import moe.pure.browser.ui.theme.palette

/** Full bookmarks and history share WebChrome's local files; they never fetch icons here. */
internal class LibraryRowIcons(private val context: Context) {
    private val skin = NativeSkinIcons(context)

    fun bind(view: ImageView, entry: LibraryEntry) {
        val address = when (entry.kind) {
            // cb.h0 uses i6.i0.f before x0: unlike the compact chooser, its key drops the port.
            LibraryKind.BOOKMARK -> bookmarkAuthority(entry.url)
            LibraryKind.HISTORY -> entry.url
            else -> ""
        }
        val bitmap = NativeFaviconCache.bitmap(context, address)
        if (bitmap != null) {
            view.imageTintList = null
            view.setImageBitmap(bitmap)
        } else {
            val resource = when (entry.kind) {
                LibraryKind.FOLDER -> R.drawable.ref_ic_folder
                LibraryKind.HISTORY -> R.drawable.ref_ic_history
                LibraryKind.OFFLINE -> R.drawable.ref_ic_menu_offline
                else -> R.drawable.ref_ic_bookmark
            }
            view.imageTintList = ColorStateList.valueOf(context.palette.icon)
            view.setImageDrawable((skin.resolve(resource) ?: context.getDrawable(resource))?.mutate())
        }
    }

    private fun bookmarkAuthority(url: String): String {
        val scheme = url.indexOf("://")
        if (scheme < 0) return ""
        val start = scheme + 3
        val end = url.indexOf('/', start).takeIf { it >= 0 } ?: url.length
        return url.substring(start, end).substringBefore(':')
    }
}
