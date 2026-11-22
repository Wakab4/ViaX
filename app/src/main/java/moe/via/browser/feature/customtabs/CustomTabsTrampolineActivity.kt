package moe.via.browser.feature.customtabs

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Exported transparent VIEW/NFC entry; SESSION presence and the saved disable flag choose the host. */
class CustomTabsTrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); forward(intent) }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); forward(intent) }
    private fun forward(incoming: Intent?) {
        if (incoming != null) startActivity(CustomTabsEntry.destination(this, incoming))
        finish()
    }
}
