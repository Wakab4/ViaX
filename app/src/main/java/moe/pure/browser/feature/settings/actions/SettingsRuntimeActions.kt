package moe.pure.browser.feature.settings.actions

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.backup.BackupController
import moe.pure.browser.feature.download.DownloadDirectoryDialog
import moe.pure.browser.feature.download.DownloadManagers
import moe.pure.browser.feature.library.LibraryDocuments
import moe.pure.browser.feature.library.LibraryRepository
import moe.pure.browser.feature.media.external.ExternalVideoPlayerSettings
import moe.pure.browser.feature.settings.ui.ClearDataController
import moe.pure.browser.feature.settings.ui.SettingsCommand
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity

/** Window operations keep their feature owners; page rendering and navigation stay in Fragments. */
internal class SettingsRuntimeActions(private val activity: ThemedActivity, private val changed: () -> Unit,
    private val launchDocument: (Intent, Int) -> Unit, private val askPermissions: (Array<String>, Int) -> Unit) {
    private val app = activity.applicationContext
    private val settings = BrowserSettings(activity)
    private val clearData by lazy { ClearDataController(activity, settings) }
    private val directoryDelegate = lazy { DownloadDirectoryDialog(activity, changed = { changed() }) }
    private val directory by directoryDelegate
    private val managers by lazy { DownloadManagers(activity) }
    private val externalDelegate = lazy { ExternalVideoPlayerSettings(activity,
        { settings.preferences.externalVideoPlayer }, { settings.preferences.externalVideoPlayer = it }) }
    private val external by externalDelegate
    private val libraryDelegate = lazy { LibraryRepository(activity) }
    private val library by libraryDelegate
    private val documentsDelegate = lazy { LibraryDocuments(activity, library, BrowserExecutors.persistence, changed, launchDocument) }
    private val documents by documentsDelegate
    private val backupDelegate = lazy { BackupController(activity, launchDocument, askPermissions, onImported = changed, legacyImport = { text ->
        moe.pure.browser.feature.sync.sync.ViaCloudDataCodec(app).use { it.importText(text) }
    }) }
    private val backup by backupDelegate

    fun perform(command: SettingsCommand) {
        when (command) {
            SettingsCommand.CUSTOMIZE -> customize()
            SettingsCommand.SCRIPTS -> moe.pure.browser.feature.scripts.ui.ScriptNavigation.manager(activity)
            SettingsCommand.ABOUT -> moe.pure.browser.feature.about.AboutEntry.open(activity)
            SettingsCommand.SYNC -> moe.pure.browser.feature.sync.ui.SyncSettingsEntry.open(activity)
            SettingsCommand.DOWNLOAD_DIRECTORY -> directory.show()
            SettingsCommand.DOWNLOAD_MANAGER -> managers.choose(activity, changed)
            SettingsCommand.EXTERNAL_PLAYER -> external.show { changed() }
            SettingsCommand.BOOKMARK_DOCUMENTS -> activity.dialogScope.show(BrowserDialog(activity, "导入/备份书签")
                .items(listOf("备份书签", "导入书签")) { if (it == 0) documents.export() else documents.import("") })
            SettingsCommand.IMPORT_DATA -> backup.showImport()
            SettingsCommand.EXPORT_DATA -> backup.showExport()
            SettingsCommand.DEFAULT_BROWSER -> defaultBrowser()
            SettingsCommand.CLEAR_DATA -> clearData.show()
            SettingsCommand.EXIT_CLEAR -> clearData.showExit()
            else -> error("This command belongs to its settings page: $command")
        }
    }
    fun customize() = moe.pure.browser.feature.home.ui.customize.HomeCustomizeEntry.open(activity)
    private fun defaultBrowser() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                val roles = activity.getSystemService(RoleManager::class.java)
                if (roles.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                    launchDocument(roles.createRequestRoleIntent(RoleManager.ROLE_BROWSER), 200)
                    return
                }
            }
            activity.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (_: ActivityNotFoundException) { activity.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) }
    }
    fun resume() {
        if (backupDelegate.isInitialized()) backup.onResume()
        if (directoryDelegate.isInitialized() || DownloadDirectoryDialog.hasRestored(activity)) directory.onResume()
    }
    fun pause() { if (backupDelegate.isInitialized()) backup.onPause() }
    fun result(request: Int, result: Int, data: Intent?): Boolean {
        if (backupDelegate.isInitialized() && backup.onActivityResult(request, result, data)) return true
        return request in 301..302 && documents.onResult(request, result, data?.data)
    }
    fun permissions(request: Int, results: IntArray) = backupDelegate.isInitialized() && backup.onRequestPermissionsResult(request, results)
    fun save(state: Bundle) { if (backupDelegate.isInitialized()) state.putBundle("backup", Bundle().also(backup::saveState)) }
    fun restore(state: Bundle?) { if (state != null) backup.restoreState(state) }
    fun close() {
        if (directoryDelegate.isInitialized()) directory.dispose()
        if (backupDelegate.isInitialized()) backup.dispose(retain = activity.isChangingConfigurations)
        if (documentsDelegate.isInitialized()) documents.close()
        if (libraryDelegate.isInitialized()) library.close()
        if (externalDelegate.isInitialized()) external.dispose()
    }
}
