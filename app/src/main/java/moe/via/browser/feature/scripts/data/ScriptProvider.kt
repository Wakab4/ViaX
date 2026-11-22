package moe.via.browser.feature.scripts.data

import android.content.Context

/** pa.c supplies the same script repository to its model and script pages. */
internal class ScriptProvider private constructor(context: Context) {
    val repository = ScriptRepository(context.applicationContext)

    companion object {
        @Volatile private var instance: ScriptProvider? = null
        fun get(context: Context): ScriptProvider = instance ?: synchronized(this) {
            instance ?: ScriptProvider(context).also { instance = it }
        }
    }
}
