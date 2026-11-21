package moe.pure.browser.feature.scripts.ui

import android.os.Bundle
import moe.pure.browser.ui.theme.ThemedActivity

/** Actual browser/settings entries use the current stack; nested pages use their owning Fragment scope. */
internal object ScriptNavigation {
    fun manager(activity: ThemedActivity, selectedId: Long? = null) {
        activity.currentRecordStack?.push(ScriptManagerFragment::class.java, selectedId?.let { Bundle().apply { putInt("id", it.toInt()) } })
    }
    fun editor(activity: ThemedActivity, id: Long? = null, url: String? = null) {
        activity.currentRecordStack?.push(ScriptEditorFragment::class.java, Bundle().apply { if (id != null) putInt("id", id.toInt()); if (url != null) putString("url", url) })
    }
    fun details(activity: ThemedActivity, id: Long) {
        activity.currentRecordStack?.push(ScriptDetailsFragment::class.java, Bundle().apply { putInt("id", id.toInt()) })
    }
}
