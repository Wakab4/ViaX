package moe.via.browser.feature.settings

enum class SettingsRoute(val title: String) {
    ROOT("设置"), GENERAL("通用"), CUSTOMIZE("定制"), PRIVACY("隐私"), ADVANCED("高级"),
    TOOLBAR("工具栏设置"), NIGHT("夜间模式"), USER_AGENT("浏览器标识"), USER_AGENT_EDITOR("编辑浏览器标识"), FONT("字体"),
    OPERATIONS("操作设定"), KEYBOARD("键盘快捷键"), ACTIONS("选择行为"),
    LONG_PRESS_MENU("定制长按菜单"), SSL("忽略 SSL 证书警告"), LABS("实验"), SKINS("皮肤")
}
