package moe.pure.browser.feature.settings.context

/** Languages available for the application interface. The empty tag follows the device. */
internal object AppLanguages {
    val choices: Map<String, String> = linkedMapOf(
        "" to "跟随系统",
        "zh-CN" to "中文(简体)",
        "zh-TW" to "中文(繁體)",
        "en" to "English",
        "ja" to "日本語"
    )

    fun selected(tag: String): String = tag.takeIf(choices::containsKey).orEmpty()
    fun label(tag: String): String = choices.getValue(selected(tag))
}
