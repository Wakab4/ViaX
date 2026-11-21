package moe.pure.browser.feature.backup.data.settings

import android.content.Context
import android.util.Base64
import moe.pure.browser.core.files.NativeDirectories
import moe.pure.browser.core.files.NativeFileCopy
import moe.pure.browser.feature.home.background.NativeBackgroundFiles
import moe.pure.browser.feature.internalpages.GeneratedPageDirtyState
import org.json.JSONObject
import java.io.File

/** m9.l.l restores the two named attachments after the settings object is applied. */
internal class SettingsImageRestore(private val context: Context) {
    private val values = context.getSharedPreferences("browser", Context.MODE_PRIVATE)

    fun restore(record: JSONObject, files: Map<String, File>) {
        val backgroundName = record.optString("backgroundPath")
        if (backgroundName.isNotEmpty()) files[backgroundName]?.takeIf(File::isFile)?.let { source ->
            NativeDirectories.appFiles(context, "content")?.let { directory ->
                val target = File(directory, source.name)
                if (NativeFileCopy.copy(source, target)) {
                    NativeBackgroundFiles.clear(context)
                    encoded("bghome", target.absolutePath)
                    markHome()
                }
            }
        }
        val logoName = record.optString("logoPath")
        if (logoName.isEmpty()) return
        val source = files[logoName]?.takeIf(File::isFile) ?: return
        val directory = NativeDirectories.appFiles(context, "content") ?: return
        if (!NativeFileCopy.copy(source, File(directory, source.name))) return
        values.edit().putInt("logochioce", 1).apply()
        // The original interpolates the archive source File here, not the copied target.
        encoded("taghome", "<img class=\"smaller\" src=\"file://$source\" />")
        markHome()
    }

    private fun encoded(key: String, text: String) {
        values.edit().putString(key, Base64.encodeToString(text.trim { it <= ' ' }.toByteArray(Charsets.UTF_8), Base64.DEFAULT)).apply()
    }

    private fun markHome() = GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.HOME or GeneratedPageDirtyState.HOME_STYLE)
}
