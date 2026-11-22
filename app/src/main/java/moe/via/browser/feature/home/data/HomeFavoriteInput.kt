package moe.via.browser.feature.home.data

import android.content.Context
import moe.via.browser.feature.search.data.SearchEngineRepository
import moe.via.browser.feature.search.resolve.SearchTemplate
import moe.via.browser.feature.search.resolve.SearchUrlResolver
import moe.via.browser.storage.preferences.BrowserPreferences

/** i0.v uses the ordinary selected desktop/mobile search template when input is not an address. */
internal object HomeFavoriteInput {
    fun resolve(context: Context, raw: String): String {
        if (SearchUrlResolver.isAddress(raw)) return SearchUrlResolver.normalize(raw)
        val desktop = BrowserPreferences(context.getSharedPreferences("browser", Context.MODE_PRIVATE)).desktop
        return SearchEngineRepository(context).use { engines -> SearchTemplate.apply(engines.selected().template(desktop), raw) }
    }
}
