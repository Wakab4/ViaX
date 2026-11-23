package moe.via.browser.feature.site.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.webkit.CookieManager
import moe.via.browser.core.logging.NativeLog
import java.io.File
import java.io.IOException

internal data class SharedSiteData(val httpCache: Long, val localStorage: Long, val sessionStorage: Long)

/** Read the default WebView profile owned by the browser process. */
internal class SiteDataFiles(context: Context) {
    private val cache = context.cacheDir
    private val data = File(context.dataDir, "app_webview")
    private val profile = File(data, "Default")

    fun indexedDbBytes(): Map<SiteDataAddress, Long> = buildMap {
        for (root in listOf(data, profile)) {
            for (file in File(root, "IndexedDB").listFiles().orEmpty()) {
                val identifier = file.name.substringBefore(".indexeddb.", "")
                val address = indexedDbAddress(identifier) ?: continue
                val bytes = file.fileBytes()
                if (bytes > 0) put(address, (get(address) ?: 0) + bytes)
            }
        }
    }

    fun cookieBytes(): Map<String, Long> = try {
        CookieManager.getInstance().flush()
        val database = File(profile, "Cookies").takeIf(File::isFile) ?: File(data, "Cookies")
        if (database.isFile) readCookies(database) else emptyMap()
    } catch (error: android.database.SQLException) {
        NativeLog.warn("SiteData", "Read WebView cookie sizes", error)
        emptyMap()
    } catch (error: IOException) {
        NativeLog.warn("SiteData", "Read WebView cookie sizes", error)
        emptyMap()
    }

    private fun readCookies(database: File): Map<String, Long> {
        // Android and WebView use separate SQLite libraries. Query a private temporary copy
        // so the framework connection never acquires or releases locks on WebView's database.
        val snapshot = File.createTempFile("site-cookies-", ".db", cache)
        try {
            val journal = File("${database.path}-journal")
            val wal = File("${database.path}-wal")
            val modified = database.lastModified()
            val walModified = wal.lastModified()
            if (journal.length() > 0) return emptyMap()
            database.copyTo(snapshot, overwrite = true)
            if (wal.isFile) wal.copyTo(File("${snapshot.path}-wal"))
            if (journal.length() > 0 || database.lastModified() != modified || wal.lastModified() != walModified) {
                return emptyMap()
            }

            val now = (System.currentTimeMillis() + WINDOWS_EPOCH_OFFSET_MS) * 1000
            val query = """
                SELECT host_key, SUM(length(CAST(name AS BLOB)) + length(CAST(value AS BLOB))
                    + length(encrypted_value) + length(CAST(path AS BLOB)) + length(CAST(host_key AS BLOB)))
                FROM cookies
                WHERE has_expires = 0 OR expires_utc > ?
                GROUP BY host_key
            """.trimIndent()
            return SQLiteDatabase.openDatabase(snapshot.path, null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS).use { db ->
                db.rawQuery(query, arrayOf(now.toString())).use { rows ->
                    buildMap {
                        while (rows.moveToNext()) {
                            val host = rows.getString(0).trimStart('.')
                            put(host, (get(host) ?: 0) + rows.getLong(1))
                        }
                    }
                }
            }
        } finally {
            SQLiteDatabase.deleteDatabase(snapshot)
        }
    }

    fun sharedUsage() = SharedSiteData(
        httpCache = File(cache, "WebView/Default/HTTP Cache").fileBytes() + File(cache, "WebView/HTTP Cache").fileBytes(),
        localStorage = File(profile, "Local Storage").fileBytes() + File(data, "Local Storage").fileBytes(),
        sessionStorage = File(profile, "Session Storage").fileBytes() + File(data, "Session Storage").fileBytes(),
    )

    private fun indexedDbAddress(identifier: String): SiteDataAddress? {
        val scheme = identifier.substringBefore('_')
        if (scheme != "https" && scheme != "http") return null
        val hostAndPort = identifier.substringAfter('_')
        val port = hostAndPort.substringAfterLast('_', "").toIntOrNull() ?: return null
        val encodedHost = hostAndPort.substringBeforeLast('_')
        if (encodedHost.isEmpty()) return null
        val host = if (encodedHost.startsWith('[')) encodedHost.replace('_', ':') else encodedHost
        return SiteDataAddress.parse("$scheme://$host" + if (port == 0) "" else ":$port")
    }

    private fun File.fileBytes(): Long = walkTopDown().sumOf { if (it.isFile) it.length() else 0L }

    private companion object {
        const val WINDOWS_EPOCH_OFFSET_MS = 11_644_473_600_000L
    }
}
