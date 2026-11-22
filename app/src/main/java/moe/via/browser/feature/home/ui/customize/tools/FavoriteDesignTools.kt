package moe.via.browser.feature.home.ui.customize.tools

import android.content.Context
import moe.via.browser.R
import moe.via.browser.feature.home.ui.customize.DesignTool
import moe.via.browser.feature.home.ui.customize.HomeCustomizeState

internal class FavoriteDesignTools(private val context: Context, private val model: HomeCustomizeState) : HomeToolSource {
    private fun text(id: Int) = context.getString(id)
    override fun items(): List<DesignTool> {
        val value = model.favoriteDesign
        return listOf(
            DesignTool.Number(1, text(R.string.ref_home_tool_width), R.drawable.ref_home_width, 24..80,
                if (value.width == 0) 24 else value.width, "%dpx", 24, text(R.string.ref_home_tool_adaptive)),
            DesignTool.Number(2, text(R.string.ref_home_tool_height), R.drawable.ref_home_height, 24..80,
                if (value.height == 0) 24 else value.height, "%dpx", 24, text(R.string.ref_home_tool_adaptive)),
            DesignTool.Number(3, text(R.string.ref_home_tool_radius), R.drawable.ref_home_radius, 0..100, value.radius, "%d%%"),
            DesignTool.Toggle(4, R.drawable.ref_home_icon_style, text(R.string.ref_home_tool_icon_style), text(R.string.ref_home_tool_text_first),
                text(R.string.ref_home_tool_icon_style), text(R.string.ref_home_tool_icon_first), value.iconDisabled),
            DesignTool.Toggle(5, R.drawable.ref_home_icon_color, text(R.string.ref_home_tool_icon_color), text(R.string.ref_home_tool_transparent_icon),
                text(R.string.ref_home_tool_icon_color), text(R.string.ref_home_tool_colorful), value.colorDisabled))
    }
    override fun apply(tool: DesignTool, refreshRow: () -> Unit) {
        // e9.l fallback DEX confirms the numeric/default and boolean dispatch that structured JADX loses.
        val number = if (tool is DesignTool.Number) { if (tool.value == tool.specialValue) 0 else tool.value } else 0
        val checked = if (tool is DesignTool.Toggle) tool.value else false
        val value = model.favoriteDesign
        when (tool.id) {
            1 -> value.width = number
            2 -> value.height = number
            3 -> value.radius = number
            4 -> { value.iconDisabled = checked; model.preferences.markDirty(html = true, style = false) }
            5 -> { value.colorDisabled = checked; model.preferences.markDirty(html = true, style = false) }
            else -> return
        }
        model.saveFavorites()
    }
}
