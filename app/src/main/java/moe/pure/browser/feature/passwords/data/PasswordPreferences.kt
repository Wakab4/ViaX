package moe.pure.browser.feature.passwords.data

import android.content.Context
import java.util.Locale

class PasswordPreferences(context: Context) {
    private val values = context.applicationContext.getSharedPreferences("browser", Context.MODE_PRIVATE)
    var promptToSave: Boolean
        get() = values.getInt("appflag", defaultFlags()) and 131072 == 0
        set(value) = synchronized(LOCK) {
            val flags = values.getInt("appflag", defaultFlags())
            values.edit().putInt("appflag", if (value) flags and 131072.inv() else flags or 131072).apply()
        }
    private fun defaultFlags() = 226 or 32768 or if (Locale.getDefault().country.equals("CN", true)) 1024 else 0
    private companion object { val LOCK = Any() }
}
