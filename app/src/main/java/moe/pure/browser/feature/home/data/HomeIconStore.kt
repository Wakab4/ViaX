package moe.pure.browser.feature.home.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import moe.pure.browser.core.documents.NativeContentFiles
import moe.pure.browser.core.files.NativeDirectories
import moe.pure.browser.feature.home.icons.NativeHomeIconBitmaps
import moe.pure.browser.feature.home.icons.NativeHomeIconFiles
import moe.pure.browser.feature.home.icons.NativeHomeIconPaths
import java.io.File

/** Homepage PNG paths, transforms and file mutations belong to their Native domain owners. */
class HomeIconStore(private val context: Context) {
    private val directory = NativeHomeIconPaths.directory(context)
    fun file(url: String): File = NativeHomeIconPaths.file(context, url)
    fun relativePath(url: String): String = NativeHomeIconPaths.relative(url)
    fun load(url: String, width: Int, height: Int, radius: Int): Bitmap? = NativeHomeIconBitmaps.read(file(url), width, height, radius)
    fun stage(uri: Uri): File? = NativeContentFiles.copyTo(context, uri, File(directory, "cache.png"))?.let(::File)
    fun commit(source: File, url: String) = NativeHomeIconFiles.install(directory, source, url)
    fun remove(url: String) = NativeHomeIconFiles.remove(directory, url)
    fun clear() = NativeDirectories.delete(directory)
    fun retain(urls: Set<String>) = NativeHomeIconFiles.retain(directory, urls)
}
