package moe.pure.browser.feature.adblock

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class FilterPreset(val url: String, val title: String)
object FilterDefaults {
    fun forLanguage(context: Context, language: String): List<FilterPreset> {
        val data = JSONObject(context.assets.open("filter-presets.json").bufferedReader().use { it.readText() })
        fun entries(array: JSONArray?) = if (array == null) emptyList() else List(array.length()) { index -> array.getJSONObject(index).let { FilterPreset(it.getString("url"), it.getString("title")) } }
        val base = entries(data.getJSONArray("base"))
        val region = entries(data.getJSONObject("regions").optJSONArray(language.substringBefore('-').uppercase(Locale.ROOT)))
        return base.take(1) + region + base.drop(1)
    }
}
