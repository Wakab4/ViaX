package moe.via.browser.feature.home.ui.customize.tools

import android.content.Context
import android.graphics.Color
import moe.via.browser.R
import moe.via.browser.feature.home.ui.customize.DesignTool
import moe.via.browser.feature.home.ui.customize.HomeCustomizeState
import moe.via.browser.feature.home.ui.customize.HomeDesignPanels
import moe.via.browser.feature.home.ui.customize.palette.BackgroundPaletteEntry

internal class BackgroundDesignTools(private val context: Context, private val model: HomeCustomizeState,
    private val commands: HomeDesignPanels.Commands, private val selected: () -> BackgroundPaletteEntry?) : HomeToolSource {
    private fun text(id: Int) = context.getString(id)
    override fun items(): List<DesignTool> = buildList {
        val entry = selected() ?: return@buildList
        val photo = entry == BackgroundPaletteEntry.Image
        val summary = if (photo) text(R.string.ref_home_tool_background_image)
            else if ((entry as BackgroundPaletteEntry.Color).value == 0) text(R.string.ref_home_tool_mode_default) else text(R.string.ref_home_tool_background_color)
        add(DesignTool.Command(1, text(R.string.ref_home_tool_background), R.drawable.ref_home_background, summary))
        if (photo) {
            val value = model.backgroundDesign
            val color = model.preferences.accentColor
            val light = if (value.overrideLight) value.light else .299 * Color.red(color) + .587 * Color.green(color) + .114 * Color.blue(color) >= 192.0
            add(DesignTool.Toggle(3, R.drawable.ref_home_theme, text(R.string.ref_home_tool_theme), text(R.string.ref_home_tool_light),
                text(R.string.ref_home_tool_theme), text(R.string.ref_home_tool_dark), light))
            add(DesignTool.Number(2, text(R.string.ref_home_tool_opacity), R.drawable.ref_home_background_opacity, 0..80, value.opacity, "%d%%"))
        }
    }
    override fun apply(tool: DesignTool, refreshRow: () -> Unit) {
        when (tool.id) {
            1 -> commands.chooseBackground()
            2 -> { model.backgroundDesign.opacity = (tool as DesignTool.Number).value; model.saveBackground() }
            3 -> { model.backgroundDesign.overrideLight = true; model.backgroundDesign.light = (tool as DesignTool.Toggle).value; model.saveBackground() }
        }
    }
}
