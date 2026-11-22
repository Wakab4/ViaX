package moe.via.browser.browser.ui.contextmenu.generated

import android.content.Context
import android.webkit.URLUtil
import moe.via.browser.browser.ui.contextmenu.ContextMenuTarget
import moe.via.browser.browser.ui.contextmenu.PageContextAction

/** s6.o9 DEX 0306/0307: generated pages share only these links and threshold-based hidden menus. */
internal object GeneratedLinkContextEntries {
    fun entries(target: ContextMenuTarget, hidden: Set<Int>, more: Boolean,
        extras: List<PageContextAction> = emptyList(), baseline: Int = 0): List<PageContextAction> {
        val link = target.link.orEmpty()
        val actions = buildList {
            if (!target.state.customTab && URLUtil.isNetworkUrl(link)) {
                add(PageContextAction.BACKGROUND); add(PageContextAction.NEW_TAB)
            }
            addAll(extras)
            if (link.isNotEmpty() && !link.startsWith("folder://", true) && !link.startsWith("v://", true) && !link.startsWith("file://", true)) {
                add(PageContextAction.COPY_LINK); add(PageContextAction.SHARE_LINK)
            }
        }
        val excluded = actions.filter { it.nativeId in hidden }
        if (more) return if (excluded.isEmpty()) emptyList() else excluded + PageContextAction.CUSTOMIZE
        val visible = actions.filter { it.nativeId !in hidden }
        return when {
            excluded.isNotEmpty() -> visible + PageContextAction.MORE
            visible.size > baseline -> visible + PageContextAction.CUSTOMIZE
            else -> visible
        }
    }
    fun label(context: Context, action: PageContextAction) = HistoryContextEntries.label(context, action)
}
