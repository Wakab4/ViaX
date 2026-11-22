package moe.via.browser.feature.scan

import android.app.Activity
import moe.via.browser.browser.ui.host.BrowserRootFragment
import moe.via.browser.ui.pages.RecordPageScope
import moe.via.browser.ui.theme.ThemedActivity

object ScannerNavigation {
    fun open(activity: Activity) {
        val root = requireNotNull(BrowserRootFragment.find(activity as ThemedActivity))
        val manager = root.parentFragmentManager
        manager.setFragmentResultListener(ScannerFragment.RESULT, root) { _, bundle ->
            if (bundle.containsKey("qrcode")) root.scannerResult(bundle.getString("qrcode", "").orEmpty())
            manager.clearFragmentResultListener(ScannerFragment.RESULT)
        }
        if (root.isVisible && !root.isRemoving) RecordPageScope.of(root)?.push(ScannerFragment::class.java, null)
    }
}
