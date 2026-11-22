package moe.via.browser.feature.report

import android.app.Activity
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope

/** The report form has its own lifecycle and submits only after the user presses its positive action. */
class ReportController(
    private val activity: Activity,
    private val currentTitle: () -> String?
) {
    private val dialogs = DialogScope()
    private val messages = BrowserMessage(activity)
    private var disposed = false

    fun show(url: String?): Boolean {
        if (disposed || !supports(url)) return false
        val pageUrl = url ?: return false
        val dialog = BrowserDialog(activity, "举报网站")
            .singleChoice(REASONS, -1)
        val note = dialog.field("", "备注（可不填）", 1)
        dialog.positive("确定") { form ->
            val value = note.text.toString()
            val reason = reasonCode(form.selectedIndices)
            if (value.isNotEmpty() || reason != 0) {
                ReportClient.submit(ReportPayload(pageUrl, reason, currentTitle(), value))
                // Native acknowledges dispatch immediately; there is no response-dependent form state.
                messages.show("举报成功")
            }
            true
        }.negative()
        dialogs.show(dialog)
        return true
    }

    fun updateGeometry() = dialogs.updateGeometry()

    fun dispose() {
        if (disposed) return
        disposed = true
        dialogs.dispose()
        messages.close()
    }

    companion object {
        private val REASONS = listOf("干扰内容太多", "这是危险网站", "页面显示不正确")

        fun supports(url: String?): Boolean = url != null &&
            (url.length > 6 && url.startsWith("http://", true) ||
                url.length > 7 && url.startsWith("https://", true))

        private fun reasonCode(selected: Set<Int>): Int = selected.sumOf { index ->
            // T3 deliberately skips index zero despite the native {1, 2, 4} lookup table.
            if (index in 1..2) intArrayOf(1, 2, 4)[index] else 0
        }
    }
}
