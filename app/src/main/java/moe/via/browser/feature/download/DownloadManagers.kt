package moe.via.browser.feature.download

import moe.via.browser.R
import android.app.Activity
import android.app.DownloadManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.theme.ThemedActivity

data class DownloadManagerApp(val packageName: String, val mainActivity: String, val downloadActivities: List<String>)
data class DownloadManagerChoice(val id: String, val label: String)

/** Explicit integrations from the reference APK's z1 registry. */
class DownloadManagers(private val context: Context) {
    private val values = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
    val selected get() = values.getString("download_manager", "").orEmpty()
    fun choices(): List<DownloadManagerChoice> = buildList {
        add(DownloadManagerChoice("", context.getString(R.string.settings_download_builtin))); add(DownloadManagerChoice("system", context.getString(R.string.settings_download_system)))
        APPS.forEach { app ->
            val info = runCatching { @Suppress("DEPRECATION") context.packageManager.getApplicationInfo(app.packageName, 0) }.getOrNull()
            if (info?.enabled == true) add(DownloadManagerChoice(app.packageName, context.packageManager.getApplicationLabel(info).toString()))
        }
    }
    fun label() = choices().firstOrNull { it.id == selected }?.label ?: context.getString(R.string.settings_download_builtin)
    fun choose(activity: Activity, changed: () -> Unit = {}) {
        val options = choices()
        val dialogs = (activity as? ThemedActivity)?.dialogScope ?: DialogScope()
        dialogs.show(BrowserDialog(activity, context.getString(R.string.settings_download_manager)).singleChoice(options.map { it.label }, options.indexOfFirst { it.id == selected }.coerceAtLeast(0)) { index ->
            values.edit().putString("download_manager", options[index].id).apply(); changed()
        })
    }
    fun sendToSelected(activity: Activity, url: String): Boolean {
        val app = APPS.firstOrNull { it.packageName == selected } ?: return false
        return app.downloadActivities.any { target -> runCatching { activity.startActivity(sendIntent(app.packageName, target, url)); true }.getOrDefault(false) }
    }
    fun openSelected(activity: Activity): Boolean {
        val intent = if (selected == "system") Intent(DownloadManager.ACTION_VIEW_DOWNLOADS) else {
            val app = APPS.firstOrNull { it.packageName == selected } ?: return false
            Intent().setComponent(ComponentName(app.packageName, app.mainActivity))
        }
        return runCatching { activity.startActivity(intent); true }.getOrDefault(false)
    }
    fun enqueueSystem(url: String, name: String, mime: String, headers: Map<String, String>): Long {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(name); setDescription(url); setMimeType(mime)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            headers.forEach { (key, value) -> addRequestHeader(key, value) }
            if (headers.keys.none { it.equals("Cookie", true) }) CookieManager.getInstance().getCookie(url)?.let { addRequestHeader("Cookie", it) }
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
        }
        return context.getSystemService(DownloadManager::class.java).enqueue(request)
    }
    companion object {
        fun sendIntent(packageName: String, activityName: String, url: String) = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url); component = ComponentName(packageName, activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val APPS = listOf(
            DownloadManagerApp("com.dv.adm.pay", "com.dv.get.Main", listOf("com.dv.get.AEditor", "com.dv.adm.AEditor")),
            DownloadManagerApp("com.dv.adm", "com.dv.get.Main", listOf("com.dv.get.AEditor", "com.dv.adm.AEditor")),
            DownloadManagerApp("idm.internet.download.manager.plus", "idm.internet.download.manager.MainActivity", listOf("idm.internet.download.manager.Downloader", "idm.internet.download.manager.UrlHandlerDownloader")),
            DownloadManagerApp("idm.internet.download.manager", "idm.internet.download.manager.MainActivity", listOf("idm.internet.download.manager.Downloader", "idm.internet.download.manager.UrlHandlerDownloader")),
            DownloadManagerApp("idm.internet.download.manager.adm.lite", "idm.internet.download.manager.MainActivity", listOf("idm.internet.download.manager.Downloader", "idm.internet.download.manager.UrlHandlerDownloader")),
            DownloadManagerApp("com.vanda_adm.vanda", "com.vanda_adm.vanda.MainActivity", listOf("com.vanda_adm.vanda.ClipActivity")),
            DownloadManagerApp("org.freedownloadmanager.fdm", "org.freedownloadmanager.fdm.MyActivity", listOf("org.freedownloadmanager.fdm.SendActivity")),
            DownloadManagerApp("com.dv.get", "com.dv.get.Main", listOf("com.dv.get.AEditor")),
            DownloadManagerApp("com.tachibana.downloader", "com.tachibana.downloader.ui.main.MainActivity", listOf("com.tachibana.downloader.ui.adddownload.AddDownloadActivity")),
            DownloadManagerApp("com.gianlu.aria2app", "com.gianlu.aria2app.main.MainActivity", listOf("com.gianlu.aria2app.LoadingActivity")),
            DownloadManagerApp("com.gopeed", "com.gopeed.MainActivity", listOf("com.gopeed.MainActivity")),
            DownloadManagerApp("com.gopeed.gopeed", "com.gopeed.gopeed.MainActivity", listOf("com.gopeed.gopeed.MainActivity")),
            DownloadManagerApp("com.abdownloadmanager", "com.abdownloadmanager.android.ui.MainActivity", listOf("com.abdownloadmanager.android.pages.add.AddDownloadActivity")),
            DownloadManagerApp("com.fluxdown.app", "com.fluxdown.app.MainActivity", listOf("com.fluxdown.app.MainActivity"))
        )
    }
}
