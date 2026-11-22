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
        toggle(1, "滑动屏幕前进后退", prefs.swipeNavigation) { prefs.swipeNavigation = it },
        toggle(2, "使用音量键翻页", prefs.volumePaging) { prefs.volumePaging = it },
        toggle(4, "启用视频播放器手势", prefs.videoGestures, "如果可能，在全屏播放视频时允许滑动屏幕以调整视频进度、屏幕亮度、设备音量") { prefs.videoGestures = it },
        link(3, "键盘快捷键", SettingsRoute.KEYBOARD),
        SettingsItem.content("", "") { bindingPreview() }
    ) }
    private fun bindingPreview(): LinearLayout {
        val activity = host.activity
        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        body.addView(NativeTypography.text(activity).apply {
            text = "点击下列按钮来设置它的长按事件，长按查看它的事件。你也可以滑动工具栏来设置滑动事件。"
            textSize = 12f; setTextColor(activity.palette.secondary); setPadding(dp(16), dp(10), dp(16), dp(10))
        }, LinearLayout.LayoutParams(-1, -2))
        val bar = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
        val resources = listOf(R.drawable.ref_ic_back, R.drawable.ref_ic_forward, R.drawable.ref_ic_home, R.drawable.ref_ic_tabs, R.drawable.ref_ic_menu)
        val bindings = listOf(ActionBinding.LONG_PRESS_BACK, ActionBinding.LONG_PRESS_FORWARD, ActionBinding.LONG_PRESS_HOME, ActionBinding.LONG_PRESS_TAB, ActionBinding.LONG_PRESS_MENU)
        resources.forEachIndexed { index, icon ->
            bar.addView(ImageView(activity).apply {
                setImageResource(icon); imageTintList = ColorStateList.valueOf(activity.palette.icon)
                setPadding(dp(13), dp(13), dp(13), dp(13)); background = activity.touchFeedback()
                contentDescription = bindings[index].label
                setOnClickListener { selectAction(bindings[index]) }
                setOnLongClickListener {
                    BrowserMessage(activity).show(prefs.action(bindings[index]).label, "编辑") { selectAction(bindings[index]) }; true
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
            "" to listOf(0), "标签页行为" to listOf(5, 10, 11, 12, 13, 9, 16, 27),
            "特性行为" to listOf(7, 30, 8, 28, 26, 14, 15, 24, 25, 29),
            "地址栏行为" to listOf(4), "网页行为" to listOf(19, 1, 20, 6, 18, 17, 2, 3)
        )
        groups.forEach { (name, ids) ->
            if (name.isNotEmpty()) add(SettingsItem.section(name))
            ids.forEach { id ->
                val action = NativeAction.fromId(id)
                add(SettingsItem.radio(id, action.label, prefs.action(binding) == action) {
                    prefs.setAction(binding, action)
                    host.command(SettingsCommand.ACTION_CHOSEN)
                })
            }
        }
    }
    fun keyboard(): List<SettingsItem> = listOf(
        "Ctrl + t" to "新建标签", "Ctrl + Tab" to "下一个标签", "Ctrl + Shift + Tab" to "上一个标签",
        "Alt + ←" to "网页后退", "Alt + →" to "网页前进", "Ctrl + w" to "关闭标签",
        "Alt + f" to "菜单", "Ctrl + Shift + b" to "书签", "Ctrl + h" to "历史",
        "Ctrl + f" to "页内查找", "Ctrl + l" to "输入网址", "F5 / Ctrl + r" to "刷新网页",
        "Ctrl + u" to "源码", "Ctrl + d" to "添加书签"
    ).map { (key, action) -> SettingsItem.content(key, key to action) {
        LinearLayout(host.activity).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(20), dp(16), dp(20))
            addView(NativeTypography.text(context).apply { text = key; textSize = 14f; setTextColor(context.palette.text) }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(NativeTypography.text(context).apply { text = action; textSize = 14f; setTextColor(context.palette.text) }, LinearLayout.LayoutParams(-2, -2))
        }
    } }
}
