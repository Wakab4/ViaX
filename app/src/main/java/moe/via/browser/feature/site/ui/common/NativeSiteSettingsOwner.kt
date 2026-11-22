package moe.via.browser.feature.site.ui.common

import android.view.View
import moe.via.browser.ui.components.recycler.NativeRecyclerView

interface NativeSiteSettingsOwner : AutoCloseable {
    val view: View
    val title: CharSequence
    val list: NativeRecyclerView get() = view as NativeRecyclerView
    fun render()
    fun back(): Boolean
}
