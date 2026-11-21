package moe.pure.browser.feature.contextmenu.generated

import moe.pure.browser.feature.contextmenu.ContextMenuTarget
import moe.pure.browser.feature.contextmenu.PageContextAction

/** s6.o9 kind1 appends the three favorite actions only when the native image value is null. */
internal object FavoriteContextEntries {
    fun entries(target: ContextMenuTarget, hidden: Set<Int>, more: Boolean): List<PageContextAction> {
        val favorite = target.image == null
        val extras = if (favorite) listOf(PageContextAction.SORT_FAVORITES, PageContextAction.EDIT_FAVORITE,
            PageContextAction.DELETE_FAVORITE) else emptyList()
        return GeneratedLinkContextEntries.entries(target, hidden, more, extras, if (favorite) 3 else 2)
    }
}
