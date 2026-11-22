package moe.via.browser.feature.customtabs

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle

/** Supported launch values are immutable snapshots; caller actions execute only on a user click. */
data class CustomTabRequest(
    val initialUrl: String?,
    val colorScheme: Int,
    val closeIcon: Bitmap?,
    val action: Action?,
    val menuItems: List<MenuItem>
) {
    data class Action(val icon: Bitmap?, val description: String?, val pendingIntent: PendingIntent?)
    data class MenuItem(val title: String, val pendingIntent: PendingIntent)

    /** Scheme 0 inherits the application's explicit/automatic policy, as Native CustomTab does. */
    val nightOverride: Boolean? get() = when (colorScheme) { 1 -> false; 2 -> true; else -> null }

    companion object {
        fun nightOverride(intent: Intent?): Boolean? = when (runCatching {
            intent?.getIntExtra(CustomTabsContract.COLOR_SCHEME, 0) ?: 0
        }.getOrDefault(0)) { 1 -> false; 2 -> true; else -> null }

        fun from(intent: Intent?): CustomTabRequest {
            val extras = runCatching { intent?.extras }.getOrNull() ?: Bundle()
            val action = runCatching { extras.getBundle(CustomTabsContract.ACTION_BUTTON) }.getOrNull()?.let {
                Action(it.parcel(CustomTabsContract.ACTION_ICON, Bitmap::class.java),
                    runCatching { it.getString(CustomTabsContract.ACTION_DESCRIPTION) }.getOrNull(),
                    it.parcel(CustomTabsContract.ACTION_PENDING_INTENT, PendingIntent::class.java))
            }
            val items = runCatching {
                if (Build.VERSION.SDK_INT >= 33) extras.getParcelableArrayList(CustomTabsContract.MENU_ITEMS, Bundle::class.java)
                else { @Suppress("DEPRECATION") extras.getParcelableArrayList<Bundle>(CustomTabsContract.MENU_ITEMS) }
            }.getOrNull().orEmpty().mapNotNull { item ->
                val title = runCatching { item.getString(CustomTabsContract.MENU_TITLE) }.getOrNull() ?: return@mapNotNull null
                val pending = item.parcel(CustomTabsContract.ACTION_PENDING_INTENT, PendingIntent::class.java) ?: return@mapNotNull null
                MenuItem(title, pending)
            }
            return CustomTabRequest(intent?.dataString, runCatching { extras.getInt(CustomTabsContract.COLOR_SCHEME, 0) }.getOrDefault(0),
                extras.parcel(CustomTabsContract.CLOSE_BUTTON_ICON, Bitmap::class.java), action, items)
        }

        private fun <T : android.os.Parcelable> Bundle.parcel(key: String, type: Class<T>): T? = runCatching {
            if (Build.VERSION.SDK_INT >= 33) getParcelable(key, type)
            else { @Suppress("DEPRECATION") getParcelable<android.os.Parcelable>(key)?.let { if (type.isInstance(it)) type.cast(it) else null } }
        }.getOrNull()
    }
}
