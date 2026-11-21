package moe.pure.browser.feature.backup.archive

import java.io.Closeable
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Only encrypted password bytes and explicitly selected archive files are staged; plaintext passwords never are. */
class PreparedBackup internal constructor(private val root: File, val files: Map<String, File>) : Closeable {
    private val closed = AtomicBoolean()
    val encrypted = files["pass.enc"]?.isFile == true
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        val directory = root.canonicalFile
        val parent = directory.parentFile ?: return
        if (parent.name != "backup-staging" || directory.name != root.name) return
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.canonicalFile.parentFile == directory) file.delete()
        }
        directory.delete()
    }
}
