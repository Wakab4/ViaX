package moe.pure.browser.feature.ai.ui.chat

import moe.pure.browser.R
import moe.pure.browser.feature.ai.model.AiThread
import moe.pure.browser.feature.ai.session.AiConversation
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.theme.ThemedActivity

/** r2.q3 captures selected IDs and uses the raw topic name in its single-item prompt. */
internal class AiHistoryMenus(private val activity: ThemedActivity, private val scope: DialogScope,
    private val conversation: AiConversation, private val finishSelection: () -> Unit) {
    fun deleteThreads(threads: List<AiThread>) {
        if (threads.isEmpty()) return
        val ids = threads.map(AiThread::id)
        val message = if (threads.size == 1) activity.getString(R.string.ref_ai_dialog_delete_message, threads[0].name)
            else activity.getString(R.string.ref_ai_dialog_delete_many, threads.size)
        scope.show(BrowserDialog(activity, activity.getString(R.string.ref_ai_dialog_delete), message)
            .positive(activity.getString(android.R.string.ok)) { finishSelection(); conversation.deleteThreads(ids); true }
            .negative(activity.getString(android.R.string.cancel)))
    }
}
