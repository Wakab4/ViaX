package moe.pure.browser.feature.pdf.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

/** PdfViewer Q/Z: heap/8 KB memory, 20MiB private PNG disk cache and unpadded MD5 page keys. */
internal class PdfBitmapCache(context: Context, private val uri: String) {
    private val memory = object : LruCache<String, Bitmap>(max(8192, (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt())) {
        override fun sizeOf(key: String, value: Bitmap) = if (value.isRecycled) 0 else max(1, value.byteCount / 1024)
    }
    private val folder = File(context.applicationContext.externalCacheDir ?: context.applicationContext.cacheDir, "pdf_viewer")
    private fun key(page: PdfPageRect): String {
        val value = "${uri}_0_${page.index}_${page.bounds.width().roundToInt()}_${page.bounds.height().roundToInt()}"
        return runCatching { BigInteger(1, MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))).toString(16) }
            .getOrElse { "${uri.hashCode()}_0_${page.index}_${page.bounds.width().roundToInt()}_${page.bounds.height().roundToInt()}" }
    }
    @Synchronized fun get(page: PdfPageRect): Bitmap? {
        val key = key(page)
        memory[key]?.takeUnless { it.isRecycled }?.let { return it }
        val file = File(folder, "$key.png")
        if (!file.isFile) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()?.also {
            file.setLastModified(System.currentTimeMillis()); memory.put(key, it)
        }
    }
    @Synchronized fun put(page: PdfPageRect, bitmap: Bitmap) {
        val key = key(page); memory.put(key, bitmap)
        runCatching {
            if (folder.isFile) check(folder.delete())
            check(folder.isDirectory || folder.mkdirs())
            val output = File(folder, "$key.tmp")
            output.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val target = File(folder, "$key.png")
            check(output.renameTo(target))
            val files = folder.listFiles { file -> file.name.endsWith(".png") }.orEmpty().sortedBy { it.lastModified() }
            var bytes = files.sumOf { it.length() }
            for (file in files) { if (bytes <= 20971520L) break; val size = file.length(); if (file.delete()) bytes -= size }
        }.onFailure { moe.pure.browser.core.logging.NativeLog.warn("PdfCache", "Cannot cache page bitmap", it) }
    }
    @Synchronized fun clearMemory() = memory.evictAll()
}
