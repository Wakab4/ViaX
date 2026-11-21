package moe.pure.browser.feature.scripts.ui.details

import moe.pure.browser.feature.scripts.model.ScriptDetailsResource
import moe.pure.browser.feature.scripts.model.ScriptScope

/** ta.c compares native row types before their IDs, URL or scope pattern. */
internal sealed interface ScriptDetailsItem {
    data class Setting(val id: Int, val title: String?, val detail: String? = null) : ScriptDetailsItem
    data class Section(val title: String) : ScriptDetailsItem
    data class Add(val match: Boolean, val title: String) : ScriptDetailsItem
    data class Scope(val value: ScriptScope) : ScriptDetailsItem
    data class Resource(val value: ScriptDetailsResource) : ScriptDetailsItem
}
