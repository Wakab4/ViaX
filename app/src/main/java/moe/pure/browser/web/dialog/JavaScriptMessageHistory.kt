package moe.pure.browser.web.dialog

import android.os.SystemClock

/** GP d8.a keeps independent alert/confirm counters per site for the current process. */
internal object JavaScriptMessageHistory {
    private data class Record(var score: Int = 0, var last: Long = 0)
    private val records = mutableMapOf<Pair<String, Boolean>, Record>()
    fun status(site: String, confirm: Boolean): Int {
        val record = records.getOrPut(site to confirm, ::Record)
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - record.last
        record.score = when {
            elapsed >= 60_000 -> 0
            record.score >= 30 -> record.score
            else -> minOf(30, record.score + when { elapsed <= 5_000 -> 15; elapsed <= 10_000 -> 10; elapsed <= 30_000 -> 2; else -> 1 })
        }
        if (record.score <= 30) record.last = now
        return when { record.score > 30 -> 2; record.score == 30 -> 1; else -> 0 }
    }
    fun answered(site: String, confirm: Boolean, muted: Boolean) {
        val record = records.getOrPut(site to confirm, ::Record)
        if (record.score > 30) return
        if (muted) record.score = 31
        record.last = SystemClock.elapsedRealtime()
    }
}
