package moe.via.browser.browser.ui.contextmenu.image

import android.graphics.Bitmap
import moe.via.browser.feature.scan.decode.QrImageDecoder

/** GP t5.z.c uses a QR reader, full image resolution, TRY_HARDER and explicit UTF-8. */
internal object ContextQrDecoder {
    fun decode(bitmap: Bitmap): String? = QrImageDecoder.decode(bitmap)
}
