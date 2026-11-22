package moe.via.browser.feature.settings.data.updates

import android.content.SharedPreferences
import moe.via.browser.feature.home.model.FavoriteDesign
import moe.via.browser.feature.home.model.LogoDesign
import moe.via.browser.feature.home.model.SearchDesign
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState

/** x9.c/d/e convert Via's own earlier customization words, without prototype revision keys. */
internal class NativePreferenceHomeUpdates(private val values: SharedPreferences) {
    fun initialDesign() {
        if (listOf("searchinfo", "logoinfo2", "favinfo", "custominfo").any(values::contains)) return
        val skin = values.getInt("homeskinchioce", 0)
        val image = values.getInt("logochioce", 0) == 1
        val logo = LogoDesign().apply {
            radius = if (image) 127 else 0
            width = if (image) 80 else 0
            height = if (image) 80 else 72
        }
        values.edit().putInt("logoinfo2", logo.flag).apply()
        val search = SearchDesign().apply { radius = if (skin == 1) 30 else 127; line = skin == 2 }
        values.edit().putInt("searchinfo", search.flag).apply()
        val favorites = FavoriteDesign().apply {
            width = if (skin == 1) 64 else 46
            height = if (skin == 1) 42 else 0
            radius = if (skin == 1) 30 else 127
            colorDisabled = skin == 2
            iconDisabled = skin == 2
        }
        values.edit().putInt("favinfo", favorites.flag).apply()
        values.edit().putInt("custominfo", if (values.getBoolean("hidesearchpart", false)) 2 else 0).apply()
    }

    fun percentageDesign() {
        val logo = LogoDesign(values.getInt("logoinfo2", LogoDesign.DEFAULT))
        if (logo.flag != LogoDesign.DEFAULT) {
            logo.radius = percentage(logo.radius)
            values.edit().putInt("logoinfo2", logo.flag).apply()
        }
        val search = SearchDesign(values.getInt("searchinfo", SearchDesign.DEFAULT))
        if (search.flag != SearchDesign.DEFAULT) {
            search.radius = percentage(search.radius)
            search.opacity = percentage(search.opacity)
            search.strokeOpacity = percentage(search.strokeOpacity)
            values.edit().putInt("searchinfo", search.flag).apply()
        }
        val favorites = FavoriteDesign(values.getInt("favinfo", FavoriteDesign.DEFAULT))
        if (favorites.flag != FavoriteDesign.DEFAULT) {
            favorites.radius = percentage(favorites.radius)
            values.edit().putInt("favinfo", favorites.flag).apply()
        }
        markHome()
    }

    fun markHome() = GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.HOME or GeneratedPageDirtyState.HOME_STYLE)
    private fun percentage(value: Int) = ((value / 127.0f) * 100.0f).toInt()
}
