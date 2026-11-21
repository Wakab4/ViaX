package moe.pure.browser.web.ssl

/** d8.f keeps remembered choices only for this browser process, across page and Activity changes. */
internal object SslSessionDecisions {
    private val choices = mutableMapOf<String, Boolean>()
    fun get(host: String): Boolean? = choices[host]
    fun remember(host: String, allowed: Boolean) { if (host.isNotEmpty()) choices[host] = allowed }
}
