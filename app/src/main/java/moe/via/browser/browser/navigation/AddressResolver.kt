package moe.via.browser.browser.navigation

import android.net.Uri

object AddressResolver {
    fun isWebUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        return (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) && !uri.host.isNullOrBlank()
    }

}
