package moe.via.browser.feature.scan

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.surface
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QrCodeDialog(private val activity: Activity,
    private val permission: (List<String>, (Set<String>) -> Unit) -> Unit) {
    private var current: Dialog? = null
    private val notice = BrowserMessage(activity)
    fun show(text: String) {
        if (text.isEmpty()) return
        if (text.length > 1024) {
            notice.show(activity.getString(R.string.ref_qr_prompt_too_long), activity.getString(R.string.ref_qr_prompt_copy)) {
                activity.getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(android.content.ClipData.newPlainText("网址", text))
                BrowserMessages.show(activity, R.string.ref_qr_prompt_copied)
            }
            return
        }
        val width = minOf((minOf(activity.resources.displayMetrics.widthPixels, activity.resources.displayMetrics.heightPixels) * .72f).toInt(), activity.dp(420))
        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL; background = activity.surface(radius = 18); clipToOutline = true
        }
        val image = ImageView(activity).apply { setPadding(dp(6), dp(6), dp(6), 0); contentDescription = "二维码" }
        body.addView(image, LinearLayout.LayoutParams(-1, width))
        val save = NativeTypography.text(activity).apply {
            this.text = "保存图片"; textSize = 14f; NativeTypography.style(this, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER; setTextColor(activity.palette.accent)
            setPadding(0, dp(14), 0, dp(14)); background = activity.touchFeedback(); isEnabled = false
        }
        body.addView(save, LinearLayout.LayoutParams(-1, -2))
        val dialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE); setContentView(body)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        current?.dismiss(); current = dialog
        dialog.setOnDismissListener { if (current === dialog) current = null }
        dialog.show(); dialog.window?.setLayout(width, -2)
        BrowserExecutors.persistence.execute {
            var size = 512
            while (size < text.length) size *= 2
            val result = runCatching { QrCodec.encode(text, size) }
            activity.runOnUiThread {
                if (activity.isDestroyed || !dialog.isShowing) return@runOnUiThread
                result.onSuccess { bitmap ->
                    image.setImageBitmap(bitmap); save.isEnabled = true
                    save.setOnClickListener {
                        if (Build.VERSION.SDK_INT >= 29) save(bitmap, save, dialog)
                        else permission(listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)) { allowed ->
                            if (Manifest.permission.WRITE_EXTERNAL_STORAGE in allowed) save(bitmap, save, dialog)
                        }
                    }
                }.onFailure { dialog.dismiss(); message("操作失败") }
            }
        }
    }

    private fun save(bitmap: Bitmap, button: TextView, dialog: Dialog) {
        button.isEnabled = false
        BrowserExecutors.persistence.execute {
            val result = runCatching {
                val name = "qrcode-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())}.png"
                if (Build.VERSION.SDK_INT >= 29) {
                    val resolver = activity.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name); put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS); put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }) ?: error("无法创建图片")
                    try {
                        resolver.openOutputStream(uri)!!.use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                        resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                    } catch (error: Exception) { resolver.delete(uri, null, null); throw error }
                } else {
                    @Suppress("DEPRECATION")
                    val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).apply { mkdirs() }
                    var file = File(directory, name); var index = 1
                    while (file.exists()) { file = File(directory, name.removeSuffix(".png") + "-${index++}.png") }
                    file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                    MediaScannerConnection.scanFile(activity, arrayOf(file.absolutePath), arrayOf("image/png"), null)
                }
            }
            activity.runOnUiThread {
                if (!activity.isDestroyed) {
                    message(if (result.isSuccess) "二维码已保存到相册" else "操作失败")
                    if (result.isSuccess) dialog.dismiss() else button.isEnabled = true
                }
            }
        }
    }
    private fun message(message: String) = BrowserMessages.show(activity, message)
    fun dispose() { current?.dismiss(); current = null; notice.close() }
}
