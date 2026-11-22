package moe.via.browser.feature.backup.data.export

import java.io.File

/** m9.h supplies every file; rb.e supplies only the three actual sync dataset prefixes. */
internal class SettingsExportFiles(private val directory: File, val inline: Boolean) {
    fun file(name: String): File? {
        if (inline && !name.startsWith("bookmarks.html") && !name.startsWith("settings.txt") && !name.startsWith("favorites.txt")) return null
        return File(directory, name)
    }
}
