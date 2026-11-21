package moe.pure.browser.feature.backup.data.export

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.feature.backup.model.BackupFileResult
import moe.pure.browser.feature.backup.model.NativeBackupFile
import moe.pure.browser.feature.home.data.HomeIconStore
import moe.pure.browser.feature.home.data.NativeFavoriteRows
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** m9.f writes favorites first, then stages its independent icon ZIP from a path HashSet. */
internal class FavoritesFileExport(private val context: Context, private val db: SQLiteDatabase) {
    fun write(directory: File): BackupFileResult {
        val records = NativeFavoriteRows(db).all()
        if (records.isEmpty()) return BackupFileResult(emptyList(), 0)
        val files = ArrayList<NativeBackupFile>()
        val favorites = File(directory, "favorites.txt")
        val written = NativeBackupJsonFile.write(favorites, records) { JSONObject().put("title", it.title).put("url", it.url).put("order", it.position) }
        if (written) files.add(NativeBackupFile(favorites))
        val icons = HomeIconStore(context)
        val paths = HashSet<String>()
        records.forEach { paths.add(icons.file(it.url).path) }
        val archive = File(directory, "icons.zip")
        try {
            ZipOutputStream(FileOutputStream(archive)).use { zip ->
                var count = 0
                for (path in paths) {
                    val source = File(path)
                    if (!source.isFile) continue
                    count++
                    try {
                        FileInputStream(source).use { input ->
                            zip.putNextEntry(ZipEntry(source.name))
                            val buffer = ByteArray(1024)
                            while (true) {
                                val size = input.read(buffer)
                                if (size <= 0) break
                                zip.write(buffer, 0, size)
                            }
                            zip.closeEntry()
                        }
                    } catch (error: Exception) { NativeLog.warn(null, error) }
                }
                if (count > 0) files.add(NativeBackupFile(archive))
            }
        } catch (error: IOException) { NativeLog.warn(null, error) }
        return BackupFileResult(files, if (written) records.size else 0)
    }
}
