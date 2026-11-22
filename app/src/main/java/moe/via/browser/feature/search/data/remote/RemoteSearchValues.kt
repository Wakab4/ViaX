package moe.via.browser.feature.search.data.remote

import moe.via.browser.core.config.NativeRemoteFeatureValues
import moe.via.browser.core.config.RemoteFeatureDocument

/** Process configuration reads never open preferences, files, a database or a network connection. */
object RemoteSearchValues {
    val fulltextAllowed: Boolean get() = NativeRemoteFeatureValues.fulltextAllowed
    internal fun activate(value: RemoteFeatureDocument) = NativeRemoteFeatureValues.activate(value)
}
