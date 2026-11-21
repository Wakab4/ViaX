package moe.pure.browser.feature.offline.document

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.net.URLDecoder

data class SavedArchiveEntry(val file: File, val title: String) {
    val url: String get() = "file://${file.parent}/${Uri.encode(file.name)}"
}

/** u9.c.j/c1.D/f.b. Call directory creation, discovery and deletion on the owning IO worker. */
object SavedArchiveFiles {
    fun primaryDirectory(context: Context): File? {
        val directory = primaryPath(context)
        return directory.takeIf { it.exists() || it.mkdirs() }
    }

    fun archiveRoots(context: Context): List<File> = listOfNotNull(primaryPath(context), legacyDirectory(context))

    fun read(context: Context): List<SavedArchiveEntry> {
        val native = mutableListOf<File>()
        primaryDirectory(context)?.listFiles()?.let { native.addAll(it) }
        legacyDirectory(context)?.listFiles()?.let { native.addAll(it) }
        native.sortWith(MODIFIED)
        return native.filter { it.name.endsWith(".mht") }.map { file -> SavedArchiveEntry(file, decodeName(file.name.dropLast(4))) }
    }

    /** Native action18 decodes only the last filename component, not the raw parent path. */
    fun decodePath(url: String): String? {
        if (!url.startsWith("file://")) return null
        val slash = url.lastIndexOf('/')
        if (slash < 7) return null
        return url.substring(7, slash + 1) + Uri.decode(url.substring(slash + 1))
    }

    fun containsArchive(context: Context, url: String): Boolean {
        val path = decodePath(url) ?: return false
        return runCatching {
            val file = File(path).canonicalFile
            file.name.endsWith(".mht") && archiveRoots(context).any { file.parentFile == it.canonicalFile }
        }.getOrDefault(false)
    }

    /** c1.f regards a missing entry as deleted and includes listed directories ending in .mht. */
    fun delete(context: Context, url: String): Boolean {
        if (!containsArchive(context, url)) return false
        val file = File(decodePath(url) ?: return false)
        return runCatching {
            val owned = file.canonicalFile.toPath()
            fun remove(entry: File): Boolean {
                if (java.nio.file.Files.isSymbolicLink(entry.toPath())) return entry.delete()
                if (!entry.exists()) return true
                if (!entry.canonicalFile.toPath().startsWith(owned)) return false
                if (entry.isDirectory) entry.listFiles()?.forEach { if (!remove(it)) return false }
                return entry.delete()
            }
            remove(file)
        }.getOrDefault(false)
    }

    private fun primaryPath(context: Context): File = context.getExternalFilesDir("offline") ?: File(context.filesDir, "offline")

    @Suppress("DEPRECATION")
    private fun legacyDirectory(context: Context): File? = runCatching {
        val values = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
        val original = values.getString("downloaddir", Environment.DIRECTORY_DOWNLOADS)
        val directory = normalizeLegacyDirectory(original)
        if (original != directory) values.edit().putString("downloaddir", directory).apply()
        File(Environment.getExternalStorageDirectory().absolutePath + "/" + directory + "/")
    }.getOrNull()

    /** w9.k.U2 plus c1.m; in particular a content:// preference is not converted into a SAF query. */
    private fun normalizeLegacyDirectory(value: String?): String {
        val downloads = Environment.DIRECTORY_DOWNLOADS
        if (value != null && value != downloads) {
            if (value.startsWith("content://")) return value
            var clean = value.trim { it <= ' ' }.replace(Regex("[\\\\:*?\"<>|]"), "")
            while ("//" in clean) clean = clean.replace("//", "/")
            if (clean.isNotEmpty()) {
                if (clean[0] == File.separatorChar) clean = clean.substring(1)
                if (clean.startsWith(downloads) || clean.length <= downloads.length || clean[downloads.length] == File.separatorChar)
                    return clean
                return downloads + File.separatorChar + clean
            }
        }
        return downloads
    }

    private fun decodeName(value: String): String = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
    private val MODIFIED = Comparator<File> { first, second ->
        val difference = first.lastModified() - second.lastModified()
        if (difference == 0L) 0 else if (difference > 0L) -1 else 1
    }
}
