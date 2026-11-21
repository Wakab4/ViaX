package moe.pure.browser.feature.fonts

import android.content.Context
import android.net.Uri
import moe.pure.browser.core.documents.NativeDocumentAccess
import java.io.File

/** hb.j2.g3 and z8.b1.k/j keep metadata validation separate from the unbounded copy. */
internal class NativeFontImport(private val context: Context, private val fonts: NativeFontStore) {
    data class Result(val code: Int, val entry: FontEntry? = null)
    fun import(uri: Uri): Result {
        val name = NativeDocumentAccess.name(context, uri) ?: return Result(1)
        val extension = name.substringAfterLast('.', "")
        if (listOf("ttf", "otf", "woff", "woff2").none { it.equals(extension, true) }) return Result(1)
        if (NativeDocumentAccess.size(context, uri) > 52428800L) return Result(2)
        val file = File(fonts.directory, name)
        if (file.exists() && !file.delete()) return Result(3)
        val path = NativeFontFiles.fromUri(context, uri, file) ?: return Result(4)
        val entry = fonts.accept(path) ?: return Result(5)
        return Result(0, entry)
    }
}
