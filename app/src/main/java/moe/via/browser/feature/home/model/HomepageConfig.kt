package moe.via.browser.feature.home.model

/** Rendering values, with dimensions in CSS pixels and alpha in 0..1. Logo content is a separate input. */
data class HomepageConfig(
    val isRtl: Boolean,
    val isNight: Boolean,
    val accentColor: Int,
    val backgroundPath: String?,
    val backgroundOpacity: Float,
    val usingBlur: Boolean,
    val favoriteItemWidth: Int,
    val favoriteItemHeight: Int,
    val favoriteItemRadius: Int,
    val favoriteColorDisabled: Boolean,
    val favoriteIconDisabled: Boolean,
    val logoPath: String,
    val logoWidth: Int,
    val logoHeight: Int,
    val logoRadius: Int,
    val logoFontSize: Int,
    val isLogoBold: Boolean,
    val isLogoItalic: Boolean,
    val searchBarEnabled: Boolean,
    val searchBarRadius: Int,
    val searchBarAlpha: Float,
    val searchBarStrokeWidth: Int,
    val searchBarStrokeAlpha: Float,
    val searchBarLineEnabled: Boolean,
    val isLight: Boolean,
    val additionalStyleSheet: String?
) {
    companion object {
        fun from(
            design: HomeDesign, night: Boolean, accentColor: Int, isRtl: Boolean,
            backgroundPath: String?, additionalStyleSheet: String?, defaultLogoPath: String
        ): HomepageConfig {
            val favorite = design.favorites
            val height = favorite.height.takeIf { it != 0 } ?: favorite.width.takeIf { it != 0 } ?: 54
            val width = favorite.width.takeIf { it != 0 } ?: height
            val logo = design.logo
            val logoHeight = if (logo.width == 0 && logo.height == 0) 72 else logo.height
            val search = design.search
            val light = if (design.background.overrideLight) design.background.light else
                ((accentColor ushr 16 and 255) * 0.299 + (accentColor ushr 8 and 255) * 0.587 + (accentColor and 255) * 0.114) >= 192.0
            return HomepageConfig(
                isRtl, night, accentColor, backgroundPath, minOf(0.8f, design.background.opacity / 100f),
                design.general.blur, width, height, (minOf(width, height) / 2f * (favorite.radius / 100f)).toInt(),
                favorite.colorDisabled, favorite.iconDisabled, defaultLogoPath, logo.width, logoHeight,
                (maxOf(logo.width, logoHeight) / 2f * (logo.radius / 100f)).toInt(), logo.fontSize, logo.bold, logo.italic,
                !design.general.searchPartHidden, if (search.line) 0 else (search.radius / 100f * (search.strokeWidth + 23f)).toInt(),
                if (search.line) 0f else search.opacity / 100f, search.strokeWidth, search.strokeOpacity / 100f,
                search.line, light, additionalStyleSheet
            )
        }
    }
}
