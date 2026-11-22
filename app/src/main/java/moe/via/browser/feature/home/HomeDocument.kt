package moe.via.browser.feature.home

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import moe.via.browser.feature.home.data.HomePreferences
import moe.via.browser.feature.home.render.HomeDocumentCache
import moe.via.browser.feature.home.render.HomeDocumentFiles
import moe.via.browser.feature.home.render.HomeHtmlSource
import moe.via.browser.feature.home.render.HomeRenderContext
import moe.via.browser.feature.home.web.HomeFileResources
import moe.via.browser.feature.home.web.HomePageLocation
import moe.via.browser.feature.search.resolve.SearchUrlResolver

object HomeDocument {

    fun startUrl(context: Context): String {
        val configured = HomePreferences(context).homepage.trim()
        return if (configured.isEmpty()) url(context) else if (isHome(context, configured)) normalize(context, configured)
        else SearchUrlResolver.normalize(configured)
    }

    fun url(context: Context): String = HomePageLocation.url(context)
    fun isHome(context: Context, url: String?): Boolean = HomePageLocation.isHome(context, url)
    fun normalize(context: Context, url: String): String = HomePageLocation.normalize(context, url)
    fun engineUrl(context: Context, url: String): String = normalize(context, url)
    fun logicalUrl(context: Context, url: String): String = normalize(context, url)

    /** Source and normal navigation use the same generated file. Call from a file/resource worker. */
    fun sourceUrl(context: Context): String {
        return files(context).htmlUrl
    }
    fun response(context: Context, uri: Uri, night: Boolean): WebResourceResponse? {
        val document = HomePageLocation.generated(context, uri)
        val stylesheet = HomePageLocation.isStylesheet(context, uri)
        val image = if (!document && !stylesheet) HomeFileResources.image(context, uri) else null
        if (!document && !stylesheet && image == null) return null
        return runCatching {
            when {
                document -> HomeFileResources.response("text/html", files(context, night).htmlFile)
                // The main document generated both files before its stylesheet was requested.
                stylesheet -> HomeFileResources.response("text/css", HomePageLocation.stylesheet(context))
                else -> image?.let { HomeFileResources.response(it.mime, it.file) } ?: HomeFileResources.missing()
            }
        }.getOrElse { HomeFileResources.missing() }
    }

    /** Called by WebView's resource worker, or the persistence worker for view-source. */
    private fun files(context: Context, night: Boolean? = null): HomeDocumentFiles {
        val app = context.applicationContext
        val preferences = HomePreferences(app)
        val options = HomeRenderContext.options(context)
        val isRtl = HomeRenderContext.isRtl(context)
        val config = preferences.config(night, isRtl = isRtl)
        return HomeDocumentCache(app).render(config, options) { HomeHtmlSource.read(app) }
    }
}
