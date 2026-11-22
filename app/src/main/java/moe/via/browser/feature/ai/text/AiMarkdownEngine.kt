package moe.via.browser.feature.ai.text

import android.content.Context
import android.text.Spanned
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.LinkResolver
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.core.MarkwonTheme
import moe.via.browser.feature.ai.text.table.AiTablePlugin
import moe.via.browser.feature.ai.text.table.AiTableStyle
import moe.via.browser.ui.components.dp

/** One parser for display and database previews; browser links are supplied by the UI owner. */
class AiMarkdownEngine(context: Context, resolver: LinkResolver? = null) {
    private val markdown = Markwon.builder(context)
        .usePlugin(AiTablePlugin(AiTableStyle(context)))
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                builder.headingBreakHeight(context.dp(1)).thematicBreakHeight(context.dp(1))
                    .codeBackgroundColor(0x20808080).codeBlockBackgroundColor(0x20808080)
            }
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                if (resolver != null) builder.linkResolver(resolver)
            }
        }).build()

    fun render(text: String): Spanned = markdown.toMarkdown(text)
    fun plain(text: String): String = render(text).toString()
}
