package moe.via.browser.feature.scripts.model

/** Version labels follow the reference's string comparison, including reinstall and downgrade. */
data class ScriptInstallationPrompt(val title: String, val message: String) {
    companion object {
        fun forScript(script: UserScript, installed: UserScript?): ScriptInstallationPrompt {
            val version = script.version ?: installed?.version ?: "0.1"
            if (installed == null) return ScriptInstallationPrompt("安装脚本", "你确定安装 \"${script.name}\"($version) 吗？")
            return if (version == installed.version) ScriptInstallationPrompt("重新安装脚本", "你确定重新安装 \"${script.name}\"($version) 吗？")
            else ScriptInstallationPrompt("更新脚本", "你确定更新脚本 \"${script.name}\"(${installed.version ?: "0.1"} -> $version) 吗？")
        }
    }
}
