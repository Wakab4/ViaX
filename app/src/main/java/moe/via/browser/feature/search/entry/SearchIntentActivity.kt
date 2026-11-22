package moe.via.browser.feature.search.entry

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import moe.via.browser.BrowserActivity

/** Transparent system text-search entry; the browser owns interpretation and tab reuse. */
class SearchIntentActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forward(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        forward(intent)
    }

    private fun forward(source: Intent?) {
        val destination = if (source == null) Intent(this, BrowserActivity::class.java)
        else Intent(source).setClass(this, BrowserActivity::class.java).apply {
            // minSdk 28 includes the reference's API 22+ referrer forwarding path.
            putExtra(REFERER_EXTRA, referrer)
        }
        startActivity(destination)
        finish()
    }

    companion object { const val REFERER_EXTRA = "moe.via.browser.REFERER" }
}
