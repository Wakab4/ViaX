package moe.via.browser.feature.adblock

import java.io.File
import moe.via.browser.webview.compat.NativePageAddress

/** w4.c stores mutable metadata while the timestamp always comes from the subscription file. */
data class FilterSubscription(var url: String, var title: String? = null, var homepage: String? = null,
    var license: String? = null, var enabled: Boolean = false, var count: Int = 0, var filePath: String? = null) {
    val id: String get() = filePath?.takeIf(String::isNotEmpty)?.let { File(it).nameWithoutExtension } ?: url
    val updated: Long get() = filePath?.takeIf(String::isNotEmpty)?.let { File(it).takeIf(File::exists)?.lastModified() } ?: 0L
    val label: String get() = title ?: NativePageAddress.authority(url)
    override fun equals(other: Any?): Boolean = this === other || other is FilterSubscription && url == other.url
    override fun hashCode(): Int = url.hashCode()
}
