package moe.via.browser.feature.scripts.popup.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import moe.via.browser.core.logging.NativeLog as Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import moe.via.browser.browser.navigation.AddressResolver
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.scripts.data.ScriptRepository
import moe.via.browser.feature.scripts.model.ScriptScopeEditor
import moe.via.browser.feature.scripts.runtime.UserScriptBridge
import moe.via.browser.webview.compat.NativePageAddress

/** ua.c0. Constructing a restored Fragment's model must not open the database or the script engine. */
internal class ScriptPopupModel : ViewModel() {
    val state = MutableLiveData(ScriptPopupState())
    val event = MutableLiveData<ScriptPopupEvent>()
    private var repository: ScriptRepository? = null
    @Volatile private var cleared = false
    private var restoredSelection: Long? = null
    private var bindingEpoch = 0L
    private val value get() = state.value ?: ScriptPopupState()

    fun arguments(url: String?) { if (!value.loaded && value.url == null) state.value = value.copy(url = url) }
    fun restore(saved: Bundle?) {
        if (saved == null || value.loaded) return
        if (saved.containsKey("selected")) restoredSelection = saved.getLong("selected")
        if (saved.containsKey("page")) state.value = value.copy(page = saved.getInt("page").takeIf { it in 0..1 })
    }
    fun save() = Bundle().apply {
        (value.selected?.id ?: restoredSelection)?.let { putLong("selected", it) }
        value.page?.let { putInt("page", it) }
    }

    fun bind(context: Context, url: String?) {
        if (cleared) return
        val application = context.applicationContext
        bindingEpoch++
        val data = repository ?: ScriptRepository(application).also { repository = it }
        state.value = value.copy(url = url, bound = true, changedId = null)
        BrowserExecutors.persistence.execute {
            val result = runCatching {
                data.all().filter { url.isNullOrEmpty() || it.applies(url) }
                    .sortedWith(compareByDescending<moe.via.browser.feature.scripts.model.UserScript> { it.enabled }.thenByDescending { it.created })
            }
            MAIN.post { if (!cleared) result.onSuccess { scripts ->
                state.value = value.copy(loaded = true, scripts = scripts,
                    selected = scripts.firstOrNull { it.id == (value.selected?.id ?: restoredSelection) } ?: value.selected, changedId = null)
                restoredSelection = null
            }.onFailure { Log.warn("ScriptPopup", "Unable to load scripts: ${it.javaClass.simpleName}") } }
        }
    }

    fun unbind() { bindingEpoch++; if (!cleared) state.value = value.copy(bound = false, commands = emptyList(), changedId = null) }
    fun commands(commands: List<UserScriptBridge.MenuCommand>) {
        if (!cleared) state.value = value.copy(commands = commands.sortedBy { it.label }, changedId = null)
    }
    fun select(id: Long) {
        if (!value.bound) return
        value.scripts.firstOrNull { it.id == id }?.let { state.value = value.copy(selected = it, page = 1, changedId = null) }
    }
    fun back() { state.value = value.copy(page = 0, changedId = null) }
    fun toggle(id: Long, enabled: Boolean) {
        if (!value.bound) return
        val data = repository ?: return
        state.value = value.copy(scripts = value.scripts.map { if (it.id == id) it.copy(enabled = enabled) else it },
            selected = value.selected?.let { if (it.id == id) it.copy(enabled = enabled) else it }, changedId = id)
        BrowserExecutors.persistence.execute {
            runCatching { data.enable(id, enabled) }
                .onFailure { Log.warn("ScriptPopup", "Unable to update script: ${it.javaClass.simpleName}") }
        }
    }

    fun add() { if (value.bound) event.value = ScriptPopupEvent(ScriptPopupAction.Add(value.url)) }
    fun edit() { if (value.bound) value.selected?.let { event.value = ScriptPopupEvent(ScriptPopupAction.Edit(it.id)) } }
    fun manager() { if (value.bound) value.selected?.let { event.value = ScriptPopupEvent(ScriptPopupAction.Manager(it.id)) } }
    fun command(command: UserScriptBridge.MenuCommand) {
        if (value.bound) event.value = ScriptPopupEvent(ScriptPopupAction.Command(command))
    }
    fun exclude() {
        if (!value.bound) return
        val selected = value.selected ?: return
        val url = value.url ?: return
        val authority = NativePageAddress.authority(url)
        if (!AddressResolver.isWebUrl(url) || authority.isEmpty()) return
        val data = repository ?: return
        val pattern = url.substring(0, url.indexOf("://") + 3) + authority + "/*"
        val epoch = bindingEpoch
        BrowserExecutors.persistence.execute {
            val success = runCatching {
                val script = data.get(selected.id) ?: return@runCatching false
                data.override(script.id, ScriptScopeEditor.add(script, pattern, false))
                true
            }.getOrDefault(false)
            MAIN.post { if (!cleared && success && value.bound && bindingEpoch == epoch) event.value = ScriptPopupEvent(ScriptPopupAction.Excluded(selected.name, authority)) }
        }
    }

    override fun onCleared() {
        cleared = true
        repository?.let { data -> BrowserExecutors.persistence.execute { data.close() } }
        repository = null
    }
    private companion object { val MAIN = Handler(Looper.getMainLooper()) }
}
