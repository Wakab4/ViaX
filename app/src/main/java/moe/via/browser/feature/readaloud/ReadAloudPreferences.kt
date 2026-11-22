package moe.via.browser.feature.readaloud

import android.content.Context

class ReadAloudPreferences(context: Context) {
    private val values = context.applicationContext.getSharedPreferences("browser", Context.MODE_PRIVATE)
    var rate: Float
        get() = (values.getInt("readaloudspeed", 100) / 100f).coerceIn(.25f, 5f)
        set(value) { values.edit().putInt("readaloudspeed", (normalize(value) * 100).toInt()).apply() }

    companion object {
        fun normalize(value: Float) = if (value.isFinite()) value.coerceIn(.25f, 5f) else 1f
    }
}
