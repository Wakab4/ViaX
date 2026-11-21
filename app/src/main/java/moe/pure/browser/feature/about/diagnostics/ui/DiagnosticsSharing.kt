package moe.pure.browser.feature.about.diagnostics.ui

import android.content.Intent
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.core.content.FileProvider
import moe.pure.browser.R
import moe.pure.browser.feature.about.diagnostics.DiagnosticsExport
import moe.pure.browser.feature.about.diagnostics.DiagnosticsInfo
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.tasks.NativeViewJobs
import moe.pure.browser.ui.theme.ThemedActivity
import java.io.File

internal class DiagnosticsSharing(private val activity: ThemedActivity, private val jobs: NativeViewJobs) {
    private var progress: BrowserDialog? = null
    fun share(logs: List<File>) {
        if (logs.isEmpty()) return
        val dialog = BrowserDialog(activity).custom(LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(24), dp(16), dp(24), dp(16))
            addView(ProgressBar(activity), LinearLayout.LayoutParams(dp(32), dp(32)))
            addView(NativeTypography.text(activity).apply { setText(R.string.ref_diagnostics_wait); textSize = 14f },
                LinearLayout.LayoutParams(-2, -2).apply { leftMargin = activity.dp(16) })
        }).apply { setCancelable(false); setCanceledOnTouchOutside(false) }
        activity.dialogScope.show(dialog)
        progress = dialog
        val target = DiagnosticsExport.target(logs)
        val info = DiagnosticsInfo.capture(activity)
        jobs.launch({ DiagnosticsExport.create(target, info, logs) }, failed = { dialog.dismiss() }) { success ->
            if (success && target.exists() && !email(target)) activity.startActivity(intent(target, false))
            dialog.dismiss()
        }
    }

    private fun email(file: File): Boolean {
        val intent = intent(file, true)
        return try { activity.startActivity(intent); true } catch (_: Exception) { false }
    }
    private fun intent(file: File, email: Boolean) = Intent(Intent.ACTION_SEND).apply {
        type = if (email) "message/rfc822" else "application/zip"
        if (email) {
            putExtra(Intent.EXTRA_EMAIL, arrayOf("yafengtu@gmail.com"))
            putExtra(Intent.EXTRA_TEXT, ""); putExtra(Intent.EXTRA_SUBJECT, "Via Issue Report")
        }
        putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    fun close() { progress?.dismiss(); progress = null }
}
