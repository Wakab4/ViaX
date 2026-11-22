package moe.via.browser.feature.scripts

import android.content.Intent
import android.webkit.WebView
import moe.via.browser.feature.home.HomeDocument
import moe.via.browser.feature.scripts.popup.model.ScriptPopupAction
import moe.via.browser.feature.scripts.runtime.UserScriptBridge
import moe.via.browser.feature.scripts.runtime.UserScriptEngine
import moe.via.browser.feature.scripts.ui.ScriptNavigation
import moe.via.browser.feature.scripts.ui.ScriptInstallDialogs
import moe.via.browser.feature.scripts.ui.ScriptPageDialog
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.theme.ThemedActivity
import java.lang.ref.WeakReference

/** Menu routing and source-window binding stay outside the retained popup model. */
class ScriptController(private val activity: ThemedActivity,
    private val sourceTabs: () -> List<BrowserTab> = { emptyList() }) {
    private val engine by lazy { UserScriptEngine.get(activity) }
    private val repository get() = engine.repository
    private val install by lazy { ScriptInstallDialogs(activity, repository) {} }
    private val message = BrowserMessage(activity)
    private var binding: ScriptPageDialog.Host? = null
    private var window = WeakReference<ScriptPageDialog>(null)
    private var boundSource = WeakReference<WebView>(null)
    private var disposed = false
    val enabled get() = engine.enabled
    fun addon(payload: String) { if (!disposed && payload.isNotEmpty()) install.addon(payload) }
    private fun supportedPage(url: String) = url.startsWith("http://", true) || url.startsWith("https://", true)
    fun pageHasScripts(tab: BrowserTab, url: String = tab.url) = enabled && supportedPage(url) && repository.all().any { it.applies(url) }
    fun pageStarted(url: String) {
        if ((url.startsWith("http://") || url.startsWith("https://")) && url.endsWith(".user.js")) install.request(url, page = true)
    }
    fun show(tab: BrowserTab, longPress: Boolean) {
        if (disposed || activity.isFinishing || activity.isDestroyed) return
        if (longPress) { ScriptNavigation.manager(activity); return }
        val matching = supportedPage(tab.url) && repository.all().any { it.applies(tab.url) }
        if (!enabled) {
            moe.via.browser.storage.preferences.BrowserPreferences(activity.getSharedPreferences("browser", 0)).scriptsEnabled = true
            tab.liveView?.let { UserScriptBridge.refresh(it); tab.reload() }
            message.show(activity.getString(moe.via.browser.R.string.ref_script_popup_global_enabled)); return
        }
        if (!matching) { ScriptNavigation.manager(activity); return }
        val manager = activity.supportFragmentManager
        if (manager.isStateSaved || manager.findFragmentByTag(ScriptPageDialog.TAG) != null) return
        val view = tab.liveView ?: return
        val identity = tab.sourceIdentity(view) ?: return
        val dialog = ScriptPageDialog.create(tab.id, identity, tab.persistence.get()?.takeIf(String::isNotEmpty), tab.url.takeUnless { HomeDocument.isHome(activity, it) })
        dialog.showNow(manager, ScriptPageDialog.TAG)
        bind(dialog, tab, view)
    }

    /** Only the matching already-live source may activate a framework-restored window. */
    fun bindRestored(tab: BrowserTab?) {
        if (disposed) return
        val dialog = activity.supportFragmentManager.findFragmentByTag(ScriptPageDialog.TAG) as? ScriptPageDialog ?: return
        val identity = dialog.sourceIdentity
        val pid = dialog.sourcePid?.takeIf(String::isNotEmpty)
        val match = if (identity == null) null else sequence {
            if (tab != null) yield(tab)
            for (candidate in sourceTabs()) if (candidate !== tab) yield(candidate)
        }.firstNotNullOfOrNull { candidate ->
            if (pid != null && candidate.persistence.get() != pid) null
            else candidate.liveSource(identity)?.let { candidate to it }
        }
        if (match == null) {
            binding?.let { window.get()?.release(it, false) }; binding = null; boundSource.clear(); return
        }
        bind(dialog, match.first, match.second)
    }

    private fun bind(dialog: ScriptPageDialog, tab: BrowserTab, source: WebView) {
        val previous = binding
        if (previous != null && window.get() === dialog && boundSource.get() === source && dialog.isBound(previous)) {
            return
        }
        val weakTab = WeakReference(tab)
        val weakSource = WeakReference(source)
        val host = ScriptPageDialog.Host { action ->
            if (disposed || activity.isFinishing || activity.isDestroyed) return@Host
            when (action) {
                is ScriptPopupAction.Add -> ScriptNavigation.editor(activity, url = action.url)
                is ScriptPopupAction.Edit -> ScriptNavigation.details(activity, action.id)
                is ScriptPopupAction.Manager -> ScriptNavigation.manager(activity, action.id)
                is ScriptPopupAction.Excluded -> message.show(activity.getString(moe.via.browser.R.string.ref_script_popup_excluded, action.name, action.authority))
                is ScriptPopupAction.Command -> {
                    val view = weakSource.get()
                    if (view != null && weakTab.get()?.owns(view) == true) UserScriptBridge.invoke(view, action.command)
                }
            }
        }
        binding?.let { window.get()?.release(it, false) }
        binding = host; window = WeakReference(dialog); boundSource = WeakReference(source)
        dialog.bind(activity.applicationContext, host)
        dialog.commands(emptyList())
        collectCommands(dialog, tab, source, host)
    }
    private fun collectCommands(dialog: ScriptPageDialog, tab: BrowserTab, source: WebView, host: ScriptPageDialog.Host) {
        UserScriptBridge.commands(source) { commands ->
            if (!disposed && binding === host && tab.owns(source) && dialog.isAdded) dialog.commands(commands)
        }
    }

    fun closeWindow(): Boolean {
        val dialog = activity.supportFragmentManager.findFragmentByTag(ScriptPageDialog.TAG) as? ScriptPageDialog ?: return false
        dialog.dismiss(); return true
    }
    fun dispose(changingConfigurations: Boolean = activity.isChangingConfigurations) {
        if (disposed) return
        disposed = true
        binding?.let { window.get()?.release(it, !changingConfigurations) }
        binding = null; window.clear(); boundSource.clear(); message.close()
    }
}
