package moe.pure.browser.feature.home.model

/** Stored flags use the reference bit layout. Dimensions become CSS pixels only in HomepageConfig. */
data class HomeDesign(
    val background: BackgroundDesign = BackgroundDesign(),
    val general: GeneralDesign = GeneralDesign(),
    val favorites: FavoriteDesign = FavoriteDesign(),
    val logo: LogoDesign = LogoDesign(),
    val search: SearchDesign = SearchDesign()
)

private fun Int.field(shift: Int, mask: Int = 127) = (this ushr shift) and mask
private fun Int.replace(value: Int, shift: Int, mask: Int = 127): Int {
    require(value in 0..mask)
    return (this and (mask shl shift).inv()) or (value shl shift)
}
private fun Int.bit(index: Int) = this and (1 shl index) != 0
private fun Int.replaceBit(index: Int, value: Boolean) = if (value) this or (1 shl index) else this and (1 shl index).inv()

data class BackgroundDesign(var flag: Int = DEFAULT) {
    var opacity: Int
        get() = flag.field(0)
        set(value) { flag = flag.replace(value, 0) }
    var overrideLight: Boolean
        get() = flag.bit(7)
        set(value) { flag = flag.replaceBit(7, value) }
    var light: Boolean
        get() = flag.bit(8)
        set(value) { flag = flag.replaceBit(8, value) }
    companion object { const val DEFAULT = 0 }
}

data class GeneralDesign(var flag: Int = DEFAULT) {
    var blur: Boolean
        get() = flag.bit(0)
        set(value) { flag = flag.replaceBit(0, value) }
    var searchPartHidden: Boolean
        get() = flag.bit(1)
        set(value) { flag = flag.replaceBit(1, value) }
    val reservedOpacity: Int get() = flag.field(2)
    companion object { const val DEFAULT = 0 }
}

data class FavoriteDesign(var flag: Int = DEFAULT) {
    var width: Int
        get() = flag.field(0)
        set(value) { flag = flag.replace(value, 0) }
    var height: Int
        get() = flag.field(7)
        set(value) { flag = flag.replace(value, 7) }
    var radius: Int
        get() = flag.field(14)
        set(value) { flag = flag.replace(value, 14) }
    var iconDisabled: Boolean
        get() = flag.bit(21)
        set(value) { flag = flag.replaceBit(21, value) }
    var colorDisabled: Boolean
        get() = flag.bit(22)
        set(value) { flag = flag.replaceBit(22, value) }
    companion object { const val DEFAULT = 1638446 }
}

data class LogoDesign(var flag: Int = DEFAULT) {
    var width: Int
        get() = flag.field(0)
        set(value) { flag = flag.replace(value, 0) }
    var height: Int
        get() = flag.field(7)
        set(value) { flag = flag.replace(value, 7) }
    var radius: Int
        get() = flag.field(14)
        set(value) { flag = flag.replace(value, 14) }
    var fontSize: Int
        get() = flag.field(21, 63)
        set(value) { flag = flag.replace(value, 21, 63) }
    var bold: Boolean
        get() = flag.bit(27)
        set(value) { flag = flag.replaceBit(27, value) }
    var italic: Boolean
        get() = flag.bit(28)
        set(value) { flag = flag.replaceBit(28, value) }
    fun nextTextStyle() {
        val next = ((if (bold) 1 else 0) or (if (italic) 2 else 0)) + 1
        bold = next and 1 != 0
        italic = next and 2 != 0
    }
    companion object { const val DEFAULT = 193766400 }
}

data class SearchDesign(var flag: Int = DEFAULT) {
    var radius: Int
        get() = flag.field(0)
        set(value) { flag = flag.replace(value, 0) }
    var opacity: Int
        get() = flag.field(7)
        set(value) { flag = flag.replace(value, 7) }
    var strokeOpacity: Int
        get() = flag.field(14)
        set(value) { flag = flag.replace(value, 14) }
    var strokeWidth: Int
        get() = flag.field(21, 7)
        set(value) { flag = flag.replace(value, 21, 7) }
    var line: Boolean
        get() = flag.bit(24)
        set(value) { flag = flag.replaceBit(24, value) }
    companion object { const val DEFAULT = 2490468 }
}

enum class LogoMode(val id: Int) {
    DEFAULT(0), IMAGE(1), TEXT(2), HTML(3), NONE(4);
    companion object { fun fromId(id: Int) = entries.firstOrNull { it.id == id } ?: DEFAULT }
}

enum class HomeDraftKind(val fileName: String) { TEXT("KEY_LOGO_TEXT"), HTML("KEY_LOGO_HTML") }
