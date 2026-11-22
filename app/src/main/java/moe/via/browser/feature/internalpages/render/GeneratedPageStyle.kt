package moe.via.browser.feature.internalpages.render

/** Plain presentation values resolve Native night/background contrast without a window reference. */
data class GeneratedPageStyle(val rtl: Boolean, val night: Boolean, val light: Boolean) {
    private val lightText get() = night || !light
    val foreground get() = if (lightText) "#fafafa" else "#1b1b1b"
    val secondary get() = if (lightText) "#d5d5d5" else "#2b2b2b"
    val icon get() = if (lightText) "#e9e9e9" else "#313131"
    val tap get() = if (lightText) "rgba(255, 255, 255, 0.1)" else "rgba(0, 0, 0, 0.1)"
}
