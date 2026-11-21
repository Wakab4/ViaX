package moe.pure.browser.feature.home.ui.customize.tools

import android.content.Context
import moe.pure.browser.R
import moe.pure.browser.feature.home.ui.customize.DesignTool
import moe.pure.browser.feature.home.ui.customize.HomeCustomizeState
import moe.pure.browser.feature.home.ui.customize.HomeDesignPanels
import java.util.Locale

internal class LogoDesignTools(private val context: Context, private val model: HomeCustomizeState,
    private val commands: HomeDesignPanels.Commands) : HomeToolSource {
    private fun text(id: Int) = context.getString(id)
    override fun items(): List<DesignTool> = buildList {
        val value = model.logoDesign
        val mode = model.preferences.logoChoice
        val names = context.resources.getStringArray(R.array.ref_home_tool_logo_modes)
        add(DesignTool.Command(1, text(R.string.ref_home_tool_logo), R.drawable.ref_home_logo, names.getOrNull(mode).orEmpty()))
        when (mode) {
            0, 1 -> {
                add(DesignTool.Number(3, text(R.string.ref_home_tool_width), R.drawable.ref_home_width, 10..127,
                    if (value.width == 0) 10 else value.width, "%dpx", 10, text(R.string.ref_home_tool_adaptive)))
                add(DesignTool.Number(2, text(R.string.ref_home_tool_height), R.drawable.ref_home_height, 10..127,
                    if (value.height == 0) 10 else value.height, "%dpx", 10, text(R.string.ref_home_tool_adaptive)))
                add(DesignTool.Number(4, text(R.string.ref_home_tool_radius), R.drawable.ref_home_radius, 0..100, value.radius, "%d%%"))
            }
            2 -> {
                add(DesignTool.Number(5, text(R.string.ref_home_tool_font_size), R.drawable.ref_home_font_size, 10..63, value.fontSize, "%dpx"))
                // Native T2 includes both names if either style is set; U2 updates only the active names.
                val summary = if (value.bold || value.italic) String.format(Locale.getDefault(), "%s %s",
                    text(R.string.ref_home_tool_bold), text(R.string.ref_home_tool_italic)).trim { it <= ' ' } else null
                add(DesignTool.Command(6, text(R.string.ref_home_tool_font_style), R.drawable.ref_home_font_style, summary))
            }
        }
    }
    override fun apply(tool: DesignTool, refreshRow: () -> Unit) {
        val value = model.logoDesign
        when (tool.id) {
            1 -> commands.chooseLogo()
            2 -> { val number = tool as DesignTool.Number; value.height = if (number.value == number.specialValue) 0 else number.value; model.saveLogo() }
            3 -> { val number = tool as DesignTool.Number; value.width = if (number.value == number.specialValue) 0 else number.value; model.saveLogo() }
            4 -> { value.radius = (tool as DesignTool.Number).value; model.saveLogo() }
            5 -> { value.fontSize = (tool as DesignTool.Number).value; model.saveLogo() }
            6 -> {
                value.nextTextStyle(); model.saveLogo()
                (tool as DesignTool.Command).summary = if (value.bold || value.italic) String.format(Locale.getDefault(), "%s %s",
                    if (value.bold) text(R.string.ref_home_tool_bold) else "", if (value.italic) text(R.string.ref_home_tool_italic) else "").trim { it <= ' ' } else null
                refreshRow()
            }
        }
    }
}
