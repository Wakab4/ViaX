package moe.pure.browser.browser.state

import android.os.Bundle
import java.util.UUID

/** Pure window callbacks need a slot identity across WebView recreation; runtime page IDs stay Native. */
internal object TabPageIdentity {
    const val KEY = "moe.pure.browser.pageIdentity"
    fun restore(state: Bundle): String = state.getString(KEY)?.takeIf(String::isNotEmpty) ?: UUID.randomUUID().toString()
}
