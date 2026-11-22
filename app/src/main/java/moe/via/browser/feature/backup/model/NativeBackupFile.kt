package moe.via.browser.feature.backup.model

import java.io.File

/** m9.a keeps file order and aliases; equal names are not collapsed into a map. */
internal data class NativeBackupFile(val file: File, val name: String = file.name)
