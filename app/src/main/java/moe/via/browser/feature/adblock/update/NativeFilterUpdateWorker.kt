package moe.via.browser.feature.adblock.update

import moe.via.browser.feature.adblock.FilterSubscription

internal object NativeFilterUpdateWorker {
    fun update(item: FilterSubscription): Boolean {
        if (!NativeFilterDownload.download(item.url, item.filePath)) return false
        NativeFilterMetadata.read(item)
        return true
    }
}
