package moe.pure.browser.feature.backup.data

import android.database.sqlite.SQLiteDatabase
import moe.pure.browser.feature.backup.model.BackupBookmarkRow
import moe.pure.browser.feature.backup.model.ImportMode
import moe.pure.browser.feature.internalpages.GeneratedPageDirtyState
import moe.pure.browser.feature.library.data.NativeBookmarkRows
import moe.pure.browser.feature.library.data.NativeBookmarkSnapshot
import moe.pure.browser.feature.library.format.BookmarkRowFormat
import moe.pure.browser.feature.library.format.NativeBookmarkHtml
import java.io.StringWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.IOException
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.feature.backup.model.BackupFileResult
import moe.pure.browser.feature.backup.model.NativeBackupFile

/** m9.d: Native seconds/TEXT identities are retained; HTML creates UUIDs during parsing. */
internal class BookmarkBackupCodec(db: SQLiteDatabase) {
    private val records = NativeBookmarkRows(db)

    fun export(): String? {
        val snapshot = records.readAll()
        if (snapshot.folders.isEmpty() && snapshot.items.isEmpty()) return null
        val output = StringWriter()
        // A folders-only HTML file is written by p.r, but m9.d does not include it in an archive.
        return output.takeIf { NativeBookmarkHtml.write(it, snapshot) > 0 }?.toString()
    }

    fun exportFile(file: File): BackupFileResult {
        val snapshot = records.readAll()
        val files = ArrayList<NativeBackupFile>()
        var count = 0
        if (snapshot.folders.isEmpty() && snapshot.items.isEmpty()) return BackupFileResult(files, count)
        try {
            FileOutputStream(file).use { output ->
                count = NativeBookmarkHtml.write(OutputStreamWriter(output, Charsets.UTF_8), snapshot)
                if (count > 0) files.add(NativeBackupFile(file))
            }
        } catch (error: IOException) { NativeLog.warn(null, error) }
        return BackupFileResult(files, count)
    }

    fun restore(text: String, mode: ImportMode, baselineMillis: Long): Int =
        restoreSnapshot(NativeBookmarkHtml.read(text.reader()), mode, baselineMillis)

    fun rows(): List<BackupBookmarkRow> = BookmarkRowFormat.export(records.readAll())

    fun restoreRows(rows: List<BackupBookmarkRow>, mode: ImportMode, baselineMillis: Long): Int =
        restoreSnapshot(BookmarkRowFormat.restore(rows), mode, baselineMillis)

    private fun restoreSnapshot(incoming: NativeBookmarkSnapshot, mode: ImportMode, baselineMillis: Long): Int {
        if (incoming.folders.isEmpty() && incoming.items.isEmpty()) return 0
        val local = records.readAll()
        val count = local.items.size
        val remote = incoming.items.size
        val clear = mode == ImportMode.REPLACE || mode == ImportMode.RECONCILE &&
            (remote > count || count - remote < count / 2)
        val baseline = baselineMillis / 1000
        val preserved = if (mode == ImportMode.RECONCILE && clear && baseline != 0L) preserve(local, baseline) else null
        // Native clear, incoming import and preserved import are distinct repository operations.
        if (clear) records.clear()
        val imported = records.importBatch(incoming)
        if (preserved != null) records.importBatch(preserved)
        GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.BOOKMARKS)
        return imported
    }

    private fun preserve(local: NativeBookmarkSnapshot, baseline: Long): NativeBookmarkSnapshot? {
        val items = local.items.filter { it.updatedAtSeconds >= baseline }
        if (items.isEmpty()) return null
        val byId = local.folders.associateBy { it.id }
        val folders = buildList {
            for (item in items) {
                var parent = item.folderId
                while (parent.isNotEmpty()) {
                    val folder = byId[parent] ?: break
                    // m9.d retains repeated ancestors; do not flatten them into new path-generated IDs.
                    add(folder)
                    parent = folder.parentId
                }
            }
        }
        return NativeBookmarkSnapshot(folders, items)
    }
}
