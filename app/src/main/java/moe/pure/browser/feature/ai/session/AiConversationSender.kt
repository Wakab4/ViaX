package moe.pure.browser.feature.ai.session

import moe.pure.browser.feature.ai.model.AiMessage
import moe.pure.browser.feature.ai.model.AiAssistantMessageDisplay
import moe.pure.browser.feature.ai.model.AiThread
import moe.pure.browser.feature.ai.network.AiErrorText
import moe.pure.browser.feature.ai.network.model.AiWireMessage
import moe.pure.browser.feature.ai.network.AiStreamResult
import moe.pure.browser.feature.ai.prompt.AiPromptVariables

/** e0.x0/z0/p/l. Only the actual request counter guards stream/UI delivery. */
internal class AiConversationSender(private val owner: AiConversation) {
    private val repository get() = owner.provider.repository
    private var lastSave = 0L
    fun send(text: String): Boolean {
        if (text.isEmpty()) return false
        val version = ++owner.requestVersion
        owner.busy.value = true
        var input = text
        val current = owner.topic.value
        val thread = current ?: topic(text, version).also { owner.reads.prompts() }
        if (current != null) input = AiPromptVariables.message(selected(), text)
        val user = AiMessage(threadId = thread.id, role = AiMessage.USER, content = input)
        repository.messages.insert(user); owner.addContext(user)
        val request = buildList {
            owner.system?.takeIf(String::isNotEmpty)?.let { add(AiWireMessage(it, AiMessage.SYSTEM)) }
            owner.contextWindow.forEach { add(AiWireMessage(it.content, it.role)) }
        }
        var assistant = AiMessage(threadId = thread.id, role = AiMessage.ASSISTANT)
        val assistantRow = owner.renderer.display(assistant) as AiAssistantMessageDisplay
        owner.appendDisplay(owner.renderer.display(user), assistantRow)
        val position = owner.display.value!!.lastIndex
        repository.messages.insert(assistant)
        val preferences = owner.provider.preferences
        owner.provider.client.stream(repository.backends.get(preferences.backendId), owner.selectedModel(), request) { result ->
            assistant = update(assistant, result)
            if (version == owner.requestVersion) received(assistant, result, assistantRow, position)
        }
        return true
    }
    private fun selected() = owner.promptChoices.value?.firstOrNull { it.selected }?.prompt
    private fun topic(text: String, version: Int): AiThread {
        val thread = AiThread(preview = AiConversationMessages.abbreviate(text, 200).ifEmpty { " " })
        owner.historyDirty.value = Unit; owner.topic.value = thread
        val attachment = owner.attachment.value
        owner.system = AiPromptVariables.system(selected(), attachment)
        repository.threads.insert(thread)
        repository.messages.insert(AiMessage(threadId = thread.id, role = AiMessage.SYSTEM, content = owner.system.orEmpty()))
        title(text, thread, version)
        if (attachment != null) repository.rememberThread(attachment.url, thread.id)
        return thread
    }
    private fun update(message: AiMessage, result: AiStreamResult): AiMessage {
        var value = if (result.message == null) message else message.copy(content = result.content, reasoning = result.reasoning, updatedAt = System.currentTimeMillis())
        if (result.terminal && result.isError && value.content.isEmpty() && value.reasoning.isNullOrEmpty()) {
            value = value.copy(errorFlags = 1, content = "❗" + AiErrorText.format(owner.app, result.error))
        }
        return value
    }
    private fun received(message: AiMessage, result: AiStreamResult, row: AiAssistantMessageDisplay, position: Int) {
        row.text = owner.renderer.text(message)
        if (result.terminal || System.currentTimeMillis() - lastSave > 2000L) { repository.messages.update(message); lastSave = System.currentTimeMillis() }
        if (result.terminal) {
            val thread = owner.topic.value!!
            thread.updatedAt = System.currentTimeMillis()
            thread.preview = AiConversationMessages.abbreviate(row.text?.toString().orEmpty(), 200).ifEmpty { " " }
            owner.historyDirty.postValue(Unit)
            if (!message.isError) owner.addContext(message)
            repository.threads.update(thread)
        }
        owner.main.post {
            if (result.terminal) owner.busy.value = false
            owner.rowChanged.value = position
        }
    }
    private fun title(input: String, thread: AiThread, version: Int) {
        val request = buildList {
            add(AiWireMessage(TITLE_PROMPT, AiMessage.SYSTEM))
            owner.attachment.value?.let { add(AiWireMessage(it.title, AiMessage.USER)) }
            add(AiWireMessage(input, AiMessage.USER))
        }
        owner.jobs.launch({
            val result = owner.provider.client.complete(repository.backends.get(owner.provider.preferences.backendId), owner.selectedModel(), request)
            val content = result.getOrNull()?.content
            thread.name = if (content.isNullOrEmpty()) AiConversationMessages.abbreviate(input, 20) else content.trim()
            repository.threads.update(thread)
        }) { saved -> if (saved && version == owner.requestVersion) owner.topic.value = owner.topic.value }
    }
    companion object {
        private const val TITLE_PROMPT = "Act as an Expert Title Summarizer. Generate a concise summary title (max 10 words, one sentence) in the same language as the input text. If the input contains proper terms (e.g., 'Java/Python'), prioritize the user's language context over term language. Avoid markdown formatting."
    }
}
