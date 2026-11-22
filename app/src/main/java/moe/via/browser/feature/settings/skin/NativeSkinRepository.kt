package moe.via.browser.feature.settings.skin

import android.content.Context
import android.net.Uri
import moe.via.browser.core.documents.NativeDocumentAccess
import moe.via.browser.core.archive.NativeZipReader
import java.io.File

/** mb.b and lb.k keep listing/deletion separate from the view and archive decoder. */
internal class NativeSkinRepository(private val context: Context) {
    private val directory = (context.getExternalFilesDir("skins") ?: File(context.filesDir, "skins")).let {
        if (it.exists() || it.mkdirs()) it else null
    }

    fun all(): List<NativeSkinEntry> = directory?.listFiles()?.filter { it.isDirectory }?.map {
        NativeSkinEntry(it.name, it.name, it.lastModified())
    }?.sortedWith { first, second ->
        if (first.modified == second.modified) 0 else if (first.modified > second.modified) -1 else 1
    }.orEmpty()

    fun import(uri: Uri): NativeSkinEntry? {
        var name = NativeDocumentAccess.name(context, uri)
        val size = NativeDocumentAccess.size(context, uri)
        if (name == null || size < 0 || size > 12582912L) return null
        val stream = NativeDocumentAccess.input(context, uri)
        val dot = name.lastIndexOf('.')
        if (dot >= name.length - 5) name = name.substring(0, dot)
        val target = File(directory, name)
        return if (NativeZipReader.extract(stream, target.absolutePath) && target.exists())
            NativeSkinEntry(target.name, target.name, target.lastModified()) else null
    }

    fun delete(name: String): Boolean {
        if (directory == null || name.isEmpty()) return false
        return deleteFile(File(directory, name))
    }

    private fun deleteFile(file: File): Boolean {
        if (!file.exists()) return true
        if (file.isDirectory) {
            val children = file.list()
            if (children != null && children.isNotEmpty()) {
                for (name in children) if (!deleteFile(File(file, name))) return false
            }
        }
        return file.delete()
    }
}
