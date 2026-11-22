package moe.via.browser.feature.home.ui.customize

import moe.via.browser.feature.home.data.HomeIconStore
import moe.via.browser.R
import moe.via.browser.feature.home.model.HomeDraftKind
import moe.via.browser.feature.home.model.LogoMode
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.theme.ThemedActivity

internal class HomeCustomizeDialogs(
    private val activity: ThemedActivity,
    private val model: HomeCustomizeState,
    private val picture: () -> Unit,
    private val editHtml: () -> Unit,
    private val refreshed: () -> Unit,
    private val reset: () -> Unit,
    private val cleared: () -> Unit,
    private val message: (String) -> Unit,
    private val scope: DialogScope
) {
    private val preferences get() = model.preferences
    fun logo() {
        scope.show(BrowserDialog(activity, activity.getString(R.string.ref_home_tool_logo))
            .singleChoice(activity.resources.getStringArray(R.array.ref_home_tool_logo_modes).toList(), preferences.logoChoice) { index ->
                when (index) {
                    0 -> {
                        preferences.setDefaultLogo()
                        preferences.markDirty(html = true, style = false)
                        model.logoDesign.apply { width = 0; height = 72; radius = 0 }
                        refreshed(); model.saveLogo()
                    }
                    1 -> picture()
                    2 -> text()
                    3 -> editHtml()
                    4 -> { preferences.hideLogo(); refreshed(); preferences.markDirty(html = true, style = false); model.documentChanged() }
                }
            })
    }

    private fun text() {
        val value = preferences.logoContent.takeIf { preferences.logoMode == LogoMode.TEXT && !it.isNullOrEmpty() }
            ?: preferences.readDraft(HomeDraftKind.TEXT)
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_home_tool_text_title))
        val field = dialog.field(value, "", 4)
        dialog.positive(activity.getString(android.R.string.ok)) {
            val text = field.text.toString()
            preferences.setTextLogo(text); refreshed(); preferences.rememberDraft(HomeDraftKind.TEXT, text)
            preferences.markDirty(html = true, style = false); model.documentChanged(); true
        }.negative()
        scope.show(dialog)
    }

    fun clearIcons() {
        scope.show(BrowserDialog(activity, activity.getString(R.string.ref_home_tool_clear_title), activity.getString(R.string.ref_home_tool_clear_message))
            .positive(activity.getString(android.R.string.ok)) {
                if (HomeIconStore(activity).clear()) { message(activity.getString(R.string.ref_home_tool_cleared)); cleared(); model.documentChanged() }
                true
            }.negative())
    }

    fun reset() {
        scope.show(BrowserDialog(activity, activity.getString(R.string.ref_home_tool_reset_title), activity.getString(R.string.ref_home_tool_reset_message))
            .positive(activity.getString(android.R.string.ok)) { reset(); true }.negative())
    }
}
