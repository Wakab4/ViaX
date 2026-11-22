package moe.via.browser.feature.customtabs.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class CustomTabsConnectionService : Service() {
    private val sessions = CustomTabsSessions()
    private val binder = CustomTabsServiceBinder(sessions)
    override fun onBind(intent: Intent?): IBinder = binder
    override fun onDestroy() { sessions.close(); super.onDestroy() }
}
