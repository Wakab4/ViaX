package moe.pure.browser.feature.adblock.data

import moe.pure.browser.feature.adblock.FilterSubscription
import org.json.JSONObject

/** w4.c's line format only stores enabled and size when filePath is present. */
internal object NativeFilterSubscriptionCodec {
    fun parse(line: String): FilterSubscription? {
        val item = FilterSubscription("")
        try {
            val value = JSONObject(line)
            if (value.isNull("url")) return null
            val url = value.optString("url", "").trim { it <= ' ' }
            if (url.isEmpty()) return null
            item.url = url
            if (!value.isNull("filePath")) {
                item.filePath = value.getString("filePath")
                if (!value.isNull("enabled")) item.enabled = value.getBoolean("enabled")
                if (!value.isNull("size")) item.count = value.getInt("size")
            }
            if (!value.isNull("title")) item.title = value.getString("title")
            if (!value.isNull("homepage")) item.homepage = value.getString("homepage")
            if (!value.isNull("license")) item.license = value.getString("license")
        } catch (error: Exception) { moe.pure.browser.core.logging.NativeLog.warn("FilterSubscriptions", "Read subscription: ${error.javaClass.simpleName}") }
        return item
    }
    fun encode(item: FilterSubscription): String = JSONObject().apply {
        put("title", item.title); put("url", item.url); put("homepage", item.homepage)
        put("license", item.license); put("filePath", item.filePath)
        if (!item.filePath.isNullOrEmpty()) { put("enabled", item.enabled); put("size", item.count) }
    }.toString()
}
