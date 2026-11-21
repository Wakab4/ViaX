package moe.pure.browser.feature.backup.archive

import android.content.Context
import moe.pure.browser.core.archive.NativeBackupZipWriter
import moe.pure.browser.core.files.NativeStreams
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.feature.backup.crypto.InternalBackupProtocol
import moe.pure.browser.feature.backup.data.BackupDataCodec
import moe.pure.browser.feature.backup.model.BackupData
import moe.pure.browser.feature.backup.model.BackupItemResult
import moe.pure.browser.feature.backup.model.BackupResult
import moe.pure.browser.feature.passwords.data.PasswordRepository
import moe.pure.browser.feature.passwords.format.PasswordBackup
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

/** Worker-only ZIP orchestration. Each dataset commits separately and reports its own failure. */
class BackupArchive(context: Context) {
    private val app = context.applicationContext

    fun exportTo(output: OutputStream, selection: Set<BackupData>, password: CharArray? = null): BackupResult {
        val directory = NativeBackupDirectory.create(app)
        val writer = NativeBackupZipWriter(output)
        try {
            writer.start()
            val exported = BackupExportFiles(app).collect(directory, selection, password)
            for (file in exported.files) {
                try { writer.append(file.file, file.name) }
                catch (error: Exception) { NativeLog.warn(null, error) }
            }
            return exported.result
        } finally { NativeStreams.close(writer) }
    }

    fun prepare(input: InputStream): PreparedBackup {
        val staging = File(app.cacheDir, "backup-staging").canonicalFile
        check(staging.parentFile == app.cacheDir.canonicalFile)
        val directory = File(staging, UUID.randomUUID().toString()).canonicalFile
        check(directory.parentFile == staging && (directory.mkdirs() || directory.isDirectory))
        val files = linkedMapOf<String, File>()
        try {
            ZipInputStream(input.buffered()).use { zip ->
                val buffer = ByteArray(8192); var total = 0L; var count = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    check(++count <= 4096) { "备份文件数量过多" }
                    val name = entry.name
                    val accepted = !entry.isDirectory && flatName(name) && recognized(name)
                    val target = if (accepted) File(directory, name).canonicalFile.also { check(it.parentFile == directory) } else null
                    var size = 0L
                    val output = target?.outputStream()
                    try { while (true) {
                        val read = zip.read(buffer); if (read < 0) break
                        size += read; total += read
                        check(size <= 128L * 1024 * 1024 && total <= 512L * 1024 * 1024) { "备份文件过大" }
                        output?.write(buffer, 0, read)
                    } } finally { output?.close() }
                    if (target != null) files[name] = target
                    zip.closeEntry()
                }
            }
            check(files.keys.any { name -> BackupData.entries.any { it.fileName == name } } || "data-for-older-versions.txt" in files) { "无效的备份文件" }
            return PreparedBackup(directory, files.toMap())
        } catch (error: Exception) { PreparedBackup(directory, emptyMap()).close(); throw error }
    }

    fun importFrom(prepared: PreparedBackup, password: CharArray? = null, legacyImport: ((String) -> Int)? = null): BackupResult {
        val results = mutableListOf<BackupItemResult>()
        BackupDataCodec(app).use { codec ->
            for (data in BackupData.entries) {
                val file = prepared.files[data.fileName] ?: continue
                val result = runCatching {
                    when (data) {
                        BackupData.BOOKMARKS -> codec.importBookmarks(file.readText())
                        BackupData.FAVORITES -> codec.restoreFavoriteFile(file, prepared.files["icons.zip"])
                        BackupData.HISTORY -> codec.importHistory(file.readText())
                        BackupData.CLOSED_TABS -> codec.importClosedTabs(file.readText())
                        BackupData.SETTINGS -> codec.restoreSettings(file, prepared.files)
                        BackupData.PASSWORDS -> {
                            check(password != null) { "未提供解密密码" }
                            val saltFile = prepared.files["info.enc"] ?: error("缺少加密备份信息")
                            val salt = InternalBackupProtocol.decodeSalt(saltFile.readBytes())
                            val effective = InternalBackupProtocol.password(password)
                            try { PasswordRepository(app).use { repository -> file.inputStream().use { PasswordBackup.importEncrypted(repository, it, effective, salt) } } }
                            finally { effective.fill('\u0000') }
                        }
                    }
                }
                results += result.fold({ BackupItemResult(data, it) }, {
                    BackupItemResult(data, error = if (data == BackupData.PASSWORDS) "保存的密码未导入，请检查解密密码" else "${data.title}导入失败")
                })
            }
        }
        if (results.isEmpty()) prepared.files["data-for-older-versions.txt"]?.let { file ->
            val result = runCatching { check(legacyImport != null); legacyImport.invoke(file.readText()) }
            results += result.fold({ BackupItemResult(BackupData.SETTINGS, it) }, { BackupItemResult(BackupData.SETTINGS, error = "旧版备份导入失败") })
        }
        return BackupResult(results)
    }

    private fun recognized(name: String) = name in setOf("bookmarks.html", "favorites.txt", "history.txt", "tabs.txt", "settings.txt", "pass.enc", "info.enc", "icons.zip", "data-for-older-versions.txt") ||
        (name.startsWith("script-") || name.startsWith("filters-")) && name.endsWith(".txt") ||
        name.substringAfterLast('.', "").lowercase() in setOf("png", "jpg", "jpeg", "webp", "gif", "svg")
    private fun flatName(name: String) = name.isNotEmpty() && name.length <= 240 && name != "." && name != ".." &&
        name.none { it == '/' || it == '\\' || it == '\u0000' } && !File(name).isAbsolute
}
