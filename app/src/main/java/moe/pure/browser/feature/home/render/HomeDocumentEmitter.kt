package moe.pure.browser.feature.home.render

import moe.pure.browser.feature.home.model.HomepageConfig
import java.io.File

/** The two i6.o emitters return independently; no atomic-file result gates their dirty clear. */
internal class HomeDocumentEmitter(private val renderer: HomeDocumentRenderer) {
    fun style(file: File, config: HomepageConfig) {
        val writer = NativeHomeFileWriter(file)
        writer.replace()
        renderer.emitCss(writer, config)
        writer.close()
    }

    fun html(file: File, cssPath: String, config: HomepageConfig, inputs: HomeHtmlInputs, options: HomeRenderOptions) {
        val writer = NativeHomeFileWriter(file)
        writer.replace()
        renderer.emitHtml(writer, config, cssPath, inputs.logoContent, inputs.favorites, options)
        writer.close()
    }
}
