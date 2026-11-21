package moe.pure.browser.feature.scripts.popup.model

import moe.pure.browser.feature.scripts.model.UserScript
import moe.pure.browser.feature.scripts.runtime.UserScriptBridge

internal data class ScriptPopupState(val url: String? = null, val bound: Boolean = false,
    val loaded: Boolean = false, val scripts: List<UserScript> = emptyList(), val selected: UserScript? = null,
    val page: Int? = null, val commands: List<UserScriptBridge.MenuCommand> = emptyList(), val changedId: Long? = null)

internal sealed interface ScriptPopupAction {
    data class Add(val url: String?) : ScriptPopupAction
    data class Edit(val id: Long) : ScriptPopupAction
    data class Manager(val id: Long) : ScriptPopupAction
    data class Command(val command: UserScriptBridge.MenuCommand) : ScriptPopupAction
    data class Excluded(val name: String, val authority: String) : ScriptPopupAction
}

/** k8.n is a one-delivery event; retained data is independent of each window's callback. */
internal class ScriptPopupEvent(val action: ScriptPopupAction) {
    var consumed = false
        private set
    fun consume(): Boolean { if (consumed) return false; consumed = true; return true }
}
