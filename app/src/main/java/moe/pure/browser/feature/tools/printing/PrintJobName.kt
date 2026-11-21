package moe.pure.browser.feature.tools.printing

import android.content.Context
import moe.pure.browser.R
import moe.pure.browser.web.compat.NativePageAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** X7 uses empty checks and the original authority spelling; whitespace titles stay valid. */
internal object PrintJobName {
    fun from(context: Context, title: String?, url: String?): String {
        if (!title.isNullOrEmpty()) return title
        NativePageAddress.authority(url).takeIf(String::isNotEmpty)?.let { return it }
        val locale = Locale.getDefault()
        val pattern = when (locale.country) {
            "US" -> "MMddyyyy"
            "UK" -> "ddMMyyyy"
            else -> "yyyyMMdd"
        }
        val date = SimpleDateFormat(pattern, locale).format(Date())
        return String.format(locale, "%s - %s", context.getString(R.string.ref_print_untitled), date)
    }
}
