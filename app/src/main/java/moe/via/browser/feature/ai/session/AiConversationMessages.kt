package moe.via.browser.feature.ai.session

import moe.via.browser.feature.ai.model.AiMessageText

/** e0.B/E/m/G/o/w/n: update the visible state only after the native Boolean result. */
internal class AiConversationMessages(private val owner: AiConversation) {
    private val repository get() = owner.provider.repository
    fun clear() {
        val thread = owner.topic.value ?: return
        owner.jobs.launch({
            if (!repository.messages.clear(thread.id)) false else {
                thread.preview = null; thread.updatedAt = System.currentTimeMillis(); repository.threads.update(thread); true
            }
        }) { cleared -> if (cleared) { owner.reads.thread(thread.id, true); owner.historyDirty.value = Unit } }
    }
    fun delete(id: String?) {
        if (id.isNullOrEmpty()) return
        owner.jobs.launch({
            if (!repository.messages.delete(id)) false else {
                val index = owner.contextWindow.indexOfFirst { it.id == id }; if (index >= 0) owner.contextWindow.removeAt(index); true
            }
        }) { deleted ->
            val before = owner.display.value
            if (!deleted || before.isNullOrEmpty()) return@launch
            val values = before.toMutableList()
            val index = values.indexOfLast { it.id == id }; if (index < 0) return@launch
            val last = index == values.lastIndex; values.removeAt(index)
            val thread = owner.topic.value
            if (last && thread != null) {
                thread.preview = values.lastOrNull()?.let { value -> abbreviate(value.text?.toString().orEmpty(), 200).ifEmpty { " " } }
                repository.threads.update(thread); owner.historyDirty.value = Unit
            }
            owner.display.value = values
        }
    }
    fun deleteThreads(ids: Collection<String>) {
        if (ids.isEmpty()) return
        owner.jobs.launch({
            val current = owner.topic.value
            val selected = current != null && ids.any { it == current.id }
            repository.threads.delete(ids) to selected
        }) { (deleted, selected) -> if (deleted) { owner.reads.history(); if (selected) owner.newThread() } }
    }
    fun exportText(): String? = owner.topic.value?.let { AiMessageText.transcript(repository.messages.forThread(it.id)).takeIf(String::isNotEmpty) }
    companion object { fun abbreviate(value: String, length: Int) = if (value.length > length) value.take(length) + "..." else value }
}
