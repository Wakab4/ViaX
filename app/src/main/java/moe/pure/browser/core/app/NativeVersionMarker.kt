package moe.pure.browser.core.app

import android.content.Context

/** BrowserApp and ua.b0 use the same installed code and original changelogcode preference. */
internal class NativeVersionMarker(context: Context) {
    private val values = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
    @Suppress("DEPRECATION")
    val installed = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
    val previous get() = values.getInt("changelogcode", 0)
    fun mark() { values.edit().putInt("changelogcode", installed).apply() }
}
