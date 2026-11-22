package moe.via.browser.storage.preferences


/** w9.i defines the stored types and iteration order of settings backup values. */
internal object NativeBackupPreferenceKeys {
    val booleans = listOf("nightcss", BrowserPreferences.SEARCH_TOOLBAR_KEY)
    val strings = listOf("home", "searchurl", "uastring", "csstheme", "taghome", "dlmanager", "language", "videoplayer",
        "displayedmenus", "readercustomcss", "searchtoolbardisabled", "duastring", "searchtoolbarorder", "hiddenmenus", "searchshortcuts")
    val integers = listOf("appflag", "webflag", "fullscreenmode", "search2", "textsize", "uachoice", "screenOrientation",
        "keyback", "keyforward", "keyhome", "keytab", "keymenu", "gesturetoolbarleft", "gesturetoolbarright", "urlbox", "appui2",
        "logochioce", "urlbarcolor", "fab", "restoreclosedtabs", "cleardata2", "cleardataonexit2", "version", "nightfilter",
        "bookmarksorder", "favinfo", "logoinfo2", "searchinfo", "custominfo", "searchsuggestion", "readertextsize", "readerthemecolor",
        "videoorientation", "bookmarksviewmode", "labflag", "duachoice", "webflag2")
    val longs = listOf("updater_filter_subscriptions", "updater_scripts")
    val all = (booleans + strings + integers + longs).toSet()
}
