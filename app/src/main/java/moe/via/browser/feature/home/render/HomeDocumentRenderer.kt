package moe.via.browser.feature.home.render

import android.content.Context
import android.util.Base64
import moe.via.browser.feature.home.model.HomeFavoriteTile
import moe.via.browser.feature.home.model.HomepageConfig

/** Shared by the actual home document and the live customization preview. Does not read preferences or the DB. */
class HomeDocumentRenderer(context: Context) {
    private val app = context.applicationContext
    private val layout by lazy { app.assets.open("home/layout.css").bufferedReader(Charsets.UTF_8).use { it.readText() } }
    private val script by lazy { app.assets.open("home/home.js").bufferedReader(Charsets.UTF_8).use { it.readText() } }
    private val suggestions by lazy { app.assets.open("home/suggestions.js").bufferedReader(Charsets.UTF_8).use { it.readText() } }
    private val suggestionBind by lazy { app.assets.open("home/suggestions-bind.js").bufferedReader(Charsets.UTF_8).use { it.readText() } }
    private val defaultLogo by lazy {
        val bytes = app.assets.open("home/default-logo.svg").use { it.readBytes() }
        "data:image/svg+xml;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun emitCss(output: Appendable, config: HomepageConfig) {
        val (main, opposite) = HomeColors.palettes(config)
        val border = if (config.searchBarLineEnabled) "border-bottom" else "border"
        val cell = config.favoriteItemWidth + 18
        val grid = buildString {
            if (cell > 0) for (column in 1..(540 / cell)) {
                val offset = if (config.searchBarEnabled) 1 else 0
                append("@media only screen and (min-width: ${(column + offset) * cell}px) { #box_container { width: ${column * cell}px } }\n")
            }
        }
        val image = if (config.logoPath.isNotEmpty()) buildString {
            append("img.smaller { ")
            if (config.logoWidth != 0) append("width: ${config.logoWidth}px; ")
            if (config.logoHeight != 0) append("height: ${config.logoHeight}px; ")
            append("border-radius: ${config.logoRadius}px; object-fit: cover; }")
        } else ""
        val position = if (config.searchBarEnabled) """
            #content { top:25%; transition:0.14s; }
            @media only screen and (min-height:250px) { #content { top:62px } }
            @media only screen and (min-height:350px) { #content { top:87px } }
            @media only screen and (min-height:450px) { #content { top:135px } }
            @media only screen and (min-height:650px) { #content { top:195px } }
            @media only screen and (min-height:850px) { #content { top:255px } }
        """.trimIndent() else "#content { top:18px; } .search_part { display:none; }"
        val replacements = mapOf(
            "TAP" to main.tapHighlight, "FOREGROUND" to main.foreground,
            "LOGO_FONT" to config.logoFontSize.toString(),
            "LOGO_WEIGHT" to if (config.isLogoBold) "font-weight:bold;" else "",
            "LOGO_STYLE" to if (config.isLogoItalic) "font-style:italic;" else "",
            "LOGO_IMAGE_RULE" to image, "NIGHT_IMAGE_RULE" to if (config.isNight) DIM_IMAGES else "",
            "SEARCH_RADIUS" to config.searchBarRadius.toString(), "BORDER" to border,
            "STROKE_WIDTH" to config.searchBarStrokeWidth.toString(), "STROKE" to main.stroke,
            "FILL" to main.fill, "INPUT" to main.input,
            "BLUR" to if (config.usingBlur) "backdrop-filter:blur(10px);" else "",
            "SUGGESTION_FILL" to main.suggestionFill, "SUGGESTION_TEXT" to main.suggestionText,
            "FAVORITE_WIDTH" to config.favoriteItemWidth.toString(),
            "FAVORITE_HEIGHT" to config.favoriteItemHeight.toString(),
            "FAVORITE_RADIUS" to config.favoriteItemRadius.toString(),
            "FAVORITE_TEXT" to if (config.favoriteColorDisabled) main.foreground else "#ffffff",
            "GRID_RULES" to grid, "POSITION_RULES" to position,
            "RTL_RULE" to if (config.isRtl) "html { direction:rtl; } #box_container { text-align:right; } #search_submit { float:left; }" else ""
        )
        val base = PLACEHOLDER.replace(layout) { replacements.getValue(it.groupValues[1]) }
        with(output) {
            append(base)
            append("@media (prefers-color-scheme:${if (config.isNight) "light" else "dark"}) {\n")
            append("html { -webkit-tap-highlight-color:${opposite.tapHighlight}; }\n")
            append(".logo { color:${opposite.foreground}; }\n")
            append(".opSug_wpr td { color:${opposite.input}; }\n")
            append(".title { color:${if (config.favoriteColorDisabled) opposite.foreground else "#ffffff"}; }\n")
            append(".url { color:${opposite.foreground}; }\n")
            append(".search_bar { $border:${config.searchBarStrokeWidth}px solid ${opposite.stroke}; background:${opposite.fill}; }\n")
            append(".search_bar { color:${opposite.input}; }\n")
            // Native leaves the base brightness rule in place when a night page matches light mode.
            if (!config.isNight) append(DIM_IMAGES).append('\n')
            append(".opSug_wpr { background:${opposite.suggestionFill}; border:${config.searchBarStrokeWidth}px solid ${opposite.stroke}; }\n")
            append(".search_bar_active { color:${opposite.suggestionText}; background:${opposite.suggestionFill}; }\n")
            append(".opSug_wpr td { color:${opposite.suggestionText}; }\n}\n")
            config.additionalStyleSheet?.let { append(it).append('\n') }
        }
    }

    fun emitHtml(
        output: Appendable,
        config: HomepageConfig,
        cssUrl: String,
        logoContent: String?,
        favorites: List<HomeFavoriteTile>,
        options: HomeRenderOptions
    ) = with(output) {
        append("<!DOCTYPE html><html><head><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\"/>")
        append("<meta name=\"color-scheme\" content=\"light dark\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, user-scalable=no, minimal-ui\">")
        if (options.pageTitle.isNotEmpty()) append("<title>").append(options.pageTitle).append("</title>")
        append("<link rel=\"stylesheet\" href=\"").append(cssUrl).append("\"></head>")
        append("<body><div class='frosted-glass' id='gesture-indicator'></div><div id=\"content\"><div class=\"search_part\">")
        append("<a class=\"logo\" href=\"\" onclick=\"javascript:window.via.cmd(257);\" title=\"")
        append(options.bookmarkTitle).append("\">")
        // taghome is deliberately a raw HTML fragment, including the Native text/HTML modes.
        append(if (logoContent.isNullOrEmpty()) "<img class=\"smaller\" src=\"$defaultLogo\" />" else logoContent)
        append("</a><div class=\"search_bar_wrapper\"><form onsubmit=\"return search()\" class=\"search_bar\" title=\"")
        append(options.searchTitle).append("\"><button onclick=\"search()\" id=\"search_submit\" value=\"\" aria-label=\"")
        append(options.searchTitle).append("\"><div class=\"search icon\"></div></button><span>")
        append("<input class=\"search\" onfocus=\"showButton()\" onblur=\"hideButton()\" type=\"text\" value=\"\" autocomplete=\"off\" id=\"search_input\" title=\"")
        append(options.searchTitle).append("\"></span></form><button type=\"button\" id=\"search_bar_trigger\" aria-label=\"")
        append(options.searchTitle).append("\"></button></div></div>")
        if (favorites.isNotEmpty()) {
            append("<div id=\"bookmark_part\"><div id=\"box_container\">")
            for (tile in favorites) append(favorite(tile, config))
            append("</div></div>")
        }
        append("<script type=\"text/javascript\">").append(script).append("</script>")
        append("<script type=\"text/javascript\">").append(suggestions).append("</script>")
        append("<script type=\"text/javascript\">").append(suggestionBind).append("</script></div></body></html>")
    }

    private fun favorite(tile: HomeFavoriteTile, config: HomepageConfig): String = buildString {
        append("<div class=\"box\"><p class=\"title\" aria-hidden=\"true\" ")
        if (tile.color.isNotEmpty() && !config.favoriteColorDisabled) append(" style=\"background:").append(tile.color).append(";\"")
        append('>').append(tile.letter).append("</p><div class=\"overlay\"")
        if (tile.iconPath.isNotEmpty() && !config.favoriteIconDisabled) {
            append(" style=\"background: url('").append(tile.iconPath)
            append("') no-repeat;background-size: cover;background-position:center center;\"")
        }
        // The DTO applies the Native tag stripping and href quote replacement before rendering.
        append("></div><p class=\"url\" aria-hidden=\"true\">").append(tile.title)
        append("</p><a href=\"").append(tile.href).append("\" title=\"").append(tile.title).append("\"></a></div>")
    }

    private companion object {
        val PLACEHOLDER = Regex("\\{\\{([A-Z_]+)\\}\\}")
        const val DIM_IMAGES = "img.smaller, .overlay, .title { -webkit-filter:brightness(75%); filter:brightness(75%); }"
    }
}
