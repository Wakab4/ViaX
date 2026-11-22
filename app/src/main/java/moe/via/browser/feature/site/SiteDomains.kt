package moe.via.browser.feature.site

/** Compatibility facade; runtime lookup never lowercases, validates, or expands parent domains. */
object SiteDomains {
    fun fromUrl(url: String): String? = NativeSiteKeys.fromUrl(url).takeIf(String::isNotEmpty)
    fun normalize(input: String): String? = NativeSiteKeys.input(input).takeIf(String::isNotEmpty)
}
