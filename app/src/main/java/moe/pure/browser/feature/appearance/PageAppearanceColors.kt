package moe.pure.browser.feature.appearance

import android.graphics.Color

/** GP g6.y.C and c8.s6.Q7 use a 192 luma threshold, without alpha compositing or WCAG gamma. */
object PageAppearanceColors {
    fun lightBackground(color: Int): Boolean = Color.red(color) * .299 + Color.green(color) * .587 + Color.blue(color) * .114 >= 192.0
    fun icon(color: Int): Int = if (lightBackground(color)) 0xFF232323.toInt() else Color.WHITE
    fun text(color: Int): Int = if (lightBackground(color)) 0xDE000000.toInt() else Color.WHITE
}
