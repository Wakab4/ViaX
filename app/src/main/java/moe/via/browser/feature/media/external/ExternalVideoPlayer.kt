package moe.via.browser.feature.media.external

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import moe.via.browser.feature.media.data.MediaPreferences

/** Native N7: chosen package first, then the system's ordinary video VIEW resolution. */
class ExternalVideoPlayer(
    private val context: Context,
    private val selectedPackage: () -> String = { MediaPreferences(context).externalPlayer }
) {
    fun open(url: String, title: String? = null): Boolean {
        if (url.isEmpty()) return false
        val intent = intent(url, title)
        val preferred = selectedPackage()
        if (preferred.isNotEmpty() && launch(Intent(intent).setPackage(preferred))) return true
        return launch(intent)
    }

    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) { false }
    catch (_: SecurityException) { false }

    companion object {
        fun intent(url: String, title: String? = null): Intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setDataAndType(Uri.parse(url), "video/*")
            if (!title.isNullOrEmpty()) {
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra("title", title)
            }
        }
    }
}
