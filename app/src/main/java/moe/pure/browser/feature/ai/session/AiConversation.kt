package moe.pure.browser.feature.ai.session

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import moe.pure.browser.core.lifecycle.NativeLiveEvent
import moe.pure.browser.feature.ai.data.AiProvider
import moe.pure.browser.feature.ai.model.*
import moe.pure.browser.feature.ai.text.AiMessageRenderer
import moe.pure.browser.ui.tasks.NativeViewJobs

/** e0 is the actual parent ViewModel; every scalar/list/event retains its native notification path. */
class AiConversation(context: Context) : ViewModel() {
    internal val app = context.applicationContext
    internal val provider = AiProvider.get(app)
    internal val jobs = NativeViewJobs()
    internal val main = Handler(Looper.getMainLooper())
    internal val topic = MutableLiveData<AiThread?>()
    internal val busy = MutableLiveData<Boolean>()
    internal val attachment = MutableLiveData<AiPageAttachment?>()
    internal val display = MutableLiveData<List<AiMessageDisplay>>()
    internal val rowChanged = MutableLiveData<Int>()
    internal val promptChoices = MutableLiveData<List<AiPromptChoice>>()
    internal val threadList = MutableLiveData<List<AiThread>>()
    internal val page = MutableLiveData<Int>()
    internal val paging = MutableLiveData<Boolean>()
    internal val backRequests = MutableLiveData<Int>()
    internal val historyDirty = NativeLiveEvent<Unit>()
    internal val actions = NativeLiveEvent<Int>()
    internal val dismissRequested = NativeLiveEvent<Unit>()
    internal val notice = NativeLiveEvent<String>()
    internal var backend: AiBackend? = null
    internal val renderer = AiMessageRenderer(provider.markdown)
    @Volatile internal var requestVersion = 0
    internal var system: String? = null
    internal val contextWindow = ArrayList<AiMessage>()
    internal val reads = AiConversationReads(this)
    private val messages = AiConversationMessages(this)
    private val sender = AiConversationSender(this)

    fun selectedModel() = reads.model(backend)
    fun isGenerating() = busy.value == true
    fun readBackend() = reads.backend()
    fun refreshPrompts() = reads.prompts()
    fun selectThread(id: String?) = reads.thread(id)
    fun newThread() = reads.thread(null)
    fun refreshHistory() = reads.history()
    fun showPage(index: Int) { page.value = index }
    fun setPaging(enabled: Boolean) { paging.value = enabled }
    fun requestBack(index: Int) { backRequests.value = index }
    fun requestAction(action: Int) { actions.value = action }
    fun dismiss() { dismissRequested.value = Unit }
    fun selectPrompt(id: String) {
        val before = promptChoices.value ?: return
        if (before.isEmpty()) return
        var changed = false
        val next = before.map { value ->
            val selected = id == value.prompt.id
            if (selected == value.selected) value else { changed = true; AiPromptChoice(value.prompt, selected) }
        }
        if (changed) promptChoices.value = next
    }
    fun removeAttachment() { attachment.value = null; reads.prompts() }
    fun selectModel(id: String) { if (id.isNotEmpty()) provider.preferences.modelId = id; notice.value = id }
    fun send(text: String) = sender.send(text)
    fun stop() { jobs.launch({ provider.client.stopStream() }) { connecting -> if (connecting) busy.value = false } }
    fun clearMessages() = messages.clear()
    fun deleteMessage(id: String?) = messages.delete(id)
    fun deleteThreads(ids: Collection<String>) = messages.deleteThreads(ids)
    fun exportText() = messages.exportText()
    internal fun appendDisplay(vararg values: AiMessageDisplay) { display.value = display.value.orEmpty() + values }
    internal fun addContext(value: AiMessage) {
        if (value.isError || value.content.isEmpty()) return
        if (contextWindow.size > 18) contextWindow.removeAt(0)
        contextWindow.add(value)
    }
    override fun onCleared() { jobs.close(); super.onCleared() }
}
