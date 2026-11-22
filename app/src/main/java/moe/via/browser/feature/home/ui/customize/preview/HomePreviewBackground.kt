package moe.via.browser.feature.home.ui.customize.preview

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.webkit.WebView
import androidx.fragment.app.Fragment
import moe.via.browser.core.graphics.NativeBitmapImages
import moe.via.browser.core.graphics.NativeDisplaySize
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.home.background.NativeBackgroundFiles
import moe.via.browser.feature.home.background.NativeBitmapDrawable
import moe.via.browser.feature.home.ui.customize.HomeCustomizeState
import moe.via.browser.ui.tasks.NativeLifecycleJobs
import moe.via.browser.storage.preferences.BrowserNightMode
import moe.via.browser.ui.theme.palette
import java.io.File

/** d9.q.y3/z3 preserve the last drawable when a Maybe decode has no value. */
internal class HomePreviewBackground(private val owner: Fragment, private val view: WebView,
    private val model: HomeCustomizeState, private val jobs: NativeLifecycleJobs) {
    private var image: Drawable? = null
    private var alpha = -1
    fun refresh() {
        if (view.parent == null) return
        val context = owner.requireContext()
        val path = model.preferences.backgroundPath
        val file = if (path.isNullOrEmpty()) null else File(path)
        if (file == null || !file.exists()) {
            val color = model.preferences.accentColor
            image = ColorDrawable(if (color == -1) context.palette.canvas else color)
            alpha = -1; bind(); return
        }
        val source = path!!
        val cached = NativeBackgroundFiles.cacheFile(context).absolutePath
        val exists = File(cached).exists()
        val display = NativeDisplaySize.real(context)
        jobs.maybe({
            val bitmap = if (exists) BitmapFactory.decodeFile(cached) else NativeBitmapImages.decodeCrop(source, display.x, display.y)
            bitmap?.let { NativeBitmapDrawable(owner.resources, it) }
        }, failed = { NativeLog.error(null, it) }) { drawable -> image = drawable; alpha = -1; bind() }
        if (!exists) BrowserExecutors.io.execute {
            try { NativeBackgroundFiles.prepare(owner.requireContext(), source) } catch (error: Exception) { NativeLog.warn(null, error) }
        }
    }
    fun bind() {
        val context = owner.requireContext()
        val drawable = image
        if (drawable == null) { view.background = ColorDrawable(context.palette.canvas); return }
        val color = model.preferences.accentColor
        val light = .299 * Color.red(color) + .587 * Color.green(color) + .114 * Color.blue(color) >= 192.0
        val minimum = if (!BrowserNightMode.global(context)) 0 else if (light) 128 else 64
        val value = maxOf(minimum, (model.backgroundDesign.opacity / 100f * 255f).toInt())
        if (alpha == value) return
        alpha = value
        view.background = LayerDrawable(arrayOf(drawable, ColorDrawable(Color.argb(value, 0, 0, 0))))
    }
}
