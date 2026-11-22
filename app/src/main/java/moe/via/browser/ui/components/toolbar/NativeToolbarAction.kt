package moe.via.browser.ui.components.toolbar

/** z.b records identity/group/visibility; the page supplies its callback. */
class NativeToolbarAction(val id: Int, val group: Int, val icon: Int?, val label: String,
    var visible: Boolean = true, val skinKey: String? = null)
