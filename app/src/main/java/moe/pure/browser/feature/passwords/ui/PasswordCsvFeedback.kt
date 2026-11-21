package moe.pure.browser.feature.passwords.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import moe.pure.browser.R
import moe.pure.browser.ui.components.BrowserMessage
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity

/** Native short messages and the Download-path confirmation share existing UI components. */
internal class PasswordCsvFeedback(
    private val activity: ThemedActivity,
    private val message: (String) -> Unit
) {
    private val actions = BrowserMessage(activity)

    fun failure() = message(activity.getString(R.string.ref_password_csv_failure))
    fun empty() = message(activity.getString(R.string.ref_password_csv_empty))

    fun imported(count: Int) {
        if (count < 0) message(activity.getString(R.string.ref_password_csv_import_failed))
        else if (count > 0) message(activity.getString(R.string.ref_password_csv_imported, count))
    }

    fun exported(path: String?) {
        if (path == null) message(activity.getString(R.string.ref_password_csv_exported))
        else activity.dialogScope.show(BrowserDialog(activity, activity.getString(R.string.ref_password_csv_hint),
            activity.getString(R.string.ref_password_csv_exported_path, path)).positive(activity.getString(android.R.string.ok)))
    }

    fun permissionDenied() {
        actions.show(activity.getString(R.string.ref_password_csv_permission_denied), activity.getString(android.R.string.ok)) {
            runCatching {
                activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", activity.packageName, null))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            }.onFailure { failure() }
        }
    }

    fun dispose() = actions.close()
}
