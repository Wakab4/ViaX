package moe.pure.browser.feature.home.ui.customize.palette

import android.content.Context
import moe.pure.browser.R

internal sealed interface BackgroundPaletteEntry {
    /** g9.b compares color only; its accessibility name does not change selection identity. */
    class Color(val value: Int, val name: String) : BackgroundPaletteEntry {
        override fun equals(other: Any?) = other is Color && value == other.value
        override fun hashCode() = value
    }
    data object Image : BackgroundPaletteEntry

    companion object {
        fun entries(context: Context): List<BackgroundPaletteEntry> = listOf(
            Color(0, context.getString(R.string.ref_home_palette_default)), Image,
            Color(-13928247, "Classic blue"), Color(-13287859, "Indigo"), Color(-7707309, "Leather"),
            Color(-4162752, "Walnut"), Color(-1919870, "Birch"), Color(-11776948, "Limestone"),
            Color(-3848376, "Red"), Color(-482746, "Orange"), Color(-79799, "Yellow"),
            Color(-4931980, "Green"), Color(-9780028, "Cyan"), Color(-10391622, "Blue"),
            Color(-6592854, "Purple"), Color(-3432857, "Brown"), Color(-6578261, "Light grey"),
            Color(-9876171, "Maroon"), Color(-6801101, "Light Pink"), Color(-2519416, "Carnation"),
            Color(-2121393, "Hay"), Color(-10790341, "Tawny"), Color(-4480632, "Light brown"),
            Color(-10727322, "Deep purple"))
    }
}
