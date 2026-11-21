package moe.pure.browser.feature.ai.prompt

import moe.pure.browser.feature.ai.model.AiBackend
import java.net.URI
import java.util.Locale
import java.util.UUID

object AiTemplates {
    fun isChinaRegion(): Boolean = Locale.getDefault().country.equals("CN", ignoreCase = true)

    /** The IDs identify static presets; get() creates a configuration with its own UUID. */
    fun providers(chinaRegion: Boolean = isChinaRegion()): List<AiBackend> {
        val byId = listOf(
            preset("CHATGPT", "ChatGPT", "https://api.openai.com/v1", "gpt-4.1-mini", "o4-mini", "o3"),
            preset("OPENROUTER", "OpenRouter", "https://openrouter.ai/api/v1",
                "deepseek/deepseek-chat-v3-0324:free", "deepseek/deepseek-r1:free", "google/gemini-2.0-flash-exp:free"),
            preset("GEMINI", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta/openai/",
                "gemini-2.0-flash", "gemini-1.5-pro"),
            preset("CLAUDE", "Claude", "https://api.anthropic.com/v1/", "claude-3-7-sonnet-20250219"),
            preset("BYTEDANCE", if (chinaRegion) "字节火山引擎" else "ByteDance", "https://ark.cn-beijing.volces.com/api/v3",
                "deepseek-v3-250324", "deepseek-r1-250120"),
            preset("ALIYUN", if (chinaRegion) "阿里云百炼" else "AlibabaCloud", "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "deepseek-v3", "deepseek-r1"),
            preset("DEEPSEEK", "DeepSeek", "https://api.deepseek.com", "deepseek-chat", "deepseek-reasoner"),
            preset("SILICONFLOW", if (chinaRegion) "硅基流动" else "SiliconFlow", "https://api.siliconflow.cn/v1",
                "deepseek-ai/DeepSeek-V3", "deepseek-ai/DeepSeek-R1", "Qwen/Qwen3-8B")
        ).associateBy(AiBackend::id)
        val order = if (chinaRegion) listOf("BYTEDANCE", "ALIYUN", "DEEPSEEK", "SILICONFLOW", "OPENROUTER", "CHATGPT", "GEMINI", "CLAUDE")
        else listOf("CHATGPT", "OPENROUTER", "GEMINI", "CLAUDE", "BYTEDANCE", "ALIYUN", "DEEPSEEK", "SILICONFLOW")
        return order.map { byId.getValue(it) }
    }

    fun get(templateId: String?): AiBackend? {
        val template = providers().firstOrNull { it.id == templateId } ?: return null
        val now = System.currentTimeMillis()
        return template.copy(id = UUID.randomUUID().toString(), createdAt = now, updatedAt = now)
    }

    fun newBackend(templateId: String? = null): AiBackend = get(templateId) ?: AiBackend()

    fun normalizeBaseUrl(value: String): String = value.trim { it <= ' ' }.removeSuffix("/chat/completions")

    fun keyManagementUrl(baseUrl: String): String? {
        val uri = runCatching { URI(baseUrl.trim()) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true) && !uri.scheme.equals("http", ignoreCase = true)) return null
        return when (uri.host?.lowercase(Locale.ROOT)) {
            "api.openai.com" -> "https://platform.openai.com/api-keys"
            "ark.cn-beijing.volces.com" -> "https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey?apikey=%7B%7D"
            "openrouter.ai" -> "https://openrouter.ai/settings/provisioning-keys"
            "dashscope.aliyuncs.com" -> "https://bailian.console.aliyun.com/?tab=model#/api-key"
            "api.deepseek.com" -> "https://platform.deepseek.com/api_keys"
            "api.siliconflow.cn" -> "https://cloud.siliconflow.cn/account/ak"
            "api.anthropic.com" -> "https://console.anthropic.com/account/keys"
            "generativelanguage.googleapis.com" -> "https://aistudio.google.com/app/apikey"
            else -> null
        }
    }

    private fun preset(id: String, name: String, url: String, vararg models: String) = AiBackend(
        id = id, name = name, baseUrl = url, token = "", models = models.toList(), createdAt = 0L, updatedAt = 0L
    )
}
