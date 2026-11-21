package moe.pure.browser.web.print

import android.content.Context

/** Native phase-one patch forwards window.print to cmd(516), which prints the current tab. */
internal object PagePrintPolicy {
    fun script(context: Context) = context.assets.open("web/print.js").bufferedReader(Charsets.UTF_8).use { it.readText() }
}
