package moe.via.browser.feature.home.ui.customize

import android.content.Context
import moe.via.browser.feature.home.ui.customize.palette.BackgroundPaletteEntry
import moe.via.browser.feature.home.ui.customize.tools.AdvancedDesignTools
import moe.via.browser.feature.home.ui.customize.tools.BackgroundDesignTools
import moe.via.browser.feature.home.ui.customize.tools.FavoriteDesignTools
import moe.via.browser.feature.home.ui.customize.tools.HomeToolSource
import moe.via.browser.feature.home.ui.customize.tools.LogoDesignTools
import moe.via.browser.feature.home.ui.customize.tools.SearchDesignTools

/** Each child owns one leaf definition; the parent model owns its persisted design state. */
internal class HomeDesignPanels(context: Context, page: DesignPage, model: HomeCustomizeState,
    commands: Commands, selectedBackground: () -> BackgroundPaletteEntry?) {
    interface Commands {
        fun chooseLogo(); fun chooseBackground(); fun editCss(); fun clearIcons(); fun reset()
    }
    private val source: HomeToolSource = when (page) {
        DesignPage.LOGO -> LogoDesignTools(context, model, commands)
        DesignPage.SEARCH -> SearchDesignTools(context, model)
        DesignPage.FAVORITES -> FavoriteDesignTools(context, model)
        DesignPage.BACKGROUND -> BackgroundDesignTools(context, model, commands, selectedBackground)
        DesignPage.ADVANCED -> AdvancedDesignTools(context, model, commands)
    }
    fun tools() = source.items()
    fun apply(tool: DesignTool, refreshRow: () -> Unit) = source.apply(tool, refreshRow)
}
