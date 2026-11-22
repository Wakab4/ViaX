package moe.via.browser.feature.home.render

import android.content.Context
import android.text.TextUtils
import android.view.View
import moe.via.browser.R

/** Resolve presentation resources before passing plain values to the renderer and its file cache. */
internal object HomeRenderContext {
    fun options(context: Context) = HomeRenderOptions(
        pageTitle = context.getString(R.string.ref_home_page_title),
        bookmarkTitle = context.getString(R.string.ref_home_bookmarks_title),
        searchTitle = context.getString(R.string.ref_home_search_title)
    )

    /** Native g6.l.d resolves direction from the supplied context's first locale. */
    fun isRtl(context: Context) = TextUtils.getLayoutDirectionFromLocale(context.resources.configuration.locales[0]) == View.LAYOUT_DIRECTION_RTL
}
