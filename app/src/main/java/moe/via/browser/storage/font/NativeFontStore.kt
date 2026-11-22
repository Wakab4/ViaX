package moe.via.browser.storage.font

import android.content.Context
import android.graphics.Typeface
import android.util.LruCache
import android.webkit.WebResourceResponse
import moe.via.browser.core.mime.NativeMimeTypes
import java.io.File
import java.io.FileInputStream

/** s9.d owns the font directory and four-face cache. */
internal class NativeFontStore private constructor(context: Context) {
    val directory: File = (context.getExternalFilesDir("font") ?: File(context.filesDir, "font")).also {
        require(it.exists() || it.mkdirs()) { "fontDirectory == null || fontDirectory.isEmpty()" }
    }
    private val faces = LruCache<String, Typeface>(4)

    fun all(): List<FontEntry> = buildList {
        add(FontEntry(""))
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles { _, name -> name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".woff") || name.endsWith(".woff2") }
                ?.sortedWith { left, right -> left.name.compareTo(right.name, ignoreCase = true) }
                ?.forEach { add(FontEntry(it.absolutePath)) }
        }
    }
    fun accept(path: String): FontEntry? {
        if (path.isEmpty()) return null
        val source = File(path)
        if (!source.exists()) return null
        if (directory.path != source.parent && !NativeFontFiles.copy(source, directory)) return null
        faces.remove(source.name)
        return FontEntry(File(directory, source.name).absolutePath)
    }
    fun delete(name: String): Boolean {
        if (name.isEmpty()) return false
        val file = File(directory, name)
        if (!file.exists() || !file.delete()) return false
        faces.remove(name)
        return true
    }
    fun rename(name: String, requested: String): FontEntry? {
        if (name.isEmpty() || requested.isEmpty() || name == requested) return null
        val source = File(directory, name)
        if (!source.isFile) return null
        val target = File(directory, NativeFontNames.available(directory, NativeFontNames.clean(requested)))
        return if (source.renameTo(target)) FontEntry(target.absolutePath) else null
    }
    fun typeface(name: String): Typeface? {
        if (name.isEmpty()) return Typeface.DEFAULT
        faces[name]?.let { return it }
        val file = File(directory, name)
        if (!file.exists()) return null
        val face = try { Typeface.createFromFile(file) } catch (error: Exception) { error.printStackTrace(); null }
            ?: Typeface.DEFAULT
        faces.put(name, face)
        return face
    }
    fun response(name: String): WebResourceResponse? {
        if (name.isEmpty()) return null
        val file = File(directory, name)
        if (!file.isFile) return null
        return try {
            val input = FileInputStream(file)
            val mime = NativeMimeTypes.mime(name.substringAfterLast('.', ""), "font/ttf")!!
            WebResourceResponse(mime, "UTF-8", input).apply {
                responseHeaders = mapOf("Cache-Control" to "immutable", "Access-Control-Allow-Origin" to "*", "Content-Type" to mime)
            }
        } catch (error: Exception) { error.printStackTrace(); null }
    }
    companion object {
        @Volatile private var instance: NativeFontStore? = null
        fun get(context: Context) = instance ?: synchronized(this) {
            instance ?: NativeFontStore(context.applicationContext).also { instance = it }
        }
    }
}
