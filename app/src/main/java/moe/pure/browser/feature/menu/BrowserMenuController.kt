package moe.pure.browser.feature.menu

import android.app.Activity
import android.content.Intent
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.settings.SettingsEntry
import moe.pure.browser.feature.settings.SettingsRoute

/** Dispatches commands; neither the view nor the catalog needs access to a WebView. */
class BrowserMenuController(private val activity: Activity, private val callbacks: Callbacks) {
    interface Callbacks {
        fun context(): MenuContext
        fun perform(action: MenuAction, longPress: Boolean)
        fun perform(action: MenuAction, longPress: Boolean, selected: Boolean) = perform(action, longPress)
        fun exit()
    }

    private val settings = BrowserSettings(activity)
    private val preferences = MenuPreferences(settings.values)
    private val menu = BrowserMenu(activity)
    val isShowing get() = menu.isShowing
    fun close() = menu.close()
    fun nextPage() = menu.nextPage()
    fun readerStateCallback(): (Int) -> Unit = menu.readerStateCallback()

    fun show() {
        val context = callbacks.context()
        val entries = MenuState.entries(activity, preferences.displayed(), context)
        menu.show(entries, context, { entry, longPress ->
            if (longPress) longPress(entry) else click(entry)
        }, callbacks::exit)
    }

    private fun click(entry: MenuEntry) {
        when (val action = entry.action) {
            MenuAction.CUSTOMIZE -> customize()
            MenuAction.SETTINGS -> SettingsEntry.open(activity)
            MenuAction.READER -> callbacks.perform(action, false, entry.selected)
            else -> callbacks.perform(action, false)
        }
    }

    private fun longPress(entry: MenuEntry) {
        val action = entry.action
        if (action !in LONG_ACTIONS) return
        if (action == MenuAction.READER) { callbacks.perform(action, true, entry.selected); return }
        when (action) {
            MenuAction.NIGHT -> SettingsEntry.open(activity, SettingsRoute.NIGHT)
            MenuAction.SETTINGS -> customize()
            MenuAction.FULLSCREEN -> SettingsEntry.open(activity, SettingsRoute.TOOLBAR)
            MenuAction.USER_AGENT -> SettingsEntry.open(activity, SettingsRoute.USER_AGENT)
            MenuAction.FONT_SIZE -> moe.pure.browser.feature.site.ui.SiteSettingsNavigation.feature(activity, moe.pure.browser.feature.site.SiteFeature.FONT_SIZE)
            MenuAction.SHARE -> callbacks.perform(MenuAction.OPEN_WITH, false)
            MenuAction.ADD_BOOKMARK -> callbacks.perform(MenuAction.ADD_SHORTCUT, false)
            MenuAction.DESKTOP -> callbacks.perform(MenuAction.USER_AGENT, false)
            MenuAction.TOOLS -> callbacks.perform(MenuAction.RESOURCES, false)
            MenuAction.NETWORK_LOG, MenuAction.MARK_AD -> moe.pure.browser.feature.adblock.ui.AdblockNavigation.editor(activity)
            else -> callbacks.perform(action, true)
        }
    }

    private fun customize() { moe.pure.browser.feature.menu.customize.MenuCustomizationEntry.open(activity) }

    private companion object {
        // s6.D9 has no click fallback for the other IDs: the popup has already closed.
        val LONG_ACTIONS = setOf(MenuAction.NIGHT, MenuAction.BOOKMARKS, MenuAction.HISTORY,
            MenuAction.DOWNLOADS, MenuAction.SHARE, MenuAction.ADD_BOOKMARK, MenuAction.DESKTOP,
            MenuAction.TOOLS, MenuAction.SETTINGS, MenuAction.TRANSLATE, MenuAction.FULLSCREEN,
            MenuAction.USER_AGENT, MenuAction.NETWORK_LOG, MenuAction.MARK_AD, MenuAction.SITE_SETTINGS,
            MenuAction.SCRIPTS, MenuAction.ADBLOCK, MenuAction.FONT_SIZE, MenuAction.READ_ALOUD,
            MenuAction.READER, MenuAction.AI)
    }
}
