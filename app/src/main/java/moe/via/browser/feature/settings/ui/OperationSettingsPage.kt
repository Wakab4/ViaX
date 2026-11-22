package moe.via.browser.feature.settings.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.SettingsRoute
import moe.via.browser.storage.preferences.ActionBinding
import moe.via.browser.storage.preferences.NativeAction
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

internal class OperationSettingsPage(private val host: SettingsHost, private val selectAction: (ActionBinding) -> Unit) {
    private val prefs get() = host.preferences
    fun rows(): List<SettingsItem> = with(host) { listOf(
        toggle(1, host.text(R.string.settings_swipe_navigation), prefs.swipeNavigation) { prefs.swipeNavigation = it },
        toggle(2, host.text(R.string.settings_volume_paging), prefs.volumePaging) { prefs.volumePaging = it },
        toggle(4, host.text(R.string.settings_video_gestures), prefs.videoGestures, host.text(R.string.settings_video_gestures_detail)) { prefs.videoGestures = it },
        link(3, host.text(R.string.settings_keyboard), SettingsRoute.KEYBOARD),
        SettingsItem.content("", "") { bindingPreview() }
    ) }
    private fun bindingPreview(): LinearLayout {
        val activity = host.activity
        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        body.addView(NativeTypography.text(activity).apply {
            text = host.text(R.string.settings_gesture_preview_help)
            textSize = 12f; setTextColor(activity.palette.secondary); setPadding(dp(16), dp(10), dp(16), dp(10))
        }, LinearLayout.LayoutParams(-1, -2))
        val bar = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
        val resources = listOf(R.drawable.ref_ic_back, R.drawable.ref_ic_forward, R.drawable.ref_ic_home, R.drawable.ref_ic_tabs, R.drawable.ref_ic_menu)
        val bindings = listOf(ActionBinding.LONG_PRESS_BACK, ActionBinding.LONG_PRESS_FORWARD, ActionBinding.LONG_PRESS_HOME, ActionBinding.LONG_PRESS_TAB, ActionBinding.LONG_PRESS_MENU)
        resources.forEachIndexed { index, icon ->
            bar.addView(ImageView(activity).apply {
                setImageResource(icon); imageTintList = ColorStateList.valueOf(activity.palette.icon)
                setPadding(dp(13), dp(13), dp(13), dp(13)); background = activity.touchFeedback()
                contentDescription = host.text(bindings[index].labelRes)
                setOnClickListener { selectAction(bindings[index]) }
                setOnLongClickListener {
                    BrowserMessage(activity).show(host.text(prefs.action(bindings[index]).labelRes), host.text(R.string.ref_library_dialog_edit)) { selectAction(bindings[index]) }; true
                }
            }, LinearLayout.LayoutParams(0, activity.dp(48), 1f))
        }
        body.addView(OperationToolbar(activity, bar) { direction ->
            selectAction(if (direction > 0) ActionBinding.TOOLBAR_LEFT else ActionBinding.TOOLBAR_RIGHT)
        }, LinearLayout.LayoutParams(-1, -2))
        return body
    }
    fun actions(binding: ActionBinding): List<SettingsItem> = buildList {
        val groups = listOf(
            "" to listOf(0), host.text(R.string.settings_tab_actions) to listOf(5, 10, 11, 12, 13, 9, 16, 27),
            host.text(R.string.settings_feature_actions) to listOf(7, 30, 8, 28, 26, 14, 15, 24, 25, 29),
            host.text(R.string.settings_address_actions) to listOf(4), host.text(R.string.settings_page_actions) to listOf(19, 1, 20, 6, 18, 17, 2, 3)
        )
        groups.forEach { (name, ids) ->
            if (name.isNotEmpty()) add(SettingsItem.section(name))
            ids.forEach { id ->
                val action = NativeAction.fromId(id)
                add(SettingsItem.radio(id, host.text(action.labelRes), prefs.action(binding) == action) {
                    prefs.setAction(binding, action)
                    host.command(SettingsCommand.ACTION_CHOSEN)
                })
            }
        }
    }
    fun keyboard(): List<SettingsItem> = listOf(
        "Ctrl + t" to host.text(R.string.ref_tab_switcher_new_tab), "Ctrl + Tab" to host.text(R.string.settings_action_next), "Ctrl + Shift + Tab" to host.text(R.string.settings_action_previous),
        "Alt + ←" to host.text(R.string.settings_action_back), "Alt + →" to host.text(R.string.settings_action_forward), "Ctrl + w" to host.text(R.string.settings_action_close_tab),
        "Alt + f" to host.text(R.string.settings_menu), "Ctrl + Shift + b" to host.text(R.string.ref_menu_bookmarks), "Ctrl + h" to host.text(R.string.ref_menu_history),
        "Ctrl + f" to host.text(R.string.ref_menu_find), "Ctrl + l" to host.text(R.string.settings_action_address), "F5 / Ctrl + r" to host.text(R.string.ref_menu_reload),
        "Ctrl + u" to host.text(R.string.ref_menu_source), "Ctrl + d" to host.text(R.string.ref_menu_add_bookmark)
    ).map { (key, action) -> SettingsItem.content(key, key to action) {
        LinearLayout(host.activity).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(20), dp(16), dp(20))
            addView(NativeTypography.text(context).apply { text = key; textSize = 14f; setTextColor(context.palette.text) }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(NativeTypography.text(context).apply { text = action; textSize = 14f; setTextColor(context.palette.text) }, LinearLayout.LayoutParams(-2, -2))
        }
    } }
}
