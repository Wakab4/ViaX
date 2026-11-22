package moe.via.browser.feature.backup.archive

import android.content.Context
import moe.via.browser.core.files.NativeDirectories
import java.io.File
import java.io.IOException
import java.util.UUID

/** m9.n.c prepares a UUID child of the original backup cache directory. */
internal object NativeBackupDirectory {
    fun create(context: Context): File {
        val parent = NativeDirectories.cacheFiles(context, "backup")
            ?: throw NullPointerException("tempDirectory == null || tempDirectory.isEmpty()")
        val directory = File(parent, UUID.randomUUID().toString())
        if (directory.isFile && !directory.delete()) throw IOException("Cannot delete file ${directory.absolutePath}")
        if (directory.exists() || directory.mkdirs()) return directory
        throw IOException("Cannot make directories for ${directory.absolutePath}")
    }
}
