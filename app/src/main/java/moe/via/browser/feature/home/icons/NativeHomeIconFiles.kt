package moe.via.browser.feature.home.icons

import moe.via.browser.core.files.NativeDirectories
import java.io.File

/** w3.a/e/f/d keep direct normalization/rename and original deletion result semantics. */
internal object NativeHomeIconFiles {
    fun install(directory: File, selected: File?, url: String): Boolean {
        if (selected == null || selected.path.isEmpty() || url.isEmpty() || !NativeDirectories.ensure(directory)) return false
        if (selected.exists() && !selected.isDirectory && NativeHomeIconBitmaps.normalize(selected)) {
            val target = File(directory, NativeHomeIconPaths.key(url) + ".png")
            if ((!target.exists() || target.delete()) && selected.renameTo(target)) return true
        }
        return false
    }

    fun remove(directory: File, url: String): Boolean {
        if (url.isEmpty()) return false
        val file = File(directory, NativeHomeIconPaths.key(url) + ".png")
        return !file.exists() || file.delete()
    }

    fun retain(directory: File, urls: Set<String>): Boolean {
        if (!directory.isDirectory || urls.isEmpty()) return true
        val files = directory.listFiles() ?: return true
        if (files.isEmpty()) return true
        val names = HashSet<String>()
        for (url in urls) if (url.isNotEmpty()) names.add(NativeHomeIconPaths.key(url) + ".png")
        var count = 0
        for (file in files) if (file.isFile && file.name !in names && file.delete()) count++
        return count > 0
    }
}
