package moe.pure.browser.feature.shortcut

import moe.pure.browser.ui.message.BrowserMessages

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.theme.ThemedActivity

class ShortcutController(private val activity: Activity) {
    private val icons = ShortcutIcons(activity.applicationContext)
    private val shortcuts = PageShortcuts(activity)
    private val dialogs = (activity as? ThemedActivity)?.dialogScope ?: DialogScope()
    private var editor: ShortcutEditor? = null
    private var savedForRestore = false
    private var disposed = false
    private var generation = 0
    private var returnedImage: Uri? = null
    fun show(url: String, title: String, favicon: Bitmap? = null) {
        if (!shortcuts.supported) { toast("操作失败"); return }
        val operation = ++generation
        if (favicon == null) { open(ShortcutDraft(url, title)); return }
        BrowserExecutors.network.execute {
            val token = runCatching { icons.save(favicon) }.getOrNull()
            activity.runOnUiThread {
                if (disposed || operation != generation) icons.remove(token)
                else open(ShortcutDraft(url, title, token, url))
            }
        }
    }
    private fun open(draft: ShortcutDraft) {
        editor?.dismiss()
        val preview = icons.read(draft.icon) ?: icons.fallback(draft.url, draft.title)
        var handingOff = false
        val dialog = ShortcutEditor(activity, draft, preview, ::pick) { ready ->
            handingOff = true
            val operation = generation
            BrowserExecutors.network.execute {
                val result = runCatching {
                    val bitmap = ready.icon?.takeIf { ready.iconFromUrl == null || ready.iconFromUrl == ready.url }?.let(icons::read)
                        ?: if (ready.url == draft.url && ready.title == draft.title) preview else icons.fallback(ready.url, ready.title)
                    val padded = icons.padded(bitmap)
                    activity.runOnUiThread {
                        if (!disposed && operation == generation) {
                            runCatching { shortcuts.pin(ready, padded) }.onSuccess { result ->
                                val host = android.net.Uri.parse(ready.url).host ?: ready.url
                                toast(when (result) {
                                    PageShortcuts.PinResult.REQUESTED -> "已为 $host 创建桌面快捷方式"
                                    PageShortcuts.PinResult.UPDATED -> "已为 $host 更新桌面快捷方式"
                                    PageShortcuts.PinResult.FAILED -> "操作失败"
                                })
                            }.onFailure { toast("操作失败") }
                        }
                    }
                }
                icons.remove(ready.icon)
                if (result.isFailure) activity.runOnUiThread { if (!disposed) toast("操作失败") }
            }
        }
        editor = dialog
        dialogs.show(dialog) {
            if (editor === dialog) editor = null
            if (!savedForRestore && !handingOff) icons.remove(dialog.snapshot().icon)
        }
        importReturnedImage()
    }
    private fun pick() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
        runCatching { @Suppress("DEPRECATION") activity.startActivityForResult(intent, REQUEST) }.onFailure { toast("操作失败") }
    }
    fun onActivityResult(request: Int, result: Int, data: Intent?): Boolean {
        if (request != REQUEST) return false
        val uri = data?.data?.takeIf { result == Activity.RESULT_OK } ?: return true
        returnedImage = uri
        importReturnedImage()
        return true
    }
    private fun importReturnedImage() {
        val dialog = editor ?: return
        val uri = returnedImage ?: return
        returnedImage = null
        BrowserExecutors.network.execute {
            val imported = runCatching { icons.import(uri) }
            activity.runOnUiThread {
                imported.onSuccess { token ->
                    if (disposed || editor !== dialog || !dialog.isShowing) icons.remove(token)
                    else icons.read(token)?.let { bitmap -> icons.remove(dialog.snapshot().icon); dialog.updateIcon(token, bitmap) }
                }.onFailure { if (!disposed) toast("无法读取图片") }
            }
        }
    }
    fun save(): Bundle? { savedForRestore = true; return editor?.takeIf { it.isShowing }?.snapshot()?.save() }
    fun restore(bundle: Bundle?) { bundle?.let(ShortcutDraft::restore)?.let { draft -> activity.window.decorView.post { if (!disposed) open(draft) } } }
    fun onResume() { savedForRestore = false }
    fun dispose() { disposed = true; generation++; editor?.dismiss(); editor = null }
    private fun toast(message: String) = BrowserMessages.show(activity, message)
    companion object { private const val REQUEST = 674 }
}
