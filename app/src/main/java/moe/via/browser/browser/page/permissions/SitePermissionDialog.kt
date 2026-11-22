package moe.via.browser.browser.page.permissions

import android.app.Activity
import android.net.Uri
import moe.via.browser.R
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope

/** e8.y0.h and s6.qa share w5.k, with different location and resource request text. */
class SitePermissionDialog(private val activity: Activity) {
    enum class Kind { MEDIA, LOCATION }
    private val scope = DialogScope()

    fun show(origin: String, labels: List<String>, kind: Kind = Kind.MEDIA,
        result: (Boolean, Boolean) -> Unit): BrowserDialog {
        val site = if (kind == Kind.LOCATION) rawAuthority(origin) else mediaAuthority(origin)
        val title = if (kind == Kind.LOCATION) activity.getString(R.string.ref_site_permission_location_title)
            else activity.getString(R.string.ref_site_permission_title,
                site.ifBlank { activity.getString(R.string.ref_site_permission_unknown) })
        val message = if (kind == Kind.LOCATION) activity.getString(R.string.ref_site_permission_location_message, site)
            else labels.joinToString("\n") { activity.getString(R.string.ref_site_permission_line, it) }
        val dialog = BrowserDialog(activity, title, message)
        val remember = dialog.checkbox(activity.getString(R.string.ref_site_permission_remember), false)
        var completed = false
        fun complete(allow: Boolean, keep: Boolean) {
            if (!completed) { completed = true; result(allow, keep) }
        }
        dialog.positive(activity.getString(R.string.ref_site_permission_allow)) { complete(true, remember.isChecked); true }
            .negative(activity.getString(R.string.ref_site_permission_deny)) { complete(false, remember.isChecked); true }
        // DialogScope installs its own dismiss listener: cancellation and owner disposal share this once gate.
        scope.show(dialog) { complete(false, false) }
        return dialog
    }

    private fun mediaAuthority(origin: String): String = runCatching {
        val uri = Uri.parse(origin)
        val host = uri.host.orEmpty()
        if (host.isEmpty() || uri.port == -1) host else "$host:${uri.port}"
    }.getOrDefault("")
    private fun rawAuthority(origin: String): String {
        val marker = origin.indexOf("://")
        if (marker < 0) return ""
        val start = marker + 3
        return origin.substring(start, origin.indexOf('/', start).takeIf { it >= 0 } ?: origin.length)
    }
    fun dispose() = scope.dispose()
}
