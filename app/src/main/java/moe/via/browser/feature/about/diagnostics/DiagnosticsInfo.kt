package moe.via.browser.feature.about.diagnostics

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.webkit.WebView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A snapshot of the installed app and public Android APIs, without reference APK values. */
object DiagnosticsInfo {
    @Suppress("DEPRECATION")
    fun capture(context: Context): String {
        val locale = Locale.getDefault()
        val app = context.packageManager.getPackageInfo(context.packageName, 0)
        val metrics = context.resources.displayMetrics
        return buildString {
            append("Device: ${Build.BRAND} ${Build.MODEL}(${Build.PRODUCT})\n")
            append("Package Name: ${context.packageName}\n")
            append("App Version: ${app.versionName}(${versionCode(app)})\n")
            append("Snapshot Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale).format(Date())}\n")
            append("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(" ")}\n")
            append(String.format(locale, "Display Size: %dx%d\n", metrics.heightPixels, metrics.widthPixels))
            append(String.format(locale, "Android Version: %s API%d\n", Build.VERSION.RELEASE, Build.VERSION.SDK_INT))
            append("Language: ${language(locale)}")
            WebView.getCurrentWebViewPackage()?.let { webView ->
                append("\nWebView Impl: ${webView.packageName}")
                append(String.format(locale, "\nWebView Version: %s(%d)", webView.versionName, versionCode(webView)))
            }
        }.trim()
    }

    private fun versionCode(info: PackageInfo): Long = info.longVersionCode
    private fun language(locale: Locale): String {
        val language = when (locale.language) {
            "in" -> "id"; "iw" -> "he"; "ji" -> "yi"; "jw" -> "jv"; "tl" -> "fil"; else -> locale.language
        }
        if (language == "no" && locale.country == "NO" && locale.variant == "NY") return "nn-NO"
        return if (locale.country.isEmpty()) language else "$language-${locale.country}"
    }
}
