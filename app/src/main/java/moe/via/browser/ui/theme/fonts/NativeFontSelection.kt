package moe.via.browser.ui.theme.fonts

import android.content.Context
import android.graphics.Typeface
import moe.via.browser.storage.font.NativeFontStore

/** w9.k.J1 caches the selected face, including a missing-file result; x8.g.i supplies DEFAULT. */
internal object NativeFontSelection {
    private var loaded = false
    private var face: Typeface? = null
    fun current(context: Context): Typeface {
        if (!loaded) {
            val name = context.getSharedPreferences("browser", Context.MODE_PRIVATE).getString("uifont", "").orEmpty()
            face = NativeFontStore.get(context).typeface(name)
            loaded = true
        }
        return face ?: Typeface.DEFAULT
    }
    fun changed() { face = null; loaded = false }
}
