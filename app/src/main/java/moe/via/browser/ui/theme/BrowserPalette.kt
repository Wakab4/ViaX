package moe.via.browser.ui.theme

import android.content.Context
import moe.via.browser.storage.preferences.BrowserNightMode

data class BrowserPalette(
    val night: Boolean,
    val canvas: Int,
    val panel: Int,
    val icon: Int,
    val text: Int,
    val secondary: Int,
    val hint: Int,
    val accent: Int,
    val divider: Int,
    val input: Int,
    val border: Int
)

private val light = BrowserPalette(false, -1, -1, 0xFF232323.toInt(), 0xDE000000.toInt(),
    0x8A000000.toInt(), 0x61000000, 0xFF6F8DE1.toInt(), 0xFFE8E8E8.toInt(), 0xFFEFEFEF.toInt(), 0xFFDDDDDD.toInt())
private val dark = BrowserPalette(true, 0xFF000000.toInt(), 0xFF1C1B1D.toInt(), 0xFF9F9F9F.toInt(),
    0xBEFFFFFF.toInt(), 0x99FFFFFF.toInt(), 0x60FFFFFF, 0xFF617AC1.toInt(), 0xFF242424.toInt(), 0xFF222222.toInt(), 0xFF303030.toInt())

val Context.palette: BrowserPalette
    get() = if (BrowserNightMode.isNight(this)) dark else light
