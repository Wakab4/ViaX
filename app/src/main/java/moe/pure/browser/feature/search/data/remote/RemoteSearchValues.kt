package moe.pure.browser.feature.search.data.remote

import moe.pure.browser.core.config.NativeRemoteFeatureValues
import moe.pure.browser.core.config.RemoteFeatureDocument

/** Process configuration reads never open preferences, files, a database or a network connection. */
object RemoteSearchValues {
    val fulltextAllowed: Boolean get() = NativeRemoteFeatureValues.fulltextAllowed
    internal fun activate(value: RemoteFeatureDocument) = NativeRemoteFeatureValues.activate(value)
}
