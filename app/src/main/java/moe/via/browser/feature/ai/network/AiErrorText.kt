package moe.via.browser.feature.ai.network

import android.content.Context
import moe.via.browser.R

internal object AiErrorText {
    fun format(context: Context, error: Throwable?): String {
        val failure = error as? AiException
        return when (failure?.status) {
            101 -> context.getString(R.string.ref_ai_page_error_empty)
            102 -> context.getString(R.string.ref_ai_page_error_backend)
            103 -> context.getString(R.string.ref_ai_page_error_incomplete)
            104 -> context.getString(R.string.ref_ai_page_error_configuration)
            else -> if (failure?.httpCode != null) context.getString(R.string.ref_ai_page_error_http, failure.httpCode, failure.responseBody?.takeIf(String::isNotEmpty) ?: "Empty")
                else context.getString(R.string.ref_ai_page_error_unknown, failure?.finishReason?.takeIf(String::isNotEmpty) ?: "Unknow error")
        }
    }
}
