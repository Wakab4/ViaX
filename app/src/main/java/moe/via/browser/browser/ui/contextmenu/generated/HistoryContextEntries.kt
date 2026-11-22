package moe.via.browser.browser.ui.contextmenu.generated

import android.content.Context
import android.webkit.URLUtil
import moe.via.browser.R
import moe.via.browser.browser.ui.contextmenu.ContextMenuTarget
import moe.via.browser.browser.ui.contextmenu.PageContextAction

/** s6.o9's kind 3 branch has two library actions; ordinary image/info/ad items are not appended. */
internal object HistoryContextEntries {
    fun entries(target: ContextMenuTarget, hidden: Set<Int>, more: Boolean): List<PageContextAction> {
        val link = target.link.orEmpty()
        val actions = buildList {
            if (!target.state.customTab && URLUtil.isNetworkUrl(link)) {
                add(PageContextAction.BACKGROUND); add(PageContextAction.NEW_TAB)
            }
            add(PageContextAction.DELETE_HISTORY); add(PageContextAction.CLEAR_HISTORY)
            if (link.isNotEmpty() && !link.startsWith("folder://", true) && !link.startsWith("v://", true) && !link.startsWith("file://", true)) {
                add(PageContextAction.COPY_LINK); add(PageContextAction.SHARE_LINK)
            }
        }
        val hiddenActions = actions.filter { it.nativeId in hidden }
        if (more) return if (hiddenActions.isEmpty()) emptyList() else hiddenActions + PageContextAction.CUSTOMIZE
        val visible = actions.filter { it.nativeId !in hidden }
        // Native size2 is fixed at 2 for history, not the total count before hiding.
        return when {
            hiddenActions.isNotEmpty() -> visible + PageContextAction.MORE
            visible.size > 2 -> visible + PageContextAction.CUSTOMIZE
            else -> visible
        }
    }

    fun label(context: Context, action: PageContextAction): String {
        val resource = when (action) {
            PageContextAction.BACKGROUND -> R.string.ref_generated_history_context_background
            PageContextAction.NEW_TAB -> R.string.ref_generated_history_context_new_tab
            PageContextAction.COPY_LINK -> R.string.ref_generated_history_context_copy_link
            PageContextAction.SHARE_LINK -> R.string.ref_generated_history_context_share
            PageContextAction.MORE -> R.string.ref_generated_history_context_more
            PageContextAction.CUSTOMIZE -> R.string.ref_generated_history_context_customize
            PageContextAction.DELETE_HISTORY -> R.string.ref_generated_history_context_delete
            PageContextAction.CLEAR_HISTORY -> R.string.ref_generated_history_context_clear
            else -> return action.label(context)
        }
        return context.getString(resource)
    }
}
