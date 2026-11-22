package moe.via.browser.feature.site.data

import moe.via.browser.feature.site.SiteFeature

data class SiteCommitResult(val key: String, val changed: Boolean, val flags: Int)
enum class SiteRenameResult { RENAMED, UNCHANGED, CONFLICT, MISSING, INVALID, FAILED }

/** u4 edits a copy. No repository write occurs until its page owner commits on departure. */
class SiteDraft(val key: String, val launchFlags: Int = 0, var config: NativeSiteConfig = NativeSiteConfig()) {
    var resultFlags = 0
    fun set(feature: SiteFeature, value: Int?) { config = config.withFeature(feature, value) }
    fun setUserAgent(choice: Int, text: String?) { config = config.withUserAgent(choice, text) }
    fun reset() { config = NativeSiteConfig() }
}
