package moe.pure.browser.feature.backup.archive

import android.content.Context
import moe.pure.browser.feature.backup.crypto.InternalBackupProtocol
import moe.pure.browser.feature.backup.model.BackupData
import moe.pure.browser.feature.backup.model.BackupFileResult
import moe.pure.browser.feature.backup.model.NativeBackupFile
import moe.pure.browser.feature.passwords.data.PasswordRepository
import moe.pure.browser.feature.passwords.format.PasswordBackup
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Stages files using the existing protocol APIs; protocol implementation stays with its owner. */
internal class PasswordFileExport(private val context: Context) {
    fun write(directory: File, password: CharArray?): BackupFileResult {
        check(password != null) { "未提供备份密码" }
        val effective = InternalBackupProtocol.password(password)
        val salt = UUID.randomUUID().toString().replace("-", "")
        try {
            val info = File(directory, "info.enc")
            FileOutputStream(info).use { it.write(InternalBackupProtocol.encodeSalt(salt)) }
            val data = File(directory, BackupData.PASSWORDS.fileName)
            val count = FileOutputStream(data).use { output ->
                PasswordRepository(context).use { PasswordBackup.exportEncrypted(it, output, effective, salt) }
            }
            return BackupFileResult(listOf(NativeBackupFile(info), NativeBackupFile(data)), count)
        } finally { effective.fill('\u0000') }
    }
}
