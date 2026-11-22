package moe.via.browser.feature.backup.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import moe.via.browser.core.archive.NativeZipReader
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.backup.model.ImportMode
import moe.via.browser.feature.home.data.NativeFavoriteRows
import moe.via.browser.feature.home.icons.NativeHomeIconPaths
import moe.via.browser.feature.home.model.HomeFavorite
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

/** m9.f restores only an EOF-complete record batch; blank-line returns also skip icon restoration. */
internal class FavoriteBackupCodec(private val context: Context, db: SQLiteDatabase) {
    private val rows = NativeFavoriteRows(db)

    fun export(): String? {
        val records = rows.all()
        if (records.isEmpty()) return null
        return buildString {
            for (record in records) append(JSONObject().put("title", record.title).put("url", record.url).put("order", record.position)).append('\n')
        }
    }

    fun restore(text: String, mode: ImportMode): Int = restore({ text.reader().buffered() }, mode, null)

    fun restore(file: File, icons: File?, mode: ImportMode): Int =
        if (file.isFile) restore({ BufferedReader(FileReader(file)) }, mode, icons) else 0

    private fun restore(open: () -> BufferedReader, mode: ImportMode, icons: File?): Int {
        var count = 0
        val records = ArrayList<HomeFavorite>()
        try {
            open().use { reader ->
                while (true) {
                    val line = reader.readLine()
                    if (line == null) {
                        if (records.isNotEmpty()) {
                            var clear = mode == ImportMode.REPLACE
                            if (!clear && mode == ImportMode.RECONCILE) {
                                val local = rows.all().size
                                clear = records.size > local || local - records.size < local / 2
                            }
                            if (clear) rows.clear()
                            rows.restoreBatch(records)
                            count = records.size
                        }
                        GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.HOME)
                        break
                    }
                    if (line.isEmpty()) return 0
                    val item = JSONObject(line)
                    val record = HomeFavorite(title = item.getString("title"), url = item.getString("url"), position = item.getInt("order"))
                    if (record.url.isNotEmpty()) records.add(record)
                }
            }
        } catch (error: IOException) { NativeLog.warn(null, error) }
        catch (error: JSONException) { NativeLog.warn(null, error) }
        restoreIcons(icons)
        return count
    }

    private fun restoreIcons(archive: File?) {
        if (archive == null || !archive.isFile) return
        val directory = NativeHomeIconPaths.directory(context)
        if (!directory.exists()) directory.mkdirs()
        try { NativeZipReader.extract(archive, directory.canonicalPath) }
        catch (_: IOException) { NativeZipReader.extract(archive, directory.absolutePath) }
    }
}
