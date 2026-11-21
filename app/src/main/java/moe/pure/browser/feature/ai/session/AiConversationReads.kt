package moe.pure.browser.feature.ai.session

import moe.pure.browser.feature.ai.model.AiBackend
import moe.pure.browser.feature.ai.model.AiMessage
import moe.pure.browser.feature.ai.model.AiMessageDisplay
import moe.pure.browser.feature.ai.model.AiPrompt
import moe.pure.browser.feature.ai.model.AiPromptChoice

/** e0.q/C/u0 and s0/v0/w0 are independent cached-IO subscriptions, without view generations. */
internal class AiConversationReads(private val owner: AiConversation) {
    private val repository get() = owner.provider.repository
    fun model(backend: AiBackend?): String? = owner.provider.preferences.modelId?.takeIf { backend?.models?.contains(it) == true } ?: backend?.firstModel
    fun backend() { owner.jobs.launch({ repository.backends.get(owner.provider.preferences.backendId) }) { value -> owner.backend = value } }
    fun history() { owner.jobs.launch({ repository.threads.all() }) { values -> owner.threadList.value = values } }
    fun prompts() {
        owner.jobs.launch({
            val type = if (owner.topic.value == null) AiPrompt.TYPE_SYSTEM else AiPrompt.TYPE_MESSAGE
            val page = owner.topic.value == null && owner.attachment.value != null
            repository.prompts.all(type).filter { it.requiresPage == page }.let { values ->
                if (values.isEmpty()) emptyList() else listOf(AiPromptChoice(AiPrompt(id = AiPrompt.DEFAULT_ID, name = "Casual chat", type = type), true)) + values.map { AiPromptChoice(it, false) }
            }
        }) { values -> owner.promptChoices.value = values }
    }
    fun thread(id: String?, force: Boolean = false) {
        val current = owner.topic.value
        if (!force && (id.isNullOrEmpty() && current == null || !id.isNullOrEmpty() && current != null && id == current.id)) return
        owner.requestVersion++; owner.stop()
        if (id.isNullOrEmpty()) {
            owner.system = null; owner.contextWindow.clear()
            owner.topic.value = null; owner.display.value = emptyList(); owner.busy.value = false; prompts(); return
        }
        owner.jobs.launch({
            val found = repository.threads.get(id)
            val all = repository.messages.forThread(id)
            owner.system = null
            var first = 0
            for (index in all.indices) if (all[index].role == AiMessage.SYSTEM) { owner.system = all[index].content; first = index; break }
            owner.contextWindow.clear()
            val start = first + 1
            val last = maxOf(start, all.size - 18)
            val visible = ArrayList<AiMessageDisplay>()
            for (index in start until all.size) {
                val value = all[index]; visible.add(owner.renderer.display(value))
                if (index >= last && !value.isError && value.content.isNotEmpty()) owner.contextWindow.add(value)
            }
            found to visible
        }) { (found, visible) ->
            if (found != null) { owner.topic.value = found; owner.display.value = visible; owner.busy.value = false }
            prompts()
        }
    }
}
