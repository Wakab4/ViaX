package moe.pure.browser.feature.download.actions

import android.content.Context
import android.content.Intent
import moe.pure.browser.feature.download.DownloadFiles
import moe.pure.browser.feature.download.DownloadTask
import moe.pure.browser.core.mime.NativeMimeTypes

/** z8.f1.b: extension overrides stored MIME; package archives use INSTALL_PACKAGE. */
internal object DownloadFileIntents {
    fun view(context: Context, task: DownloadTask): Intent {
        val dot = task.name.lastIndexOf('.')
        val mime = if (dot > 0 && dot < task.name.lastIndex) NativeMimeTypes.mime(task.name.substring(dot + 1), task.mime) ?: task.mime else task.mime
        return Intent(if (mime == "application/vnd.android.package-archive") Intent.ACTION_INSTALL_PACKAGE else Intent.ACTION_VIEW)
            .setDataAndType(DownloadFiles(context).shareUri(task), mime)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
