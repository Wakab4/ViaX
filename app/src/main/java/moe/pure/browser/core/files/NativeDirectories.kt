package moe.pure.browser.core.files

import android.content.Context
import java.io.File

/** c1.f/N operate directly and keep partial deletion results. */
internal object NativeDirectories {
    /** c1.v returns an existing path as-is, including an existing non-directory. */
    fun appFiles(context: Context, name: String): File? {
        val directory = context.getExternalFilesDir(name) ?: File(context.filesDir, name)
        return directory.takeIf { it.exists() || it.mkdirs() }
    }

    /** c1.j uses the external cache when the system supplies it. */
    fun cacheFiles(context: Context, name: String): File? {
        val directory = File(context.externalCacheDir ?: context.cacheDir, name)
        return directory.takeIf { it.exists() || it.mkdirs() }
    }

    fun delete(file: File?): Boolean {
        if (file == null || !file.exists()) return true
        if (file.isDirectory) file.listFiles()?.forEach { if (!delete(it)) return false }
        return file.delete()
    }
    fun ensure(file: File?): Boolean {
        if (file == null) return false
        if (file.exists()) {
            if (file.isDirectory) return true
            if (!file.delete()) return false
        }
        return file.mkdirs()
    }
}
