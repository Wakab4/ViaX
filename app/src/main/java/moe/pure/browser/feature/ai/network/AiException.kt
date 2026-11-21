package moe.pure.browser.feature.ai.network

import java.io.IOException

/** Native user-facing mappings; transport exception messages and configured keys are not exposed. */
class AiException(
    val status: Int,
    val finishReason: String? = null,
    val httpCode: Int? = null,
    val responseBody: String? = null
) : IOException(when (status) {
    101 -> "无消息"
    102 -> "未配置 AI 服务提供商"
    103 -> "请求未完成"
    104 -> "AI 服务提供商未正确配置"
    else -> if (httpCode != null) "HTTP 错误：$httpCode\n\n${responseBody?.takeIf(String::isNotEmpty) ?: "Empty"}"
        else "未知错误：${finishReason?.takeIf(String::isNotEmpty) ?: "Unknow error"}"
})
