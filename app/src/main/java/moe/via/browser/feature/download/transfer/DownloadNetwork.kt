package moe.via.browser.feature.download.transfer

import android.content.Context
import moe.via.browser.core.network.BrowserNetworkState

/** k5.b/t: availability means an active network with capabilities, not validated Internet. */
internal class DownloadNetwork(context: Context) {
    private val app = context.applicationContext
    fun available() = BrowserNetworkState.type(app) != 0
}

