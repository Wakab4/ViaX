package moe.via.browser.feature.adblock.update

import android.content.Context

internal class NativeFilterUpdatePreferences(context: Context) {
    private val values = context.applicationContext.getSharedPreferences("browser", Context.MODE_PRIVATE)
    var interval: Long
        get() = values.getLong("updater_filter_subscriptions", 0L)
        set(value) { values.edit().putLong("updater_filter_subscriptions", value).apply() }
    var completedAt: Long
        get() = values.getLong("updated_filter_subscriptions", 0L)
        set(value) { values.edit().putLong("updated_filter_subscriptions", value).apply() }
    fun cutoff(now: Long): Long? {
        val period = interval
        val cutoff = now - period
        return if (period < 3600000L || completedAt >= cutoff) null else cutoff
    }
    companion object { val INTERVALS = listOf(0L, 1L, 3L, 7L, 15L).map { it * 86400000L } }
}
