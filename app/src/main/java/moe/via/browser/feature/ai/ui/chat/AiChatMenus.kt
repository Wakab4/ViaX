package moe.via.browser.feature.ai.ui.chat

import android.view.View
import androidx.lifecycle.Lifecycle
import moe.via.browser.R
import moe.via.browser.feature.ai.model.AiMessageDisplay
import moe.via.browser.feature.ai.model.AiMessageText
import moe.via.browser.feature.ai.session.AiConversation
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.tasks.NativeLifecycleJobs

internal class AiChatMenus(
    private val activity: ThemedActivity,
    private val scope: DialogScope,
    private val conversation: AiConversation,
    lifecycle: () -> Lifecycle,
    private val preview: (AiMessageDisplay) -> Unit,
    private val settings: () -> Unit,
    private val onExport: () -> Unit
) : AutoCloseable {
    private val copyJobs = NativeLifecycleJobs(lifecycle)
    fun more(anchor: View) {
        if (conversation.display.value.isNullOrEmpty()) { models(anchor); return }
        scope.show(BrowserDialog(activity).items(listOf(conversation.selectedModel()?.takeIf(String::isNotEmpty) ?: text(R.string.ref_ai_dialog_select_model), text(R.string.ref_ai_dialog_export_chat), text(R.string.ref_ai_dialog_clear_messages))) {
            when (it) {
                0 -> models(anchor)
                1 -> export()
                2 -> scope.show(BrowserDialog(activity, text(R.string.ref_ai_dialog_clear_messages)).items(listOf(text(R.string.ref_ai_dialog_confirm_clear))) {
                    conversation.clearMessages()
                }.anchoredTo(anchor))
            }
        }.anchoredTo(anchor))
    }

    private fun models(anchor: View) {
        val models = conversation.backend?.models.orEmpty()
        if (models.isEmpty()) { settings(); return }
        if (models.size == 1) return
        val selected = models.indexOf(conversation.selectedModel())
        scope.show(BrowserDialog(activity, text(R.string.ref_ai_dialog_select_model)).singleChoice(models, selected) { position ->
            if (position != selected) conversation.selectModel(models[position])
        }.anchoredTo(anchor))
    }

    fun message(value: AiMessageDisplay, action: Int, anchor: View) {
        when (action) {
            AiMessageAdapter.COPY -> copy(value.id)
            AiMessageAdapter.PREVIEW -> preview(value)
            AiMessageAdapter.DELETE -> deleteMessage(value)
            AiMessageAdapter.MENU -> scope.show(BrowserDialog(activity).items(listOf(text(R.string.ref_ai_dialog_copy_all), text(R.string.ref_ai_dialog_preview), text(R.string.ref_ai_dialog_delete))) { index ->
                message(value, listOf(AiMessageAdapter.COPY, AiMessageAdapter.PREVIEW, AiMessageAdapter.DELETE)[index], anchor)
            }.anchoredTo(anchor))
        }
    }

    private fun deleteMessage(message: AiMessageDisplay) {
        val source = message.text
        val summary = (if (source != null && source.length > 50) source.subSequence(0, 50).toString() + "..." else source?.toString().orEmpty()).replace('\n', ' ')
        scope.show(BrowserDialog(activity, text(R.string.ref_ai_dialog_delete), activity.getString(R.string.ref_ai_dialog_delete_message, summary))
            .positive(text(android.R.string.ok)) { conversation.deleteMessage(message.id); true }.negative(text(android.R.string.cancel)))
    }

    /** F3 applies observeOn(IO), then observeOn(main); the lookup itself remains on the caller. */
    private fun copy(id: String?) {
        copyJobs.fromCaller({ if (id.isNullOrEmpty()) null else conversation.provider.repository.messages.get(id)?.let(AiMessageText::markdown) }) { text ->
            if (text != null) activity.copyAiText(text)
        }
    }

    private fun export() {
        onExport()
    }
    private fun text(id: Int) = activity.getString(id)
    override fun close() { copyJobs.close() }
}
