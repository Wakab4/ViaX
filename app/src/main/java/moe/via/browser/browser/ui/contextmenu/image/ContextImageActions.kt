package moe.via.browser.browser.ui.contextmenu.image

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.WebView
import androidx.core.content.FileProvider
import moe.via.browser.R
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.search.resolve.SearchUrlResolver
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogAnchor
import moe.via.browser.ui.dialog.DialogScope
import java.io.File

/** These commands fetch real bytes and only report success after their corresponding operation. */
class ContextImageActions(private val activity: Activity, private val dialogs: DialogScope, private val host: Host) {
    interface Host {
        fun open(url: String)
        fun openText(text: String)
        fun requestStoragePermission(onAllowed: () -> Unit)
    }
    private val loader = ContextImageLoader(activity, ::read)
    private val message = BrowserMessage(activity)
    private var disposed = false

    fun downloaded(view: WebView, url: String?, data: String?): Boolean = loader.downloaded(view, url, data)

    fun share(view: WebView, url: String) = request(view, url, ContextImageOperations.SHARE)

    private fun share(image: ContextImage) {
        if (image.file.exists()) {
            try {
                val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", image.file)
                activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = image.mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }, activity.getString(R.string.ref_library_context_share)))
            } catch (_: Exception) { }
        }
    }

    fun save(view: WebView, url: String) {
        if (Build.VERSION.SDK_INT <= 28 && activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            host.requestStoragePermission { if (!disposed) save(view, url) }
            return
        }
        request(view, url, ContextImageOperations.SAVE)
    }

    private fun save(image: ContextImage) {
        BrowserExecutors.network.execute {
                val result = runCatching { saveToPictures(image) }
                activity.runOnUiThread {
                    if (disposed || activity.isDestroyed) return@runOnUiThread
                    result.onSuccess { uri -> message.show("图片已保存到图库", "查看") { viewSaved(uri, image.mime) } }
                        .onFailure { message.show("操作失败") }
                }
        }
    }

    fun scan(view: WebView, url: String) = request(view, url, ContextImageOperations.SCAN)

    private fun scan(image: ContextImage) {
        BrowserExecutors.network.execute {
            val result = runCatching {
                val bitmap = BitmapFactory.decodeFile(image.file.absolutePath) ?: return@runCatching null
                try { ContextQrDecoder.decode(bitmap) } finally { if (!bitmap.isRecycled) bitmap.recycle() }
            }
            activity.runOnUiThread {
                if (disposed || activity.isDestroyed) return@runOnUiThread
                val text = result.getOrNull()
                if (text.isNullOrEmpty()) message.show("未检测到二维码") else showScanResult(text)
            }
        }
    }

    fun reverseSearch(url: String, point: DialogAnchor) {
        if (disposed) return
        val providers = buildList {
            add(ImageSearchProvider("谷歌", "https://www.google.com/searchbyimage?safe=off&image_url="))
            add(ImageSearchProvider("谷歌 Lens", "https://lens.google.com/uploadbyurl?url="))
            add(ImageSearchProvider("必应", "https://www.bing.com/images/search?view=detailv2&iss=sbi&q=imgurl:"))
            add(ImageSearchProvider("TinEye", "https://tineye.com/search/?url="))
            add(ImageSearchProvider("Yandex", "https://yandex.com/images/touch/search?family=yes&rpt=imageview&url="))
            add(ImageSearchProvider("SauceNAO", "https://saucenao.com/search.php?db=999&url="))
            add(ImageSearchProvider("IQDB", "https://iqdb.org/?url="))
            add(ImageSearchProvider("3D IQDB", "https://3d.iqdb.org/?url="))
            add(ImageSearchProvider("WhatAnime", "https://trace.moe/?url="))
            add(ImageSearchProvider("Ascii2D", "https://ascii2d.net/search/url/"))
        }
        dialogs.show(BrowserDialog(activity, "以图搜图").items(providers.map { it.label }) { index ->
            host.open(providers[index].prefix + Uri.encode(url))
        }.anchoredAt(point.x, point.y))
    }

    fun dispose() { disposed = true; loader.dispose(); message.close() }

    private fun request(view: WebView, url: String, operations: Int) {
        if (disposed || url.isEmpty()) return
        loader.load(view, url, operations) { message.show("稍等片刻…") }
    }

    private fun read(result: Result<ContextImageResult>) {
        if (!disposed && !activity.isDestroyed) result.onSuccess(::dispatch).onFailure { message.show("操作失败") }
    }

    private fun dispatch(result: ContextImageResult) {
        if (result.operations and ContextImageOperations.SAVE != 0) {
            loader.consume(result.url, ContextImageOperations.SAVE); save(result.image)
        }
        if (result.operations and ContextImageOperations.SHARE != 0) {
            loader.consume(result.url, ContextImageOperations.SHARE); share(result.image)
        }
        if (result.operations and ContextImageOperations.SCAN != 0) {
            loader.consume(result.url, ContextImageOperations.SCAN); scan(result.image)
        }
    }

    private fun showScanResult(text: String) {
        dialogs.show(BrowserDialog(activity, "二维码扫描结果", text)
            .negative().neutral("复制") {
                activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("文本", text))
                message.show("已复制文本"); true
            }.positive(if (SearchUrlResolver.isAddress(text)) "新标签打开" else "搜索") { host.openText(text); true })
    }

    private fun saveToPictures(image: ContextImage): Uri {
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = activity.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, image.name)
                put(MediaStore.Images.Media.MIME_TYPE, image.mime.takeIf { it.startsWith("image/") } ?: "image/*")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Via")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }) ?: error("无法创建图片")
            try {
                resolver.openOutputStream(uri)?.use { output -> image.file.inputStream().use { it.copyTo(output) } }
                    ?: error("无法保存图片")
                check(resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null) > 0)
                return uri
            } catch (error: Throwable) { resolver.delete(uri, null, null); throw error }
        }
        @Suppress("DEPRECATION")
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Via")
        check(directory.mkdirs() || directory.isDirectory)
        var target = File(directory, image.name)
        var index = 1
        while (target.exists()) {
            target = File(directory, image.name.substringBeforeLast('.', image.name) + " (${index++})." + image.name.substringAfterLast('.', "png"))
        }
        try { image.file.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } } }
        catch (error: Throwable) { target.delete(); throw error }
        MediaScannerConnection.scanFile(activity, arrayOf(target.absolutePath), arrayOf(image.mime), null)
        return Uri.fromFile(target)
    }

    private fun viewSaved(uri: Uri, mime: String) {
        runCatching {
            val target = if (uri.scheme.equals("file", true))
                FileProvider.getUriForFile(activity, "${activity.packageName}.provider", File(uri.path.orEmpty())) else uri
            activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(target, mime).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        }.onFailure { message.show("操作失败") }
    }

    private data class ImageSearchProvider(val label: String, val prefix: String)
}
