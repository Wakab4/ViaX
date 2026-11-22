package moe.via.browser.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import moe.via.browser.storage.preferences.BrowserPreferences
import moe.via.browser.ui.theme.fonts.NativeTypography

object SettingsWindowEffects {
    /** Dialogs own independent windows; the main Activity is never blurred without a popup. */
    fun attachDialog(dialog: Dialog): AutoCloseable {
        val window = dialog.window ?: return AutoCloseable {}
        NativeTypography.tree(window.decorView)
        return SettingsDialogBlur.attach(dialog)
    }
}
