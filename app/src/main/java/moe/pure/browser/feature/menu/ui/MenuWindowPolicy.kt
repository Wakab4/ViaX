package moe.pure.browser.feature.menu.ui

import android.app.Activity
import android.app.Dialog
import android.os.Build
import moe.pure.browser.ui.components.insets.NativeFullscreenState
import java.util.Locale

/** k8.a.R1 and w5.k use n0.c/l3.n only for an already-fullscreen host. */
internal object MenuWindowPolicy {
    fun apply(host: Activity, popup: Dialog) {
        val source = host.window ?: return
        if (!NativeFullscreenState.requested(source)) return
        val cutout = source.decorView.rootWindowInsets?.displayCutout != null
        if (cutout && NativeMenuRom.excludesFullscreenPopup()) return
        val window = popup.window ?: return
        NativeFullscreenState.apply(window, true)
    }

}

/** g6.o's cached priority is significant: Huawei manufacturer alone is not its EMUI predicate. */
private object NativeMenuRom {
    private val name: String by lazy {
        when {
            property("ro.miui.ui.version.name").isNotEmpty() || property("ro.mi.os.version.name").isNotEmpty() -> "MIUI"
            property("ro.build.version.emui").isNotEmpty() -> "EMUI"
            property("ro.build.version.opporom").isNotEmpty() -> "OPPO"
            property("ro.vivo.os.version").isNotEmpty() -> "VIVO"
            property("ro.smartisan.version").isNotEmpty() -> "SMARTISAN"
            Build.DISPLAY?.uppercase(Locale.ROOT)?.contains("FLYME") == true -> "FLYME"
            Build.MANUFACTURER != null -> Build.MANUFACTURER.uppercase(Locale.ROOT)
            else -> runCatching {
                val build = Class.forName("com.huawei.system.BuildEx")
                if (build.getMethod("getOsBrand").invoke(null) == "harmony") "HARMONY" else "UNKNOWN"
            }.getOrDefault("UNKNOWN")
        }
    }
    fun excludesFullscreenPopup() = name == "EMUI" || name == "HARMONY"
    private fun property(key: String): String = runCatching {
        Runtime.getRuntime().exec(arrayOf("getprop", key)).inputStream.bufferedReader().use { it.readLine().orEmpty() }
    }.getOrDefault("")
}
