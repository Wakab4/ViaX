package moe.via.browser.browser.ui.contextmenu.generated

import moe.via.browser.feature.library.bookmarks.document.BookmarkDocumentLinks
import moe.via.browser.browser.ui.contextmenu.ContextMenuTarget
import moe.via.browser.browser.ui.contextmenu.PageContextAction

/** o9 DEX 0173–01d6: kind2/11 share a nonempty-href gate and skip actions when its final char is '='. */
internal object BookmarkContextEntries {
    fun entries(target: ContextMenuTarget, hidden: Set<Int>, more: Boolean): List<PageContextAction> {
        val link = target.link.orEmpty()
        val extras = buildList {
            if (link.isNotEmpty() && link.last() != '=') {
                val folder = BookmarkDocumentLinks.isFolderLink(link)
                add(if (folder) PageContextAction.EDIT_BOOKMARK_FOLDER else PageContextAction.EDIT_BOOKMARK)
                if (!folder) add(PageContextAction.ADD_BOOKMARK_FAVORITE)
                add(if (folder) PageContextAction.DELETE_BOOKMARK_FOLDER else PageContextAction.DELETE_BOOKMARK)
            }
        }
        return GeneratedLinkContextEntries.entries(target, hidden, more, extras, extras.size)
    }
}
