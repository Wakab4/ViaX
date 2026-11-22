package moe.via.browser.feature.site.data

import org.json.JSONObject

/** ba.c records and the two historical packed-mask formats supported by Via itself. */
object NativeSiteConfigCodec {
    fun decode(text: String): NativeSiteConfig? = runCatching { decode(JSONObject(text)) }.getOrNull()

    fun decode(json: JSONObject): NativeSiteConfig? {
        val modern = json.has("flags")
        val middle = json.has("uastring") || json.has("uachoice")
        if (!modern && !middle && !json.has("flag")) return null
        val masks = if (modern) json.optInt("enabled") to json.optInt("flags") else legacyMasks(json.optInt("flag"))
        val choice = if (modern || middle) json.optInt("uachoice", -1000) else when (json.optInt("uac")) {
            2 -> -3; 3 -> -4; 4 -> -6; 5 -> -8; 6 -> -999; else -> 0
        }
        val custom = if (modern || middle) json.optString("uastring").takeIf(String::isNotEmpty)
            else if (choice == -999) json.optString("ua").takeIf(String::isNotEmpty) else null
        return NativeSiteConfig(choice, custom?.replace(Regex("[\\t\\n\\r]+"), " ")?.trim { it <= ' ' },
            masks.first, masks.second, if (modern) json.optInt("textsize") else 0)
    }

    fun encode(config: NativeSiteConfig): JSONObject = JSONObject().apply {
        if (!config.customUserAgent.isNullOrEmpty()) put("uastring", config.customUserAgent)
        if (config.userAgentChoice != -1000) put("uachoice", config.userAgentChoice)
        if (config.enabledFlags != 0) put("enabled", config.enabledFlags)
        if (config.textSize > 0) put("textsize", config.textSize)
        put("flags", config.flags)
    }

    private fun legacyMasks(packed: Int): Pair<Int, Int> {
        if (packed == 0 || packed shr 2 == 0) return 0 to 0
        var enabled = 0; var values = 0
        listOf(1, 2, 4, 8, 16, 32).forEachIndexed { index, mask ->
            val pair = packed shr (index * 2) and 3
            if (pair != 0) {
                if (pair and 2 != 0) enabled = enabled or mask
                if (if (mask == 4) pair and 1 == 0 else pair and 1 != 0) values = values or mask
            }
        }
        val clipboard = packed shr 12
        if (clipboard and 3 != 0) {
            if (clipboard == 3) { enabled = enabled or 128; values = values or 128 }
            else { enabled = enabled or 64; if (clipboard == 2) values = values or 64 }
        }
        return enabled to values
    }
}
