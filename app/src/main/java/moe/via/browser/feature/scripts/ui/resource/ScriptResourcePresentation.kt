package moe.via.browser.feature.scripts.ui.resource

import android.content.Context
import android.text.Html
import android.text.format.DateUtils
import moe.via.browser.R
import moe.via.browser.feature.download.DownloadNames
import moe.via.browser.feature.scripts.model.ScriptResourceInfo

internal object ScriptResourcePresentation {
    /** g6.p.c builds raw label/value HTML and parses it with legacy flags. */
    fun description(context: Context, info: ScriptResourceInfo): CharSequence {
        val entries = arrayListOf(R.string.ref_script_resource_url to info.url)
        if (info is ScriptResourceInfo.Cached) {
            entries.add(R.string.ref_script_resource_path to info.file.absolutePath)
            entries.add(R.string.ref_script_resource_updated to relativeTime(context, info.modified))
            entries.add(R.string.ref_script_resource_size to DownloadNames.size(info.length))
        }
        return Html.fromHtml(entries.joinToString("<br />") { (label, value) -> "<b>${context.getString(label)}</b><br />$value" }, Html.FROM_HTML_MODE_LEGACY)
    }

    private fun relativeTime(context: Context, modified: Long): String {
        val now = System.currentTimeMillis()
        return if ((((now - modified) / 1000L) / 60L).toInt() < 1) context.getString(R.string.ref_script_resource_just_now)
        else DateUtils.getRelativeTimeSpanString(modified, now, 60000L, 65552).toString()
    }

    /** i6.g0.d: UTF-16 limit and literal scheme removal, with no decoded URI rewrite. */
    fun url(value: String): String {
        val web = value.startsWith("http://", true) || value.startsWith("https://", true)
        val start = if (web) value.indexOf("://") + 3 else 0
        val end = if (web && value.indexOf('/', start) == value.lastIndex) value.lastIndex else value.length
        return value.substring(start, minOf(start + 256, end))
    }
}
