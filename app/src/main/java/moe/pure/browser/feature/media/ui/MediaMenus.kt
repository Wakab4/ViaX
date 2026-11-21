package moe.pure.browser.feature.media.ui

import android.app.Activity
import android.view.View
import moe.pure.browser.ui.dialog.BrowserDialog

/** Native c0 lists use centered highlighted selection; these are not radio dialogs. */
internal class MediaMenus(
    private val activity: Activity,
    private val onOpen: () -> Unit,
    private val onDismiss: () -> Unit,
) {
    private var menu: BrowserDialog? = null

    fun speed(anchor: View, current: Float, onSelected: (Float) -> Unit) {
        val values = listOf(5f, 3f, 2f, 1.5f, 1.25f, 1f, .75f, .5f)
        show(anchor, 78, values.map { it.toString() }, values.indexOf(current)) { onSelected(values[it]) }
    }

    fun orientation(anchor: View, current: Int, onSelected: (Int) -> Unit) =
        show(anchor, 110, listOf("自动", "横屏", "竖屏", "跟随系统"), current, onSelected)

    fun dismiss() {
        menu?.setOnDismissListener(null)
        menu?.dismiss(); menu = null
    }

    private fun show(anchor: View, widthDp: Int, labels: List<String>, selected: Int, action: (Int) -> Unit) {
        dismiss(); onOpen()
        menu = BrowserDialog(activity)
            .singleChoice(labels, selected, highlighted = true, action = action)
            .widthDp(widthDp).anchoredTo(anchor).apply {
                setOnDismissListener { menu = null; onDismiss() }
                show()
            }
    }
}
