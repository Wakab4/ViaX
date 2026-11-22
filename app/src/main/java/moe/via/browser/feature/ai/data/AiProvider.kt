package moe.via.browser.feature.ai.data

import android.content.Context
import moe.via.browser.feature.ai.network.AiClient
import moe.via.browser.feature.ai.text.AiMarkdownEngine
import moe.via.browser.feature.ai.text.AiMarkdownLinkResolver

/** pa.c/p9.v: one application provider owns the repository, URL links and active transport. */
internal class AiProvider private constructor(context: Context) {
    private val app = context.applicationContext
    val repository = AiRepository(app)
    val preferences = AiPreferences(app)
    val client = AiClient()
    val markdown by lazy { AiMarkdownEngine(app, AiMarkdownLinkResolver) }
    companion object {
        @Volatile private var instance: AiProvider? = null
        fun get(context: Context): AiProvider = instance ?: synchronized(this) {
            instance ?: AiProvider(context).also { instance = it }
        }
    }
}
