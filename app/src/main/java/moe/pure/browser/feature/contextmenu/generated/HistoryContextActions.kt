package moe.pure.browser.feature.contextmenu.generated

import android.app.Activity
import moe.pure.browser.R
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope

/** t9.e.e → g6.n.e: a centered all-history confirmation, unrelated to Library period selection. */
internal class HistoryContextActions(private val activity: Activity, private val dialogs: DialogScope) {
    fun confirmClear(onConfirmed: () -> Unit) {
        dialogs.show(BrowserDialog(activity, activity.getString(R.string.ref_generated_history_context_clear),
            activity.getString(R.string.ref_generated_history_context_confirm))
            .positive(activity.getString(android.R.string.ok)) { onConfirmed(); true }
            .negative(activity.getString(android.R.string.cancel)))
    }
}
