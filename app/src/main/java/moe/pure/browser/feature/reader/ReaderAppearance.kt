package moe.pure.browser.feature.reader

/** i6.c0.n/f and w.c: one snapshot is also the color sent to the browser surface. */
internal data class ReaderAppearance(val color: Int, val textSize: Int, val customCss: String) {
    fun script(): String {
        val dark = -0x1000000 or (channel(color, 16).times(.3f).toInt() shl 16) or
            (channel(color, 8).times(.3f).toInt() shl 8) or channel(color, 0).times(.3f).toInt()
        val css = ".via-reader-body,.via-reader-body>div{background-color: #${hex(color)} !important;}" +
            ".via-reader-body {color: #${hex(foreground(color))} !important;font-size: ${textSize}px !important;}" +
            "@media (prefers-color-scheme: dark) {.via-reader-body,.via-reader-body>div{background-color: #${hex(dark)} !important;}" +
            ".via-reader-body {color: #${hex(foreground(dark))} !important;}}" + customCss.replace("\n", "\\n")
        // Native preserves custom CSS as supplied; its template only replaces newline characters.
        return "(function(){var e=\"$css\",t=document.getElementById(\"__via_reader_custom__\");" +
            "if(t)t.innerText=e;else{(t=document.createElement(\"style\")).type=\"text/css\";" +
            "t.charset=\"UTF-8\";t.id=\"__via_reader_custom__\";t.appendChild(document.createTextNode(e));document.head.appendChild(t)}})();"
    }

    private fun channel(value: Int, shift: Int) = (value ushr shift) and 255
    private fun hex(value: Int) = (value and 0xffffff).toString(16).padStart(6, '0')
    private fun foreground(value: Int) = if (channel(value, 16) * .299 + channel(value, 8) * .587 +
        channel(value, 0) * .114 >= 192.0) -0x1000000 else -1

    companion object {
        fun from(preferences: ReaderPreferences) = ReaderAppearance(
            preferences.themeColor.takeUnless { it == 0 } ?: -1,
            preferences.textSize.coerceIn(8, 84), preferences.customCss)
    }
}
