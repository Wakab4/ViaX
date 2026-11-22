package moe.via.browser.feature.download.model

import android.os.Bundle

data class DownloadDraft(val url: String, val userAgent: String? = null, val disposition: String? = null,
    val mime: String? = null, val referer: String? = null, val name: String? = null, val manual: Boolean = false,
    val length: Long = -1, val overwriteName: Boolean = true, val authorization: String? = null, val payload: String? = null) {
    fun save() = Bundle().apply {
        putString("url", url); putString("agent", userAgent); putString("disposition", disposition)
        putString("mime", mime); putString("referer", referer); putString("name", name); putBoolean("manual", manual)
        putLong("length", length); putBoolean("overwrite", overwriteName); putString("authorization", authorization)
        putString("payload", payload)
    }
    companion object {
        fun restore(bundle: Bundle) = DownloadDraft(bundle.getString("url").orEmpty(), bundle.getString("agent"), bundle.getString("disposition"),
            bundle.getString("mime"), bundle.getString("referer"), bundle.getString("name"), bundle.getBoolean("manual"),
            bundle.getLong("length", -1), bundle.getBoolean("overwrite", true), bundle.getString("authorization"), bundle.getString("payload"))
    }
}
