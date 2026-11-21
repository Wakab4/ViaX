package moe.pure.browser.feature.settings.pages

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.feature.settings.context.SettingsBackground
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.ui.pages.RecordPageFragment

/** Settings surfaces own their background; it must never repaint the browser window beneath them. */
abstract class SettingsRecordPage : RecordPageFragment() {
    private var background: SettingsBackground? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()
        val enabled = BrowserPreferences(activity.getSharedPreferences("browser", android.content.Context.MODE_PRIVATE)).settingsBackground
        background = SettingsBackground(activity, view as ViewGroup, enabled)
    }
    override fun onResume() { super.onResume(); background?.refresh() }
    internal override fun themeChanged() { background?.refresh() }
    override fun onDestroyView() { background?.close(); background = null; super.onDestroyView() }
}
