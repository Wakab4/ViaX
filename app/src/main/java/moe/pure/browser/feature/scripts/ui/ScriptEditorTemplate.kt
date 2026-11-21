package moe.pure.browser.feature.scripts.ui

import java.util.Random

internal object ScriptEditorTemplate {
    const val HEADER = "// ==UserScript==\n// @name         New Userscript\n// @namespace    https://viayoo.com/\n// @version      0.1\n// @description  try to take over the world!\n// @author       You\n// @run-at       document-end\n// @match        https://*/*\n// @grant        none\n// ==/UserScript==\n\n"
    fun create(url: String?): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"; val random = Random()
        val key = buildString { repeat(6) { append(alphabet[random.nextInt(36)]) } }
        val template = HEADER + "(function() {\n    'use strict';\n\n    // Your code here...\n})();"
        val scoped = if (url != null && ((url.length > 6 && url.startsWith("http://", true)) || (url.length > 7 && url.startsWith("https://", true)))) template.replace("https://*/*", url) else template
        return scoped.replace("https://viayoo.com/", "https://viayoo.com/$key")
    }
}
