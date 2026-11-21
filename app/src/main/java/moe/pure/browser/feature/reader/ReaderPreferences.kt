package moe.pure.browser.feature.reader

import android.content.Context
import android.content.SharedPreferences

class ReaderPreferences(context: Context) {
    private val values = context.applicationContext.getSharedPreferences("browser", Context.MODE_PRIVATE)
    private val preferences = moe.pure.browser.feature.settings.data.BrowserPreferences(values)

    var textSize: Int
        get() = values.getInt("readertextsize", 17)
        set(value) = update { putInt("readertextsize", value) }

    var themeColor: Int
        get() = values.getInt("readerthemecolor", 0)
        set(value) = update { putInt("readerthemecolor", value) }

    var customCss: String
        get() = ReaderCssCodec.decode(values.getString("readercustomcss", null))
        set(value) = update { putString("readercustomcss", ReaderCssCodec.encode(value)) }

    var confirmEntry: Boolean
        get() = preferences.readerConfirmation
        set(value) { preferences.readerConfirmation = value }

    private fun update(change: SharedPreferences.Editor.() -> Unit) {
        values.edit().apply(change).apply()
    }

    companion object {
        val COLORS = intArrayOf(0, -462365, -4133433, -11908531, -15592942)
    }
}
