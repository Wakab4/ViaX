package moe.via.browser.feature.network

import android.app.Activity
import moe.via.browser.R
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.feature.internalpages.BrowserInternalPages
import moe.via.browser.feature.internalpages.GeneratedNetworkSource

class NetworkToolsController(private val activity: Activity, private val tabs: () -> List<BrowserTab>,
    private val refresh: () -> Unit, private val open: (GeneratedNetworkSource, Boolean) -> Unit) {
    private var sourceId: String? = null
    /** H0(6) uses the last explicit log/resource source, even when a catalog link has no rows. */
    fun generatedSource(): GeneratedNetworkSource {
        val source = tabs().firstOrNull { it.id == sourceId }
        return GeneratedNetworkSource(source?.id.orEmpty(), source?.requests?.snapshot().orEmpty())
    }
    fun clear(current: BrowserTab) {
        if (!NetworkLogPages.isPage(activity, current.url)) return
        val source = tabs().firstOrNull { it.id == sourceId } ?: return
        source.requests.clear()
        refresh()
    }
    fun show(current: BrowserTab, resources: Boolean) {
        val internal = BrowserInternalPages.kind(activity, current.liveView?.url ?: current.url) > 0
        if (!internal) sourceId = current.id
        val source = tabs().firstOrNull { it.id == sourceId }
        val requests = source?.requests?.snapshot().orEmpty()
        if (resources && source?.requests?.hasResources() != true) {
            message(activity.getString(R.string.ref_network_prompt_no_resources) + activity.getString(R.string.ref_network_prompt_hint))
        } else if (!resources && requests.isEmpty()) {
            message(activity.getString(R.string.ref_network_prompt_no_logs))
        } else open(GeneratedNetworkSource(source!!.id, requests), resources)
    }
    private fun message(value: String) = BrowserMessages.show(activity, value)
}
