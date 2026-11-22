package moe.via.browser.feature.ai.data

import android.content.Context
import java.io.Closeable

class AiRepository(context: Context) : Closeable {
    private val store = AiStore(context.applicationContext)
    private val database = AiDatabase(store)
    val backends = AiBackendRepository(database)
    val threads = AiThreadRepository(database)
    val messages = AiMessageRepository(database)
    val prompts = AiPromptRepository(database)

    fun rememberThread(url: String?, threadId: String?) {
        if (!url.isNullOrEmpty() && !threadId.isNullOrEmpty()) AiThreadLinks.links[url] = threadId
    }

    fun threadForUrl(url: String?): String? = if (url.isNullOrEmpty()) null else AiThreadLinks.links[url]

    override fun close() { store.close() }
}

internal object AiThreadLinks {
    val links = HashMap<String, String>()
}
