package moe.pure.browser.feature.home.render

import android.content.Context
import moe.pure.browser.feature.home.model.HomepageConfig
import moe.pure.browser.feature.internalpages.GeneratedPageDirtyState
import java.io.File

/** r8.d uses these same two files in the browser and designer; each branch consumes its own bit. */
internal class HomeDocumentCache(context: Context) {
    private val emitter = HomeDocumentEmitter(HomeDocumentRenderer(context.applicationContext))
    private val cssFile = File(context.filesDir, "homepage.css")
    private val htmlFile = File(context.filesDir, "homepage2.html")

    fun style(config: HomepageConfig): File {
        if (GeneratedPageDirtyState.isDirty(GeneratedPageDirtyState.HOME_STYLE) || !cssFile.exists()) {
            emitter.style(cssFile, config)
            GeneratedPageDirtyState.clear(GeneratedPageDirtyState.HOME_STYLE)
        }
        return cssFile
    }

    fun html(config: HomepageConfig, options: () -> HomeRenderOptions, inputs: () -> HomeHtmlInputs): File {
        if (GeneratedPageDirtyState.isDirty(GeneratedPageDirtyState.HOME) || !htmlFile.exists()) {
            emitter.html(htmlFile, cssFile.absolutePath, config, inputs(), options())
            GeneratedPageDirtyState.clear(GeneratedPageDirtyState.HOME)
        }
        return htmlFile
    }

    /** r8.d.f runs sequentially; r8.d.d lets the preview zip these two independent branches. */
    fun render(config: HomepageConfig, options: HomeRenderOptions, inputs: () -> HomeHtmlInputs): HomeDocumentFiles {
        val css = style(config)
        val html = html(config, { options }, inputs)
        return HomeDocumentFiles(html, css, "file://${html.absolutePath}", "file://${css.absolutePath}")
    }
}
