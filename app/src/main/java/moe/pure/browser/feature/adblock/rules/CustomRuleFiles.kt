package moe.pure.browser.feature.adblock.rules

import android.content.Context
import java.io.File

/** c1.o/x/w: the custom rule file lives in the app-owned filters directory. */
object CustomRuleFiles {
    fun file(context: Context) = File(context.getExternalFilesDir("filters") ?: File(context.filesDir, "filters"), "custom.txt")

}
