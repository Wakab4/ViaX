package moe.pure.browser.feature.home.data

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.Base64
import android.view.View
import moe.pure.browser.feature.home.model.BackgroundDesign
import moe.pure.browser.feature.home.model.FavoriteDesign
import moe.pure.browser.feature.home.model.GeneralDesign
import moe.pure.browser.feature.home.model.HomeDesign
import moe.pure.browser.feature.home.model.HomeDraftKind
import moe.pure.browser.feature.home.model.HomepageConfig
import moe.pure.browser.feature.home.model.LogoDesign
import moe.pure.browser.feature.home.model.LogoMode
import moe.pure.browser.feature.home.model.SearchDesign
import moe.pure.browser.feature.internalpages.GeneratedPageDirtyState
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.core.logging.NativeLog

/** Base64 fields are plain configuration. Reads never query a database or fetch a resource. */
class HomePreferences(context: Context) {
    private val app = context.applicationContext
    val values: SharedPreferences = app.getSharedPreferences("browser", Context.MODE_PRIVATE)

    var backgroundDesign: BackgroundDesign
        get() = BackgroundDesign(values.getInt("bginfo", BackgroundDesign.DEFAULT))
        set(value) = change(html = false, style = true) { putInt("bginfo", value.flag) }
    var generalDesign: GeneralDesign
        get() = GeneralDesign(values.getInt("custominfo", GeneralDesign.DEFAULT))
        set(value) = change(html = false, style = true) { putInt("custominfo", value.flag) }
    var favoriteDesign: FavoriteDesign
        get() = FavoriteDesign(values.getInt("favinfo", FavoriteDesign.DEFAULT))
        set(value) = change(html = false, style = true) { putInt("favinfo", value.flag) }
    var logoDesign: LogoDesign
        get() = LogoDesign(values.getInt("logoinfo2", LogoDesign.DEFAULT))
        set(value) = change(html = false, style = true) { putInt("logoinfo2", value.flag) }
    var searchDesign: SearchDesign
        get() = SearchDesign(values.getInt("searchinfo", SearchDesign.DEFAULT))
        set(value) = change(html = false, style = true) { putInt("searchinfo", value.flag) }
    var design: HomeDesign
        get() = HomeDesign(backgroundDesign, generalDesign, favoriteDesign, logoDesign, searchDesign)
        set(value) = change(html = false, style = true) {
            putInt("bginfo", value.background.flag); putInt("custominfo", value.general.flag)
            putInt("favinfo", value.favorites.flag); putInt("logoinfo2", value.logo.flag); putInt("searchinfo", value.search.flag)
        }
    val logoChoice: Int get() = values.getInt("logochioce", 0)
    var logoMode: LogoMode
        get() = LogoMode.fromId(logoChoice)
        set(value) = change(html = true, style = false) { putInt("logochioce", value.id) }
    var logoContent: String?
        get() = decoded("taghome")
        set(value) = change(html = true, style = false) { encoded("taghome", value) }
    var tagHome: String?
        get() = logoContent
        set(value) { logoContent = value }
    var backgroundPath: String?
        get() = decoded("bghome")
        set(value) = change { encoded("bghome", value) }
    var additionalStyleSheet: String?
        get() = decoded("csstheme")
        set(value) = change(html = false, style = true) { encoded("csstheme", value) }
    var homepage: String
        get() = values.getString("home", "about:home").orEmpty()
        set(value) {
            change(html = false, style = false) { putString("home", value) }
            GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.WEB_SETTINGS)
        }

    /** The homepage reads the Native urlbarcolor value directly. */
    var accentColor: Int
        get() = values.getInt("urlbarcolor", -1)
        set(value) = change { putInt("urlbarcolor", value) }


    fun config(
        night: Boolean? = null, accentColor: Int? = null, isRtl: Boolean? = null,
        defaultLogoPath: String = DEFAULT_LOGO_PATH
    ): HomepageConfig = HomepageConfig.from(
        design, night ?: moe.pure.browser.ui.theme.BrowserNightMode.global(app), accentColor ?: this.accentColor,
        isRtl ?: (TextUtils.getLayoutDirectionFromLocale(app.resources.configuration.locales[0]) == View.LAYOUT_DIRECTION_RTL),
        backgroundPath, additionalStyleSheet, defaultLogoPath
    )

    fun setDefaultLogo() {
        writeRaw { putInt("logochioce", LogoMode.DEFAULT.id) }
        writeRaw { encoded("taghome", null) }
    }
    fun setImageLogo(path: String) {
        writeRaw { putInt("logochioce", LogoMode.IMAGE.id) }
        writeRaw { encoded("taghome", "<img class=\"smaller\" src=\"file://$path\" />") }
    }
    fun setTextLogo(text: String) {
        writeRaw { putInt("logochioce", LogoMode.TEXT.id) }
        writeRaw { encoded("taghome", text) }
    }
    fun setHtmlLogo(html: String) {
        writeRaw { encoded("taghome", html.ifEmpty { "<br>" }) }
        writeRaw { putInt("logochioce", LogoMode.HTML.id) }
    }
    fun hideLogo() {
        writeRaw { encoded("taghome", "<br>") }
        writeRaw { putInt("logochioce", LogoMode.NONE.id) }
    }

    /** The design actor separately owns the mask and light override bits. */
    fun setBackgroundColor(color: Int = -1) {
        writeRaw { encoded("bghome", "") }
        writeRaw { putInt("urlbarcolor", color) }
    }
    fun setBackgroundImageResult(path: String?, sampledColor: Int?) {
        writeRaw { putInt("urlbarcolor", if (sampledColor == null) -1 else if (sampledColor == 0) 0xFF000000.toInt() else sampledColor) }
        writeRaw { encoded("bghome", if (sampledColor == null) null else path) }
    }

    fun readDraft(kind: HomeDraftKind): String = HomeDraftStore(app).read(kind)
    internal fun rememberDraft(kind: HomeDraftKind, text: String) {
        BrowserExecutors.io.execute {
            try { HomeDraftStore(app).write(kind, text) } catch (error: Exception) { NativeLog.warn(null, error) }
        }
    }

    /** Reset design only. Favorite rows, imported images, drafts and icon cache have separate lifecycles. */
    internal fun resetCustomization(design: HomeDesign) = NativeHomeDesignReset.write(values, design)

    fun markDirty(html: Boolean = true, style: Boolean = true) {
        if (html) GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.HOME)
        if (style) GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.HOME_STYLE)
    }

    private fun decoded(key: String): String? = values.getString(key, null)?.let {
        try { String(Base64.decode(it, Base64.DEFAULT), Charsets.UTF_8).trim { character -> character <= ' ' } }
        catch (error: Exception) { error.printStackTrace(); "" }
    }
    private fun SharedPreferences.Editor.encoded(key: String, text: String?) {
        if (text == null) remove(key)
        else putString(key, Base64.encodeToString(text.trim { it <= ' ' }.toByteArray(Charsets.UTF_8), Base64.DEFAULT))
    }
    private fun change(html: Boolean = true, style: Boolean = true, write: SharedPreferences.Editor.() -> Unit) {
        writeRaw(write)
        markDirty(html, style)
    }
    private fun writeRaw(write: SharedPreferences.Editor.() -> Unit) { values.edit().apply(write).apply() }

    companion object {
        const val DEFAULT_LOGO_PATH = "data:image/svg+xml;base64,PHN2ZyBmaWxsPSJub25lIiBoZWlnaHQ9IjkwIiB2aWV3Qm94PSIwIDAgMTI0IDkwIiB3aWR0aD0iMTI0IiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxwYXRoIGQ9Im0yMS43Mzg0IDI2LjY3MzljLjE5MzktMS42MTY1IDEuMzQ4My0yLjk1NDEgMi45MTktMy4zODI1bDExLjM3NTYtMy4xMDI0YzEuMjcyMy0uMzQ3IDIuNTI2Mi42MTA4IDIuNTI2MiAxLjkyOTV2MTcuODgxNWwtMjEuMDU5MiAyMnoiIGZpbGw9IiMwMGE1ZjYiLz48cGF0aCBkPSJtNjcgNy41IDMuNSAyMy0zMS43MTE0IDM3Ljg2NDRjLTEuMDc0OSAxLjI4MzQtMi44MzU0IDEuNzYxOC00LjQxMiAxLjE5ODdsLTE1LjUzNDktNS41NDgyYy0xLjM0ODQtLjQ4MTYtMS43Njg4LTIuMTgyNS0uOC0zLjIzNjd6IiBmaWxsPSIjZmYzZTAwIi8+PHBhdGggZD0ibTY2LjcyMTQgOC4yOTQzYy0uMTExNC0uNjUxMDkuNDI3OS0xLjIyOTM3IDEuMDg1Mi0xLjE2MzY0bDE3LjM5MjEgMS43MzkyMWMuNzkwNS4wNzkwNSAxLjQ1OTMuNjE4NTMgMS43MDM5IDEuMzc0NDNsMjAuOTEwNCA2NC42MzM1Yy4zNjcgMS4xMzI4LS4zMzEgMi4zMzI5LTEuNDk3IDIuNTc0MWwtMjIuNzYxIDQuNzA5MWMtMi4yMTY1LjQ1ODYtNC4zNzE2LTEuMDExNi00Ljc1MzItMy4yNDI2eiIgZmlsbD0iI2ZmYjcwMCIvPjwvc3ZnPg=="
    }
}
