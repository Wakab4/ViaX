package moe.via.browser.feature.home.data

import android.content.SharedPreferences
import moe.via.browser.feature.home.model.BackgroundDesign
import moe.via.browser.feature.home.model.FavoriteDesign
import moe.via.browser.feature.home.model.GeneralDesign
import moe.via.browser.feature.home.model.HomeDesign
import moe.via.browser.feature.home.model.LogoDesign
import moe.via.browser.feature.home.model.SearchDesign

/** d9.t.q preserves its owned objects and separate writes, including the first favorite write. */
internal object NativeHomeDesignReset {
    fun write(values: SharedPreferences, design: HomeDesign) {
        values.edit().remove("taghome").apply()
        values.edit().putInt("logochioce", 0).apply()
        design.logo.flag = LogoDesign.DEFAULT
        values.edit().putInt("logoinfo2", design.logo.flag).apply()
        values.edit().putInt("favinfo", design.favorites.flag).apply()
        design.search.flag = SearchDesign.DEFAULT
        values.edit().putInt("searchinfo", design.search.flag).apply()
        values.edit().remove("bghome").apply()
        values.edit().putInt("urlbarcolor", -1).apply()
        design.favorites.flag = FavoriteDesign.DEFAULT
        values.edit().putInt("favinfo", design.favorites.flag).apply()
        design.general.flag = GeneralDesign.DEFAULT
        values.edit().putInt("custominfo", design.general.flag).apply()
        design.background.flag = BackgroundDesign.DEFAULT
        values.edit().putInt("bginfo", design.background.flag).apply()
        values.edit().remove("csstheme").apply()
    }
}
