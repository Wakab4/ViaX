package moe.pure.browser.feature.media.data

import android.content.Context
import java.util.Locale

/** Native keys also participate in the existing settings backup/sync codec. */
class MediaPreferences(context: Context) {
    private val values = context.applicationContext.getSharedPreferences("browser", Context.MODE_PRIVATE)
    var videoOrientation: Int
        get() = values.getInt("videoorientation", 0).coerceIn(0, 3)
        set(value) { values.edit().putInt("videoorientation", value.coerceIn(0, 3)).apply() }
    var gesturesEnabled: Boolean
        get() = values.getInt("appflag", defaultFlags()) and 128 != 0
        set(value) {
            val flags = values.getInt("appflag", defaultFlags())
            values.edit().putInt("appflag", if (value) flags or 128 else flags and 128.inv()).apply()
        }
    var externalPlayer: String
        get() = values.getString("videoplayer", null).orEmpty()
        set(value) { values.edit().putString("videoplayer", value).apply() }

    private fun defaultFlags() = 226 or 32768 or if (Locale.getDefault().country.equals("CN", true)) 1024 else 0
}
