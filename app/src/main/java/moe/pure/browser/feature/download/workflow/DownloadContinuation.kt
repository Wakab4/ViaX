package moe.pure.browser.feature.download.workflow

import android.os.Bundle
import moe.pure.browser.feature.download.DownloadDraft

/** Typed continuation values. Tasks resume by ID; drafts preserve whether confirmation already happened. */
sealed class DownloadContinuation {
    data class Draft(val value: DownloadDraft, val approved: Boolean) : DownloadContinuation()
    data class Task(val id: Long, val restart: Boolean, val relocate: Boolean = false) : DownloadContinuation()
    data object Settings : DownloadContinuation()

    internal fun payload() = (this as? Draft)?.value?.payload
    internal fun save() = Bundle().apply {
        when (val work = this@DownloadContinuation) {
            is Draft -> { putBundle("draft", work.value.save()); putBoolean("approved", work.approved) }
            is Task -> { putLong("task", work.id); putBoolean("restart", work.restart); putBoolean("relocate", work.relocate) }
            Settings -> putBoolean("settings", true)
        }
    }
    companion object {
        internal fun read(bundle: Bundle?): DownloadContinuation? {
            bundle ?: return null
            bundle.getBundle("draft")?.let { return Draft(DownloadDraft.restore(it), bundle.getBoolean("approved")) }
            if (bundle.getBoolean("settings")) return Settings
            return bundle.getLong("task").takeIf { it > 0 }?.let { Task(it, bundle.getBoolean("restart"), bundle.getBoolean("relocate")) }
        }
    }
}
