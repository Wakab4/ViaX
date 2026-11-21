package moe.pure.browser.feature.ai.data

import android.content.Context

class AiPreferences(context: Context) {
    private val values = context.applicationContext.getSharedPreferences("browser", Context.MODE_PRIVATE)

    var backendId: String?
        get() = values.getString("aibackend", null)
        set(value) = update("aibackend", value)

    var modelId: String?
        get() = values.getString("aimodel", null)
        set(value) = update("aimodel", value)

    private fun update(key: String, value: String?) {
        values.edit().putString(key, value).apply()
    }
}
