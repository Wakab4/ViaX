package moe.pure.browser.feature.home.ui.customize.tools

import moe.pure.browser.feature.home.ui.customize.DesignTool

internal interface HomeToolSource {
    fun items(): List<DesignTool>
    fun apply(tool: DesignTool, refreshRow: () -> Unit)
}
