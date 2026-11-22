package moe.via.browser.feature.media.ui

import android.view.View
import moe.via.browser.feature.media.model.MediaGestureKind

/** Page controls stay in the WebView custom view; these actions are Via's extra controls. */
interface MediaActions {
    fun onLockToggle()
    fun onSpeed(anchor: View)
    fun onOrientation(anchor: View)
    fun onPip()
    fun onGestureStart(kind: MediaGestureKind)
    fun onGestureMove(kind: MediaGestureKind, fraction: Float)
    fun onGestureEnd(kind: MediaGestureKind, cancelled: Boolean)
}
