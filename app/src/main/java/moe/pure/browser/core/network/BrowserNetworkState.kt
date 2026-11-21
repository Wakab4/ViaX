package moe.pure.browser.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** k5.t.a / z8.b0.B: 0 absent, 1 unmetered, 2 metered; Native's WIFI image mode uses !=2. */
object BrowserNetworkState {
    fun type(context: Context): Int {
        val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return 0
        val network = manager.activeNetwork ?: return 0
        val capabilities = manager.getNetworkCapabilities(network) ?: return 0
        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) 1 else 2
    }
}
