package moe.via.browser.browser.ui.tabs.closed

import android.os.Bundle
import androidx.fragment.app.Fragment
import moe.via.browser.ui.pages.RecordPageScope

object ClosedTabsNavigation {
    const val RESULT = "restore_tabs"
    fun open(owner: Fragment, result: (Bundle) -> Unit) {
        val manager = owner.parentFragmentManager
        manager.setFragmentResultListener(RESULT, owner) { _, bundle -> result(bundle); manager.clearFragmentResultListener(RESULT) }
        if (owner.isVisible && !owner.isRemoving) RecordPageScope.of(owner)?.push(ClosedTabsFragment::class.java, null)
    }
    fun restoreLink(ids: Array<String>, selected: String?): String {
        if (ids.isEmpty()) return "v://tabs"
        val values = ids.filter(String::isNotEmpty)
        if (values.isEmpty()) return "v://tabs"
        return "v://tabs/restore?ids=" + values.joinToString(",") + if (selected.isNullOrEmpty()) "" else "&selected=$selected"
    }
}
