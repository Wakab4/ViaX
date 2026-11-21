package moe.pure.browser.feature.home.render

import android.content.Context
import moe.pure.browser.feature.home.data.HomePreferences
import moe.pure.browser.feature.home.data.NativeHomeFavorites

/** r8.d.h reads the application favorite owner, then taghome, only inside the HTML branch. */
internal object HomeHtmlSource {
    fun read(context: Context): HomeHtmlInputs {
        val favorites = NativeHomeFavorites.get(context).tiles()
        return HomeHtmlInputs(HomePreferences(context).logoContent, favorites)
    }
}
