package moe.pure.browser.feature.about.diagnostics.ui

import moe.pure.browser.ui.theme.ThemedActivity
import android.os.Bundle

internal object DiagnosticsEntry {
    fun open(activity: ThemedActivity) { activity.currentRecordStack?.push(DiagnosticsFragment::class.java, Bundle()) }
}
