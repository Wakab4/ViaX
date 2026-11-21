package moe.pure.browser.feature.search

import android.content.ClipData
import android.content.ClipboardManager
import android.view.View
import moe.pure.browser.feature.search.resolve.SearchUrlResolver
import moe.pure.browser.ui.components.BrowserMessage
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity

/** Clipboard reads happen only after an explicit paste action. */
internal class SearchAddressActions(private val activity: ThemedActivity, private val editor: (String) -> Unit,
    private val submit: (String, Boolean) -> Unit, private val batch: (List<String>) -> Unit) {
    private val messages = BrowserMessage(activity)

    fun show(anchor: View, url: String, internal: Boolean) {
        val copy = url.isNotEmpty() && !internal
        val actions = if (copy) listOf("复制", "粘贴", "粘贴并前往") else listOf("粘贴", "粘贴并前往")
        activity.dialogScope.show(BrowserDialog(activity).items(actions) { index ->
            when (actions[index]) {
                "复制" -> {
                    activity.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, url))
                    messages.show("已复制链接")
                }
                "粘贴" -> clipboard()?.takeIf(String::isNotEmpty)?.let(editor)
                "粘贴并前往" -> clipboard()?.takeIf(String::isNotEmpty)?.let(::pasteAndGo)
            }
        }.anchoredTo(anchor))
    }

    private fun clipboard() = runCatching {
        activity.getSystemService(ClipboardManager::class.java)?.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.text?.toString()
    }.getOrNull()

    private fun pasteAndGo(text: String) {
        val links = SearchUrlResolver.httpLinks(text)
        when (links.size) {
            0 -> submit(text, false)
            1 -> submit(links.first(), false)
            else -> activity.dialogScope.show(BrowserDialog(activity, "提示", text)
                .items(links) { submit(links[it], true) }
                .positive("打开全部") { batch(links); true }
                .negative())
        }
    }

    fun dispose() = messages.close()
}
