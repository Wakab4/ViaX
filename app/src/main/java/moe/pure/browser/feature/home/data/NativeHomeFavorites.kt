package moe.pure.browser.feature.home.data

import android.content.Context

/** pa.r.f obtains the application repository; dismissing an editor does not close that store. */
internal object NativeHomeFavorites {
    private var repository: HomeFavoritesRepository? = null
    @Synchronized fun get(context: Context): HomeFavoritesRepository = repository
        ?: HomeFavoritesRepository(context.applicationContext).also { repository = it }
}
