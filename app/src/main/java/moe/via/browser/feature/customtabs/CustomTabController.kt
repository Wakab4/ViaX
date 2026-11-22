package moe.via.browser.feature.customtabs

import android.app.Activity
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.view.View
import moe.via.browser.feature.customtabs.ui.CustomTabToolbar
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.dialog.BrowserDialog

/** Compact chrome delegates browser work to the existing feature owners through one host. */
class CustomTabController(
    private val activity: Activity,
    private val host: Host,
    val request: CustomTabRequest = CustomTabRequest.from(activity.intent)
) {
    interface Host {
        fun currentUrl(): String
        fun currentTitle(): String
        fun share()
        fun addBookmark()
        fun find()
        fun translate()
        fun scriptsAvailable(): Boolean
        fun showScripts()
        fun openInBrowser()
        fun finish()
    }
    private data class Item(val id: Int, val title: String, val pendingIntent: PendingIntent? = null)
    private val bar = CustomTabToolbar(activity, request, host::finish, ::toolbarAction, ::menu)
    val toolbar: View get() = bar.root
    private var popup: BrowserDialog? = null
    private var disposed = false

    /** Native always displays the host even when the ordinary address preference selects a title. */
    @Suppress("UNUSED_PARAMETER")
    fun pageChanged(url: String, title: String = "") { if (!disposed) bar.page(url) }
    fun recolor(background: Int?, foreground: Int?) { if (!disposed) bar.recolor(background, foreground) }

    fun menu(anchor: View = toolbar) {
        if (disposed) return
        close()
        val items = request.menuItems.mapIndexed { index, item -> Item(index + 1, item.title, item.pendingIntent) }.toMutableList()
        if (request.action != null) items += Item(990, "分享")
        items += Item(994, "添加书签")
        items += Item(993, "页内查找")
        items += Item(996, "翻译")
        if (host.scriptsAvailable()) items += Item(997, "查看脚本")
        items += Item(992, "复制链接")
        val name = activity.applicationInfo.loadLabel(activity.packageManager)
        items += Item(995, "在 $name 中打开")
        val location = IntArray(2); anchor.getLocationOnScreen(location)
        popup = BrowserDialog(activity).items(items.map(Item::title)) { index -> dispatch(items[index]) }
            .anchoredAt(location[0] + anchor.width, location[1] + anchor.height + activity.dp(4)).also { dialog ->
                dialog.setOnDismissListener { if (popup === dialog) popup = null }
                dialog.show()
            }
    }

    private fun toolbarAction() {
        if (disposed) return
        if (request.action == null) host.share()
        else CustomTabPendingAction.send(activity, request.action.pendingIntent, host.currentUrl())
    }
    private fun dispatch(item: Item) {
        if (disposed) return
        item.pendingIntent?.let { CustomTabPendingAction.send(activity, it, host.currentUrl()) }
        when (item.id) {
            990 -> host.share()
            994 -> host.addBookmark()
            993 -> host.find()
            996 -> host.translate()
            997 -> host.showScripts()
            992 -> host.currentUrl().takeIf(String::isNotEmpty)?.let { url ->
                activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("网址", url))
                BrowserMessage(activity).show("已复制网址")
            }
            995 -> if (host.currentUrl().isNotEmpty()) { host.openInBrowser(); host.finish() }
        }
    }
    fun close(): Boolean {
        val showing = popup?.isShowing == true
        popup?.dismiss(); popup = null
        return showing
    }
    fun dispose() { if (!disposed) { disposed = true; close(); bar.close() } }
}
