package moe.via.browser.feature.scripts.model

import android.content.Context
import org.json.JSONArray

object BuiltinScripts {
    const val FULLTEXT_ID = -99L
    const val OFFICIAL_FULLTEXT_KEY = "fa6f3153b591e06fbf0170d935ad0afe"
    fun fulltext(context: Context): UserScript {
        val patterns = JSONArray(context.assets.open("scripts/fulltext-patterns.json").bufferedReader().use { it.readText() })
        return UserScript(id = FULLTEXT_ID, key = java.util.UUID.randomUUID().toString().replace("-", ""), name = "",
            content = context.assets.open("scripts/fulltext.user.js").bufferedReader().use { it.readText() },
            matches = List(patterns.length()) { patterns.getString(it) }, runAt = 1, created = 0, updated = 0)
    }
}
