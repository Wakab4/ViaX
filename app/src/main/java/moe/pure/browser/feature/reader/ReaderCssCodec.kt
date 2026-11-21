package moe.pure.browser.feature.reader

import android.util.Base64

/** w9.k.e2/w0 use v0.b/a directly; invalid Base64 prints its original failure and returns empty. */
internal object ReaderCssCodec {
    fun encode(css: String): String = Base64.encodeToString(css.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
    fun decode(stored: String?): String = try {
        String(Base64.decode(stored.orEmpty().toByteArray(Charsets.UTF_8), Base64.DEFAULT), Charsets.UTF_8).trim { it <= ' ' }
    } catch (error: Exception) { error.printStackTrace(); "" }
}
