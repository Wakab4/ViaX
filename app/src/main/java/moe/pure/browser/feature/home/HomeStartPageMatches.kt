package moe.pure.browser.feature.home

import android.content.Context
import moe.pure.browser.feature.home.data.HomePreferences

/** u9.d.n/o compares the configured start page, which is distinct from a generated homepage. */
object HomeStartPageMatches {
    fun matches(context: Context, url: String): Boolean {
        val configured = HomePreferences(context).homepage
        val address = HomeDocument.normalize(context, url)
        val prefix = "file://${context.filesDir.path}/"
        val name = address.takeIf { it.startsWith(prefix) }?.substring(prefix.length)
            ?.substringBefore('?')?.substringBefore('#')
        return when {
            HomeDocument.isHome(context, address) -> configured == "about:home" || configured == "about:links"
            name == "bookmarks.html" -> configured == "about:bookmarks"
            name == "blank.html" -> configured == "about:blank"
            else -> address.equals(configured, ignoreCase = true) || address.equals("$configured/", ignoreCase = true)
        }
    }
}
