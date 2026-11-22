package moe.via.browser.feature.settings.data.updates

import android.content.Context
import android.webkit.GeolocationPermissions
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState

/** w9.k.m3 owns the preference structure version; it is independent of changelogcode. */
internal object NativePreferenceUpdates {
    private class Step(val version: Int, val apply: () -> Unit)

    fun apply(context: Context) {
        val values = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
        val previous = values.getInt("version", 0)
        NativeLog.debug(null, "check preferences, current version: $previous")
        val home = NativePreferenceHomeUpdates(values)
        val flags = NativePreferenceFlagUpdates(values)
        val selection = NativePreferenceSelectionUpdates(values)
        val steps = listOf(
            Step(2, home::initialDesign),
            Step(5, home::percentageDesign),
            Step(4, home::markHome),
            Step(6, selection::userAgent),
            Step(3, flags::webSettings),
            Step(7, selection::search),
            Step(8, flags::thirdPartyMode),
            Step(9, flags::appSettings),
            Step(10, flags::webPermissionDefaults),
            Step(12, flags::searchHistoryFlag),
            Step(11, flags::countryFlag),
            Step(12) { GeolocationPermissions.getInstance().clearAll() },
            Step(14, GeneratedPageDirtyState::markAll)
        ).sortedBy(Step::version)
        var highest = previous
        for (step in steps) {
            if (previous < step.version) {
                highest = step.version
                if (previous != 0) {
                    step.apply()
                    NativeLog.debug(null, "update preferences version to $highest")
                }
            }
        }
        if (highest > previous) values.edit().putInt("version", highest).apply()
    }
}
