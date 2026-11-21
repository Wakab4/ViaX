package moe.pure.browser.feature.settings.context

import android.app.Activity
import android.app.Dialog
import android.content.Context
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.feature.fonts.ui.NativeTypography

object SettingsWindowEffects {
    /** Dialogs own independent windows; the main Activity is never blurred without a popup. */
    fun attachDialog(dialog: Dialog): AutoCloseable {
        val window = dialog.window ?: return AutoCloseable {}
        NativeTypography.tree(window.decorView)
        return SettingsDialogBlur.attach(dialog)
    }
}
