package moe.via.browser.feature.home.ui.favorites

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.home.data.HomeIconStore
import moe.via.browser.feature.home.icons.NativeHomeIconBitmaps
import moe.via.browser.feature.home.model.FavoriteDesign
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.tasks.NativeViewJobs
import java.io.File

/** qa.h0/o decode at 42dp even when the dialog's ImageView is 36dp; pending paths stay raw. */
internal class FavoriteIconEditor(
    private val context: Context,
    private val views: FavoriteIconViews,
    private val design: FavoriteDesign,
    private val jobs: NativeViewJobs,
    pick: () -> Unit,
    private val staged: () -> Unit = {}
) {
    private val icons = HomeIconStore(context)
    private val side = context.dp(42)
    private val radius = (design.radius / 100f * side / 2f).toInt()
    private var pending: String? = null

    init {
        views.image.setOnClickListener { if (views.remove.visibility == View.GONE) pick() }
        views.image.setOnLongClickListener { clear(); true }
        views.remove.setOnClickListener { clear() }
        if (design.iconDisabled) { views.image.visibility = View.GONE; views.remove.visibility = View.GONE }
    }

    fun load(url: String) {
        if (design.iconDisabled) return
        jobs.launch({ icons.load(url, side, side, radius) }, failed = { NativeLog.error(null, it) }, delivered = ::display)
    }

    fun picked(uri: Uri) {
        jobs.launch({ icons.stage(uri) }, failed = { NativeLog.error(null, it) }) { file ->
            if (file != null) { staged(); pending = file.path; loadFile(file.path) }
        }
    }

    private fun clear() { loadFile(""); pending = "" }
    private fun loadFile(path: String) {
        if (design.iconDisabled) return
        jobs.launch({
            if (path.isEmpty()) null else File(path).let { file ->
                if (file.isFile) NativeHomeIconBitmaps.read(file, side, side, radius) else null
            }
        }, failed = { NativeLog.error(null, it) }, delivered = ::display)
    }

    private fun display(bitmap: Bitmap?) {
        views.remove.visibility = if (bitmap == null) View.GONE else View.VISIBLE
        if (bitmap == null) views.image.setImageDrawable(HomeIconPlaceholder.create(context, radius * 2))
        else views.image.setImageBitmap(bitmap)
    }

    fun apply(url: String) {
        val path = pending ?: return
        if (path.isEmpty()) icons.remove(url) else icons.commit(File(path), url)
        pending = null
    }
}
