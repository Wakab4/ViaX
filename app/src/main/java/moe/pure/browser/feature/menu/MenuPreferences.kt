package moe.pure.browser.feature.menu

import android.content.SharedPreferences

class MenuPreferences(private val preferences: SharedPreferences) {
    /** w9.s0 permits an explicit empty list; the normal popup does not force a Settings tile. */
    fun displayed(): List<Int> = read(DISPLAYED) ?: read(LEGACY) ?: MenuAction.defaultOrder
    fun hidden(): List<Int> = read(HIDDEN).orEmpty()
    fun save(displayed: List<Int>, hidden: List<Int>) {
        preferences.edit().putString(DISPLAYED, displayed.joinToString(","))
            .putString(HIDDEN, hidden.joinToString(",")).remove(LEGACY).apply()
    }

    fun reset() { preferences.edit().remove(DISPLAYED).remove(HIDDEN).remove(LEGACY).apply() }

    private fun read(key: String): List<Int>? = preferences.getString(key, null)?.split(',')?.mapNotNull(String::toIntOrNull)

    companion object {
        private const val DISPLAYED = "displayedmenus"
        private const val HIDDEN = "hiddenmenus"
        private const val LEGACY = "menu.order"
    }
}
