package moe.pure.browser.feature.internalpages.render

import android.content.Context
import moe.pure.browser.feature.home.data.HomePreferences
import moe.pure.browser.ui.theme.BrowserNightMode

/** Native workers observe current global preferences; window locale/direction remains a value input. */
internal object GeneratedPageInputs {
    fun style(context: Context, rtl: Boolean): GeneratedPageStyle {
        val config = HomePreferences(context).config(night = BrowserNightMode.global(context), isRtl = rtl)
        return GeneratedPageStyle(rtl, config.isNight, config.isLight)
    }

    fun bookmarkOrder(context: Context): Int {
        val preferences = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
        val key = if (preferences.contains("bookmarksorder")) "bookmarksorder" else "bookmark_sort"
        return (preferences.all[key] as? Number)?.toInt() ?: 0
    }
}
