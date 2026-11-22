package moe.via.browser.feature.home.ui.customize.image

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import moe.via.browser.core.documents.NativeContentFiles

internal object HomeImageImporter {
    /** z8.b.c is Maybe.fromCallable: a null copy result completes without a success/error callback. */
    fun copy(context: Context, source: Uri?, logo: Boolean): String? =
        NativeContentFiles.copy(context, source, if (logo) "logo" else "bg", ".png")

    /** e9.i.c3 invokes this directly in the main-thread success callback. */
    fun backgroundColor(path: String): Int? {
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 8 }) ?: return null
        if (bitmap.width == 0 || bitmap.height == 0) return null
        return bitmap.getPixel(0, 0).takeIf { it != 0 } ?: Color.BLACK
    }
}
