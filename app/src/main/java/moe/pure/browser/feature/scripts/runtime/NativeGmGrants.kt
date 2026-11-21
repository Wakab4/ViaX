package moe.pure.browser.feature.scripts.runtime

/** o5.a checks stored script grant bits, including Promise aliases, and keeps API-specific refusals. */
internal object NativeGmGrants {
    fun mask(name: String): Int = when (name) {
        "setValue" -> 8388609
        "getValue" -> 2097154
        "deleteValue" -> 1048584
        "listValues" -> 4194308
        "getResourceURL" -> 67108880
        "getResourceText" -> 32
        "setClipboard" -> 1073742848
        "log" -> 512
        "openInTab" -> 268468224
        "download" -> 2048
        "xmlhttpRequest" -> -2147483392
        else -> 0
    }
    fun refused(name: String): String? = when (name) {
        "getValue" -> "undefined"
        "xmlhttpRequest" -> ""
        else -> null
    }
}
