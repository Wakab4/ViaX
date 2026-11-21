package moe.pure.browser.feature.fonts

import java.io.File

/** s9.a retains an object identity and derives its displayed filename from the source path. */
internal class FontEntry(val path: String) {
    val name = if (path.isEmpty()) "" else path.substringAfterLast(File.separatorChar)
}
