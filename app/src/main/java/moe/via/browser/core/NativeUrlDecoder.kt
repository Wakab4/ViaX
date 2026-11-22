package moe.via.browser.core

import java.net.URLDecoder

/** i6.u.a keeps the encoded input on invalid UTF-8 or malformed percent escapes. */
internal object NativeUrlDecoder {
    fun guarded(value: String): String = runCatching { URLDecoder.decode(value, "UTF-8") }
        .getOrNull()?.takeUnless { '\uFFFD' in it } ?: value
}
