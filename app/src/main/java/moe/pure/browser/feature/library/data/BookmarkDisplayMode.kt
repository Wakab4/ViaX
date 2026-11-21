package moe.pure.browser.feature.library.data

import android.content.SharedPreferences

/** Native bookmarksviewmode is independent of sorting. */
internal object BookmarkDisplayMode {
    fun details(values: SharedPreferences) = values.getInt("bookmarksviewmode", 0) == 1

    fun setDetails(values: SharedPreferences, details: Boolean) {
        values.edit().putInt("bookmarksviewmode", if (details) 1 else 0).apply()
    }
}
