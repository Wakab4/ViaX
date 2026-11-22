package moe.via.browser.feature.scripts.data.update

import android.content.Context

/** w9.k.z/d2/D/p0 use one interval and a completion timestamp for the scripts updater. */
internal class NativeScriptUpdatePreferences(context: Context) {
    private val values = context.applicationContext.getSharedPreferences("browser", Context.MODE_PRIVATE)
    var interval: Long
        get() = values.getLong("updater_scripts", 0L)
        set(value) { values.edit().putLong("updater_scripts", value).apply() }
    var completedAt: Long
        get() = values.getLong("updated_scripts", 0L)
        set(value) { values.edit().putLong("updated_scripts", value).apply() }
    fun cutoff(now: Long): Long? {
        val period = interval
        val cutoff = now - period
        return if (period < 3600000L || completedAt >= cutoff) null else cutoff
    }
    companion object {
        val INTERVALS = listOf(0L, 1L, 3L, 7L, 15L).map { it * 86400000L }
    }
}
