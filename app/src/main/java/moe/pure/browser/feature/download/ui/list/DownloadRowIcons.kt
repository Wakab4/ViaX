package moe.pure.browser.feature.download.ui.list

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Size
import android.widget.ImageView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import moe.pure.browser.feature.download.DownloadCategory
import moe.pure.browser.feature.download.DownloadState
import moe.pure.browser.feature.download.DownloadTask
import moe.pure.browser.feature.settings.skin.NativeSkinIcons
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask

/** Per-list delegate: 64 access-ordered results, including failed reads, and two thumbnail workers. */
internal class DownloadRowIcons(private val context: Context, private val changed: (Long?) -> Unit) : AutoCloseable {
    private data class Key(val id: Long, val target: String, val updated: Long)
    private data class Result(val drawable: Drawable?)
    private val cache = object : LinkedHashMap<Key, Result>(64, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, Result>?) = size > 64
    }
    private val pending = mutableMapOf<Key, FutureTask<Drawable?>>()
    private val workers = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())
    private var closed = false
    private val skin = NativeSkinIcons(context)

    fun bind(view: ImageView, task: DownloadTask, allowLoad: Boolean) {
        val resource = DownloadCategory.iconForMime(task.mime)
        val thumbnail = task.state == DownloadState.COMPLETE && (task.mime.startsWith("image/") ||
            task.mime.startsWith("video/") || task.mime == "application/vnd.android.package-archive")
        val key = Key(task.id, task.target, task.updated)
        val entry = if (thumbnail) cache[key] else null
        if (entry?.drawable != null) {
            view.imageTintList = null
            view.setImageDrawable(entry.drawable)
        } else {
            view.imageTintList = ColorStateList.valueOf(context.palette.icon)
            view.setImageDrawable((skin.resolve(resource) ?: context.getDrawable(resource))?.mutate())
            if (thumbnail && entry == null && allowLoad && !closed && key !in pending) load(key, task.mime)
        }
    }

    private fun load(key: Key, mime: String) {
        val app = context.applicationContext
        val size = context.dp(24)
        val radius = context.dp(2).toFloat()
        val owner = WeakReference(this)
        val ui = main
        val job = FutureTask { read(app, key.target, mime, size, radius) }
        pending[key] = job
        workers.execute {
            job.run()
            val result = try { job.get() } catch (_: Exception) { null }
            ui.post { owner.get()?.complete(key, result) }
        }
    }

    private fun complete(key: Key, drawable: Drawable?) {
        if (closed || pending.remove(key) == null) return
        cache[key] = Result(drawable)
        if (drawable != null) changed(key.id)
    }

    override fun close() {
        if (closed) return
        closed = true
        pending.values.forEach { it.cancel(true) }; pending.clear()
        workers.shutdownNow(); cache.clear(); main.removeCallbacksAndMessages(null)
    }

    companion object {
        private fun read(context: Context, target: String, mime: String, size: Int, radius: Float): Drawable? = try {
            val uri = Uri.parse(target)
            if (mime == "application/vnd.android.package-archive") {
                if (uri.scheme == "file") packageIcon(context, uri.path, size)
                else context.contentResolver.openFileDescriptor(uri, "r")?.use {
                    packageIcon(context, "/proc/self/fd/${it.fd}", size)
                }
            } else {
                val bitmap = if (Build.VERSION.SDK_INT >= 29) context.contentResolver.loadThumbnail(uri, Size(size, size), null)
                else legacyThumbnail(context, uri, mime.startsWith("image/"))
                bitmap?.let {
                    RoundedBitmapDrawableFactory.create(context.resources, it).apply {
                        cornerRadius = radius; setAntiAlias(true)
                    }
                }
            }
        } catch (_: Exception) { null }

        @Suppress("DEPRECATION")
        private fun packageIcon(context: Context, path: String?, size: Int): Drawable? {
            if (path == null) return null
            val manager = context.packageManager
            val info = manager.getPackageArchiveInfo(path, 0)?.applicationInfo ?: return null
            info.sourceDir = path; info.publicSourceDir = path
            return info.loadIcon(manager)?.apply { setBounds(0, 0, size, size) }
        }

        @Suppress("DEPRECATION")
        private fun legacyThumbnail(context: Context, uri: Uri, image: Boolean): Bitmap? {
            val id = context.contentResolver.query(uri, arrayOf("_id"), null, null, null)?.use {
                val index = it.getColumnIndex("_id")
                if (it.moveToFirst() && index >= 0) it.getLong(index) else 0L
            } ?: 0L
            if (id <= 0) return null
            return if (image) MediaStore.Images.Thumbnails.getThumbnail(context.contentResolver, id, 1, null)
            else MediaStore.Video.Thumbnails.getThumbnail(context.contentResolver, id, 1, null)
        }
    }
}
