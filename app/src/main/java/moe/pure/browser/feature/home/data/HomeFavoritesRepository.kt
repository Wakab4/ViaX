package moe.pure.browser.feature.home.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import moe.pure.browser.data.BrowserStore
import moe.pure.browser.feature.home.model.HomeFavorite
import moe.pure.browser.feature.home.model.HomeFavoriteTile
import java.io.Closeable
import java.security.MessageDigest

/** y9.b delegates to the row owner; UI flags, undo and model mutation belong to the caller. */
class HomeFavoritesRepository(
    context: Context,
    private val store: BrowserStore = BrowserStore(context.applicationContext),
    private val closeStore: Boolean = true
) : Closeable {
    private val icons = HomeIconStore(context)
    private val db: SQLiteDatabase get() = store.writableDatabase
    private val rows get() = NativeFavoriteRows(db)

    fun all(): List<HomeFavorite> = rows.all()
    fun get(id: Int): HomeFavorite? = rows.get(id)
    fun find(url: String?): HomeFavorite? = rows.find(url)
    fun save(title: String?, url: String?): Int = rows.save(HomeFavorite(title = title, url = url))
    fun save(item: HomeFavorite?): Int = rows.save(item)
    fun move(from: Int, to: Int) = rows.move(from, to)
    fun delete(id: Int) { rows.delete(id) }
    fun deleteUrl(url: String) = rows.deleteUrl(url)

    fun tiles(): List<HomeFavoriteTile> = all().map { item ->
        val title = item.title.trim { it <= ' ' }.replace(Regex("<.*?>"), "")
        HomeFavoriteTile(HomeFavoriteUrls.href(item.url).replace("\"", "&quot;"), title,
            title.takeIf(String::isNotEmpty)?.let { String(Character.toChars(it.codePointAt(0))) }.orEmpty(),
            icons.relativePath(item.url), color(item.url))
    }

    override fun close() { if (closeStore) store.close() }

    companion object {
        fun color(url: String): String {
            val hash = MessageDigest.getInstance("MD5").digest(url.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
            val red = hash.substring(0, 3).toInt(16) % 128 + 90
            val green = hash.substring(hash.length / 2 - 3, hash.length / 2).toInt(16) % 128 + 90
            val blue = hash.takeLast(3).toInt(16) % 128 + 90
            return "#%02x%02x%02x".format(red, green, blue)
        }
    }
}
