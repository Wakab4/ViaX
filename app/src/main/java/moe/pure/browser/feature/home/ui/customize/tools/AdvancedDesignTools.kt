package moe.pure.browser.feature.home.ui.customize.tools

import android.content.Context
import moe.pure.browser.R
import moe.pure.browser.feature.home.ui.customize.DesignTool
import moe.pure.browser.feature.home.ui.customize.HomeCustomizeState
import moe.pure.browser.feature.home.ui.customize.HomeDesignPanels

internal class AdvancedDesignTools(private val context: Context, private val model: HomeCustomizeState,
    private val commands: HomeDesignPanels.Commands) : HomeToolSource {
    private fun text(id: Int) = context.getString(id)
    override fun items(): List<DesignTool> {
        val value = model.generalDesign
        return listOf(
            DesignTool.Toggle(3, R.drawable.ref_home_search_style, text(R.string.ref_home_tool_search_effect), text(R.string.ref_home_tool_blur),
                text(R.string.ref_home_tool_search_effect), text(R.string.ref_home_tool_transparent_effect), value.blur),
            DesignTool.Toggle(1, R.drawable.ref_home_search_part, text(R.string.ref_home_tool_search_part), text(R.string.ref_home_tool_hidden),
                text(R.string.ref_home_tool_search_part), text(R.string.ref_home_tool_shown), value.searchPartHidden),
            DesignTool.Command(0, text(R.string.ref_home_tool_css), R.drawable.ref_home_css, ""),
            DesignTool.Command(2, text(R.string.ref_home_tool_clear_icons), R.drawable.ref_home_clear_icons, ""),
            DesignTool.Command(4, text(R.string.ref_home_tool_reset), R.drawable.ref_home_reset, ""))
    }
    override fun apply(tool: DesignTool, refreshRow: () -> Unit) {
        when (tool.id) {
            0 -> commands.editCss()
            1 -> { model.generalDesign.searchPartHidden = (tool as DesignTool.Toggle).value; model.saveGeneral() }
            2 -> commands.clearIcons()
            3 -> { model.generalDesign.blur = (tool as DesignTool.Toggle).value; model.saveGeneral() }
            4 -> commands.reset()
        }
    }
}
