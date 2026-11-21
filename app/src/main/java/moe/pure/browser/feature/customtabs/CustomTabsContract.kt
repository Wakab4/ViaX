package moe.pure.browser.feature.customtabs

/** The GP APK exposes the support-library wire protocol; no browser UI dependency is required. */
object CustomTabsContract {
    const val SERVICE_ACTION = "android.support.customtabs.action.CustomTabsService"
    const val SERVICE_DESCRIPTOR = "android.support.customtabs.ICustomTabsService"
    const val SESSION = "android.support.customtabs.extra.SESSION"
    const val SESSION_ID = "android.support.customtabs.extra.SESSION_ID"
    const val COLOR_SCHEME = "androidx.browser.customtabs.extra.COLOR_SCHEME"
    const val CLOSE_BUTTON_ICON = "android.support.customtabs.extra.CLOSE_BUTTON_ICON"
    const val ACTION_BUTTON = "android.support.customtabs.extra.ACTION_BUTTON_BUNDLE"
    const val MENU_ITEMS = "android.support.customtabs.extra.MENU_ITEMS"
    const val ACTION_ICON = "android.support.customtabs.customaction.ICON"
    const val ACTION_DESCRIPTION = "android.support.customtabs.customaction.DESCRIPTION"
    const val ACTION_PENDING_INTENT = "android.support.customtabs.customaction.PENDING_INTENT"
    const val MENU_TITLE = "android.support.customtabs.customaction.MENU_ITEM_TITLE"
}
