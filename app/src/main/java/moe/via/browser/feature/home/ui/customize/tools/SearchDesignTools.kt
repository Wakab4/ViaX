package moe.via.browser.feature.home.ui.customize.tools

import android.content.Context
import moe.via.browser.R
import moe.via.browser.feature.home.ui.customize.DesignTool
import moe.via.browser.feature.home.ui.customize.HomeCustomizeState

internal class SearchDesignTools(private val context: Context, private val model: HomeCustomizeState) : HomeToolSource {
    private fun text(id: Int) = context.getString(id)
    override fun items(): List<DesignTool> {
        val value = model.searchDesign
        return listOf(
            DesignTool.Number(1, text(R.string.ref_home_tool_radius), R.drawable.ref_home_radius, 0..100, value.radius, "%d%%"),
            DesignTool.Number(2, text(R.string.ref_home_tool_opacity), R.drawable.ref_home_opacity, 0..100, value.opacity, "%d%%"),
            DesignTool.Number(4, text(R.string.ref_home_tool_stroke_width), R.drawable.ref_home_stroke_width, 0..7, value.strokeWidth, "%dpx"),
            DesignTool.Number(3, text(R.string.ref_home_tool_stroke_opacity), R.drawable.ref_home_stroke_opacity, 0..100, value.strokeOpacity, "%d%%"),
            DesignTool.Toggle(5, R.drawable.ref_home_search_style, text(R.string.ref_home_tool_search_style), text(R.string.ref_home_tool_line),
                text(R.string.ref_home_tool_search_style), text(R.string.ref_home_tool_rectangle), value.line))
    }
    override fun apply(tool: DesignTool, refreshRow: () -> Unit) {
        val value = model.searchDesign
        when (tool.id) {
            1 -> value.radius = (tool as DesignTool.Number).value
            2 -> value.opacity = (tool as DesignTool.Number).value
            3 -> value.strokeOpacity = (tool as DesignTool.Number).value
            4 -> value.strokeWidth = (tool as DesignTool.Number).value
            5 -> value.line = (tool as DesignTool.Toggle).value
            else -> return
        }
        model.saveSearch()
    }
}
