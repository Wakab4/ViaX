package moe.pure.browser.feature.library.history.ui

import android.app.Activity
import android.view.View
import moe.pure.browser.R
import moe.pure.browser.feature.library.history.HistoryPager
import moe.pure.browser.feature.library.history.HistoryPeriod
import moe.pure.browser.ui.dialog.BrowserDialog

/** db.n.W2/o3: cumulative global periods, duplicate-count suppression and a second confirmation. */
internal class HistoryClearController(private val activity: Activity, private val pager: HistoryPager, private val host: Host) : AutoCloseable {
    interface Host { fun anchor(): View; fun searching(): Boolean; fun exitEditing() }
    private var closed = false
    private var dialog: BrowserDialog? = null
    fun clear() {
        if (host.searching()) { selected(pager.records.orEmpty().indices.toList()); return }
        pager.periods { periods ->
            if (closed || activity.isFinishing || activity.isDestroyed) return@periods
            var previous = 0
            val choices = periods.filter { period ->
                if (period.count == 0 || period.count == previous) false else { previous = period.count; true }
            }.reversed()
            show(BrowserDialog(activity, title()).items(choices.map(::label)) { index -> confirmPeriod(choices[index]) }.anchoredTo(host.anchor()))
        }
    }
    fun selected(positions: List<Int>) {
        if (positions.isEmpty()) return
        val rows = pager.records.orEmpty()
        if (positions.size == 1 && positions[0] >= rows.size) return
        val message = if (positions.size == 1) activity.getString(R.string.ref_history_delete_one, rows.getOrNull(positions[0])?.title.orEmpty())
            else activity.getString(R.string.ref_history_delete_many, positions.size)
        show(BrowserDialog(activity, activity.getString(R.string.ref_library_context_delete), message)
            .positive(activity.getString(android.R.string.ok)) { pager.removeSelected(positions, host.searching()); host.exitEditing(); true }
            .negative(activity.getString(android.R.string.cancel)))
    }
    private fun confirmPeriod(period: HistoryPeriod) {
        show(BrowserDialog(activity, title(), label(period))
            .positive(activity.getString(android.R.string.ok)) { pager.clearSince(period.sinceSeconds); true }
            .negative(activity.getString(android.R.string.cancel)).anchoredTo(host.anchor()))
    }
    private fun label(period: HistoryPeriod): String {
        val names = intArrayOf(R.string.ref_history_last_hour, R.string.ref_history_today, R.string.ref_history_yesterday, R.string.ref_history_last_week, R.string.ref_history_all)
        return activity.getString(R.string.ref_history_period_count, activity.getString(names[period.index]), period.count)
    }
    private fun title() = activity.getString(R.string.ref_history_clear_title)
    private fun show(value: BrowserDialog) { if (closed) return; dialog?.dismiss(); dialog = value; value.show() }
    override fun close() { closed = true; dialog?.dismiss(); dialog = null }
}
