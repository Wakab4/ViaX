package moe.pure.browser.feature.scripts.ui

import moe.pure.browser.ui.message.BrowserMessages

import android.net.Uri
import android.view.View
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.scripts.data.ScriptRepository
import moe.pure.browser.feature.scripts.data.ScriptSourceLoader
import moe.pure.browser.feature.scripts.model.ScriptParser
import moe.pure.browser.feature.scripts.model.ScriptInstallationPrompt
import moe.pure.browser.feature.scripts.model.UserScript
import moe.pure.browser.feature.scripts.model.NativeAddonParser
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.ui.dialog.DialogScope

class ScriptInstallDialogs(private val activity: ThemedActivity, private val repository: ScriptRepository,
    private val scope: DialogScope = activity.dialogScope, private val active: () -> Boolean = { true }, private val changed: () -> Unit) {
    fun addon(payload: String) {
        BrowserExecutors.persistence.execute {
            val parsed = NativeAddonParser.parse(payload)
            activity.runOnUiThread {
                if (active() && !activity.isDestroyed && !activity.isFinishing) {
                    if (parsed == null) toast("解析脚本失败！") else review(parsed, false)
                }
            }
        }
    }
    fun download() {
        val dialog = BrowserDialog(activity, "下载脚本")
        val input = dialog.field("", "地址")
        scope.show(dialog.positive("确定") {
            val value = input.text.toString().trim()
            if (!value.startsWith("http://", true) && !value.startsWith("https://", true)) { toast("链接无效。"); return@positive false }
            request(value); true
        })
    }
    fun request(url: String, page: Boolean = false, targetId: Long? = null) {
        toast("解析脚本中…")
        BrowserExecutors.network.execute {
            val parsed = runCatching { ScriptParser.parse(ScriptSourceLoader.load(url).orEmpty(), url) }.getOrNull()
            activity.runOnUiThread { if (active() && !activity.isDestroyed && !activity.isFinishing) {
                if (parsed == null) toast("解析脚本失败！") else review(parsed, page, targetId)
            } }
        }
    }
    fun review(content: String, url: String?) {
        val parsed = ScriptParser.parse(content, url)
        if (parsed == null) { toast("解析脚本失败！"); return }
        review(parsed, false)
    }
    private fun review(parsed: UserScript, page: Boolean, targetId: Long? = null) {
        BrowserExecutors.persistence.execute {
            val installed = runCatching { targetId?.let(repository::get) ?: if (page) repository.findForPageInstall(parsed.key, parsed.downloadUrl) else repository.find(parsed.key) }
            activity.runOnUiThread {
                if (active() && !activity.isDestroyed && !activity.isFinishing) installed.onSuccess { confirm(parsed, it) }.onFailure { toast("解析脚本失败！") }
            }
        }
    }
    private fun confirm(parsed: UserScript, installed: UserScript?) {
        val prompt = ScriptInstallationPrompt.forScript(parsed, installed)
        var installing = false
        val dialog = BrowserDialog(activity, prompt.title, prompt.message).negative().positive("确定") { current ->
            if (installing) return@positive false
            installing = true
            BrowserExecutors.persistence.execute {
                val result = runCatching {
                    if (installed != null) check(repository.get(installed.id) != null) { "脚本已删除" }
                    moe.pure.browser.feature.scripts.data.ScriptInstaller.save(activity.applicationContext, repository, parsed, installed?.id)
                }
                activity.runOnUiThread { installing = false; if (active() && !activity.isDestroyed && !activity.isFinishing) result.onSuccess {
                    current.dismiss(); changed(); toast("安装 \"${parsed.name}\" 成功！")
                }.onFailure { toast("安装 \"${parsed.name}\" 失败！") } }
            }; true
        }
        scope.show(dialog)
    }
    private fun toast(value: String) = BrowserMessages.show(activity, value)
}
