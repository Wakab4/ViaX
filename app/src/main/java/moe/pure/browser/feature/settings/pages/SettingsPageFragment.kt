package moe.pure.browser.feature.settings.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.feature.settings.host.SettingsRuntimeFragment
import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.feature.settings.SettingsRoute
import moe.pure.browser.feature.settings.data.ActionBinding
import moe.pure.browser.feature.settings.navigation.SettingsNavigation

/** One standard settings route owns one view/controller and Framework view state. */
class SettingsPageFragment : SettingsRecordPage() {
    private var content: SettingsPageContent? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val runtime = SettingsRuntimeFragment.of(activity)
        val route = SettingsRoute.valueOf(requireNotNull(requireArguments().getString(SettingsNavigation.ROUTE)))
        val binding = arguments?.getString(SettingsNavigation.BINDING)?.let(ActionBinding::valueOf)
        return SettingsPageContent(activity, route, binding, appliesWindowInsets, ::closePage,
            { next, selected ->
                if (next == SettingsRoute.CUSTOMIZE) runtime.customize()
                else SettingsNavigation.open(this, next, selected)
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
