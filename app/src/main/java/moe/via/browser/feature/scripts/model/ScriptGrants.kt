package moe.via.browser.feature.scripts.model

/** Permission bits from the reference userscript parser. */
object ScriptGrants {
    val bits = mapOf(
        "GM.deleteValue" to 1048576,
        "GM_addStyle" to 64,
        "GM.getResourceUrl" to 67108864,
        "GM_notification" to 262144,
        "GM_addValueChangeListener" to 8192,
        "GM_xmlhttpRequest" to 256,
        "GM.setClipboard" to 1073741824,
        "GM_getResourceURL" to 16,
        "GM_unregisterMenuCommand" to 131072,
        "GM.setValue" to 8388608,
        "GM_addElement" to 128,
        "GM_getResourceText" to 32,
        "GM_listValues" to 4,
        "GM.notification" to 134217728,
        "GM.getValue" to 2097152,
        "GM.xmlHttpRequest" to -2147483648,
        "GM_openInTab" to 32768,
        "GM_deleteValue" to 8,
        "GM.registerMenuCommand" to 536870912,
        "GM_setValue" to 1,
        "GM.addStyle" to 16777216,
        "GM_download" to 2048,
        "GM.openInTab" to 268435456,
        "GM.info" to 524288,
        "GM_info" to 4096,
        "GM_getValue" to 2,
        "GM_removeValueChangeListener" to 16384,
        "GM.addElement" to 33554432,
        "GM.listValues" to 4194304,
        "GM_setClipboard" to 1024,
        "GM_registerMenuCommand" to 65536,
        "GM_log" to 512
    )
}
