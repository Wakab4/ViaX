package moe.via.browser.feature.search.resolve

import moe.via.browser.feature.search.model.SearchQuickMatch
import moe.via.browser.feature.search.model.ShortcutResult
import java.util.Locale

object SearchShortcuts {
    fun normalize(raw: String) = raw.trim { it <= ' ' }.lowercase(Locale.ROOT)
    fun validate(raw: String, id: Int, shortcuts: Map<Int, String>): ShortcutResult {
        val word = normalize(raw)
        if (word.codePointCount(0, word.length) > 16 || word.any(Character::isWhitespace)) return ShortcutResult(false, "快捷词无效")
        if (word.isNotEmpty() && shortcuts.any { it.key != id && it.value == word }) return ShortcutResult(false, "该快捷词已被使用")
        return ShortcutResult(true)
    }
    fun match(input: String, shortcuts: Map<Int, String>): SearchQuickMatch? {
        val boundary = input.indexOfFirst(Character::isWhitespace)
        if (boundary <= 0) return null
        val word = normalize(input.substring(0, boundary))
        val id = shortcuts.entries.firstOrNull { it.value.isNotEmpty() && it.value == word }?.key ?: return null
        return SearchQuickMatch(id, input.substring(boundary).trim())
    }
}
