package moe.via.browser.feature.settings.navigation

import android.os.Bundle
import moe.via.browser.feature.fonts.ui.FontSettingsFragment
import moe.via.browser.feature.settings.SettingsRoute
import moe.via.browser.storage.preferences.ActionBinding
import moe.via.browser.feature.settings.pages.SettingsPageFragment
import moe.via.browser.feature.settings.pages.UserAgentEditorFragment
import moe.via.browser.feature.settings.pages.UserAgentSettingsFragment
import moe.via.browser.feature.settings.skin.ui.SkinSettingsFragment
import moe.via.browser.ui.pages.RecordPageFragment
import moe.via.browser.ui.pages.RecordPageScope
import moe.via.browser.ui.theme.ThemedActivity

/** Settings routes create page instances; FragmentManager owns history and restored view identity. */
internal object SettingsNavigation {
    const val ROUTE = "settings.route"
    const val BINDING = "settings.binding"
    const val USER_AGENT_ID = "settings.ua.id"
    const val USER_AGENT_EDITED = "settings.ua.edited"

    private fun type(route: SettingsRoute): Class<out RecordPageFragment> = when (route) {
        SettingsRoute.FONT -> FontSettingsFragment::class.java
        SettingsRoute.SKINS -> SkinSettingsFragment::class.java
        SettingsRoute.USER_AGENT -> UserAgentSettingsFragment::class.java
        SettingsRoute.USER_AGENT_EDITOR -> UserAgentEditorFragment::class.java
        else -> SettingsPageFragment::class.java
    }
    private fun arguments(route: SettingsRoute, binding: ActionBinding? = null, userAgent: Int = 0) = Bundle().apply {
        putString(ROUTE, route.name)
        binding?.let { putString(BINDING, it.name) }
        if (route == SettingsRoute.USER_AGENT_EDITOR) putInt(USER_AGENT_ID, userAgent)
    }
    fun enter(activity: ThemedActivity, route: SettingsRoute) = requireNotNull(activity.currentRecordStack).push(type(route),
        arguments(route, if (route == SettingsRoute.ACTIONS) ActionBinding.LONG_PRESS_BACK else null))
    fun open(source: RecordPageFragment, route: SettingsRoute, binding: ActionBinding? = null) =
        RecordPageScope.of(source)?.push(type(route), arguments(route, binding))
    fun editUserAgent(source: RecordPageFragment, id: Int) = RecordPageScope.of(source)?.push(
        UserAgentEditorFragment::class.java, arguments(SettingsRoute.USER_AGENT_EDITOR, userAgent = id))
}
