package moe.via.browser.feature.home.render

import moe.via.browser.feature.home.model.HomepageConfig

internal data class HomeColors(
    val foreground: String,
    val tapHighlight: String,
    val stroke: String,
    val fill: String,
    val input: String,
    val suggestionFill: String,
    val suggestionText: String
) {
    companion object {
        /** GP Li6/o;.b, 002c..01a8. The two palettes are selected again by prefers-color-scheme. */
        fun palettes(config: HomepageConfig): Pair<HomeColors, HomeColors> {
            val foreground = if (config.isLight) "#1b1b1b" else "#fafafa"
            val fill = "rgba(255, 255, 255, ${config.searchBarAlpha})"
            val darkFill = "rgba(${if (config.isLight) "0, 0, 0" else "255, 255, 255"}, ${config.searchBarAlpha})"
            val input = if (config.searchBarAlpha.toDouble() < 0.5) foreground else "#1b1b1b"
            val darkInput = if (!config.isLight && config.searchBarAlpha.toDouble() >= 0.5) "#1b1b1b" else "#afafaf"
            // 0111 branches to Double(0.85) when alpha is lower, not when it is higher.
            val suggestionAlpha = if (config.searchBarAlpha.toDouble() < 0.85) "0.85" else config.searchBarAlpha.toString()
            // 0138/013a and 0161/0163 require a dark image background, not isLight.
            val darkImage = !config.isLight && !config.backgroundPath.isNullOrEmpty()
            val lightSuggestion = if (config.usingBlur) fill else "rgba(255, 255, 255, $suggestionAlpha)"
            val darkSuggestion = if (config.usingBlur) darkFill else
                "rgba(${if (darkImage) "255, 255, 255" else "0, 0, 0"}, $suggestionAlpha)"
            val normal = HomeColors(
                foreground,
                if (config.isLight) "rgba(0,0,0,0.1)" else "rgba(255,255,255,0.1)",
                "rgba(${if (config.isLight) "0, 0, 0" else "255, 255, 255"}, ${config.searchBarStrokeAlpha})",
                fill, input, lightSuggestion, if (config.usingBlur) input else "#1b1b1b"
            )
            val night = HomeColors(
                "#afafaf", "rgba(255,255,255,0.1)", "rgba(233, 233, 233, ${config.searchBarStrokeAlpha})",
                darkFill, darkInput, darkSuggestion,
                if (config.usingBlur) darkInput else if (darkImage) "#1b1b1b" else "#afafaf"
            )
            return if (config.isNight) night to normal else normal to night
        }
    }
}
