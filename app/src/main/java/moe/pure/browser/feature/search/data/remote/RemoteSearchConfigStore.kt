package moe.pure.browser.feature.search.data.remote

import moe.pure.browser.core.config.RemoteFeatureDocument

import android.content.Context
import java.io.File
import java.util.Locale
import java.util.UUID

/** Worker-only Native features file and its separate uid/last_fetched preferences. */
internal class RemoteSearchConfigStore(context: Context, private val channel: String) {
    private val app = context.applicationContext
    private val preferences = app.getSharedPreferences("features", Context.MODE_PRIVATE)
    private val file = File(app.filesDir, "features")

    data class Cached(val userId: String, val document: RemoteFeatureDocument, val lastFetched: Long) {
        fun needsFetch(): Boolean = document.isEmpty || System.currentTimeMillis() > lastFetched + 60_000L
    }

    fun load(): Cached {
        // r9.j.a creates/loads the identity even when the existing feature cache is fresh.
        val userId = synchronized(identityLock) {
            preferences.getString("uid", null)?.takeIf(String::isNotEmpty) ?: run {
                val suffix = channel.foldIndexed(0) { index, total, value -> total + value.code * (index + 1) }
                val value = UUID.randomUUID().toString().replace("-", "").uppercase(Locale.ROOT) +
                    Integer.toHexString(suffix % 256).uppercase(Locale.ROOT)
                preferences.edit().putString("uid", value).apply()
                value
            }
        }
        val raw = runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
        return Cached(userId, RemoteFeatureDocument.parse(raw), preferences.getLong("last_fetched", 0L))
    }

    fun successful(raw: String) {
        // Native ignores the file writer's boolean and still stamps a HTTP-200 response.
        runCatching { file.writeText(raw, Charsets.UTF_8) }
        preferences.edit().putLong("last_fetched", System.currentTimeMillis()).apply()
    }

    private companion object { val identityLock = Any() }
}
