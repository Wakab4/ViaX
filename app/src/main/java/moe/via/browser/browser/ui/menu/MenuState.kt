package moe.via.browser.browser.ui.menu

import android.content.Context
import moe.via.browser.R

data class MenuContext(
    val home: Boolean,
    val url: String,
    val night: Boolean,
    val incognito: Boolean,
    val bookmarked: Boolean,
    val favorite: Boolean = false,
    val desktop: Boolean,
    val fullscreen: Boolean,
    val imageMode: Int,
    val customUserAgent: Boolean,
    val textZoom: Int,
    val orientation: Int,
    val adblock: Boolean,
    val game: Boolean,
    val reader: Boolean = false,
    val scripts: Boolean = false,
    val siteSettings: Boolean = false,
    val internalPage: Boolean = home,
    val javascript: Boolean = true,
    val orientationAdjustable: Boolean = true,
    val parentWidth: Int = 0,
    val parentHeight: Int = 0
)

data class MenuEntry(
    val action: MenuAction,
    val title: String,
    val enabled: Boolean = true,
    var selected: Boolean = false,
    val description: String = title
)

/** Snapshot state once when opening the menu; dispatch receives that same state. */
object MenuState {
    fun entries(resources: Context, ids: List<Int>, context: MenuContext): List<MenuEntry?> {
        val seen = HashSet<Int>()
        val actions = mutableListOf<MenuAction?>()
        for (id in ids) {
            // i8.k.v3 exits before applying dynamic state when a duplicate is encountered.
            if (!seen.add(id)) return actions.map { it?.let { action -> MenuEntry(action, action.title(resources)) } }
            actions += MenuAction.fromId(id)
        }
        return actions.map { action -> action?.let { entry(resources, it, context) } }
    }

    private fun entry(resources: Context, action: MenuAction, context: MenuContext): MenuEntry {
        val web = !context.home && (context.url.startsWith("https://", true) || context.url.startsWith("http://", true))
        val file = context.home || context.url.startsWith("file://", true)
        val enabled = when (action) {
            MenuAction.SAVE, MenuAction.FONT_SIZE -> !file
            MenuAction.TRANSLATE, MenuAction.REPORT -> web
            MenuAction.SOURCE -> web || file
            MenuAction.MARK_AD -> web && context.adblock && context.javascript
            MenuAction.PRINT, MenuAction.READ_ALOUD, MenuAction.READER, MenuAction.OPEN_WITH,
            MenuAction.GAME -> !context.internalPage
            MenuAction.ORIENTATION -> context.orientationAdjustable
            else -> true
        }
        val selected = when (action) {
            MenuAction.NIGHT -> context.night
            MenuAction.INCOGNITO -> context.incognito
            MenuAction.ADD_BOOKMARK -> context.bookmarked
            MenuAction.ADD_FAVORITE -> context.favorite
            MenuAction.DESKTOP -> context.desktop
            MenuAction.FULLSCREEN -> context.fullscreen
            MenuAction.IMAGES -> context.imageMode != 0
            MenuAction.USER_AGENT -> context.customUserAgent
            MenuAction.SITE_SETTINGS -> context.siteSettings
            MenuAction.SCRIPTS -> context.scripts
            MenuAction.FONT_SIZE -> context.textZoom != 100
            MenuAction.ORIENTATION -> context.orientationAdjustable && context.orientation != 0
            MenuAction.ADBLOCK -> context.adblock
            MenuAction.GAME -> context.game
            MenuAction.READER -> context.reader
            else -> false
        }
        val title = when {
            action == MenuAction.ADD_BOOKMARK && selected -> resources.getString(R.string.ref_menu_bookmark_added)
            action == MenuAction.ADD_FAVORITE && selected -> resources.getString(R.string.ref_menu_favorite_added)
            action == MenuAction.IMAGES -> resources.getString(when (context.imageMode) {
                2 -> R.string.ref_menu_images_wifi; 1 -> R.string.ref_menu_images_deny; else -> R.string.ref_menu_images_allow
            })
            action == MenuAction.ORIENTATION && selected -> resources.resources.getStringArray(R.array.ref_menu_orientation)[context.orientation.coerceIn(0, 3)]
            else -> action.title(resources)
        }
        val description = when (action) {
            MenuAction.NIGHT, MenuAction.INCOGNITO, MenuAction.DESKTOP, MenuAction.FULLSCREEN,
            MenuAction.ADBLOCK, MenuAction.SITE_SETTINGS, MenuAction.SCRIPTS -> resources.getString(if (selected) R.string.ref_menu_on else R.string.ref_menu_off, title)
            MenuAction.FONT_SIZE -> resources.getString(R.string.ref_menu_set, title)
            MenuAction.USER_AGENT -> if (selected) resources.getString(R.string.ref_menu_set, title) else title
            else -> title
        }
        return MenuEntry(action, title, enabled, selected, description)
    }
}
