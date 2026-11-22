package moe.via.browser.browser.session.state

import android.os.Bundle
import java.util.UUID

/** Window callbacks need a slot identity across WebView recreation; runtime page IDs stay Native. */
internal object TabPageIdentity {
    const val KEY = "moe.via.browser.pageIdentity"
    fun restore(state: Bundle): String = state.getString(KEY)?.takeIf(String::isNotEmpty) ?: UUID.randomUUID().toString()
}
