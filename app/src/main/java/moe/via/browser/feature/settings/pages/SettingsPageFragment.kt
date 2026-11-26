package moe.via.browser.feature.settings.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.via.browser.feature.settings.host.SettingsRuntimeFragment
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.feature.settings.SettingsRoute
import moe.via.browser.storage.preferences.ActionBinding
import moe.via.browser.feature.settings.navigation.SettingsNavigation

/** One standard settings route owns one view/controller and Framework view state. */
class SettingsPageFragment : SettingsRecordPage() {
    private var content: SettingsPageContent? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val runtime = SettingsRuntimeFragment.of(activity)
        val args = requireArguments()
        val route = SettingsRoute.valueOf(requireNotNull(args.getString(SettingsNavigation.ROUTE)))
        val binding = args.getString(SettingsNavigation.BINDING)?.let(ActionBinding::valueOf)
        val entry = args.takeIf { it.containsKey(SettingsNavigation.ENTRY) }?.getInt(SettingsNavigation.ENTRY)
        return SettingsPageContent(activity, route, binding, entry, appliesWindowInsets, ::closePage,
            { next, selected, target ->
                if (next == SettingsRoute.CUSTOMIZE) runtime.customize()
                else SettingsNavigation.open(this, next, selected, target)
            }, runtime::perform)
            .also { content = it }.view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        content!!.created(savedInstanceState != null)
    }
    fun refreshSettings() { content?.render() }
    override fun onResume() { super.onResume(); refreshSettings() }
    override fun onDestroyView() { content?.close(); content = null; super.onDestroyView() }
}
