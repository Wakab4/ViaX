package moe.pure.browser.feature.site.data

import moe.pure.browser.feature.site.SiteDecision
import moe.pure.browser.feature.site.SiteFeature

/** ba.b's independent enabled/value words. */
data class NativeSiteConfig(
    val userAgentChoice: Int = -1000,
    val customUserAgent: String? = null,
    val enabledFlags: Int = 0,
    val flags: Int = 0,
    val textSize: Int = 0
) {
    val enabled: Boolean get() = boolean(1, false)
    fun isEmpty(): Boolean = customUserAgent.isNullOrEmpty() && userAgentChoice == -1000 &&
        enabledFlags shr 1 == 0 && flags shr 1 == 0 && textSize == 0

    fun boolean(mask: Int, fallback: Boolean): Boolean =
        if (enabledFlags and mask != 0) flags and mask != 0 else fallback

    fun withEnabled(value: Boolean): NativeSiteConfig = bit(1, true, value)

    fun withUserAgent(choice: Int, text: String?): NativeSiteConfig = copy(userAgentChoice = choice,
        customUserAgent = text?.takeIf { it.isNotEmpty() }?.replace(Regex("[\\t\\n\\r]+"), " ")?.trim { it <= ' ' })

    fun withFeature(feature: SiteFeature, value: Int?): NativeSiteConfig {
        if (feature == SiteFeature.USER_AGENT) return copy(userAgentChoice = value ?: -1000)
        if (feature == SiteFeature.FONT_SIZE) return copy(textSize = value ?: 0)
        BOOLEAN_MASKS[feature]?.let { mask -> return bit(mask, value != null, value == SiteDecision.ALLOW) }
        if (feature == SiteFeature.REDIRECTS) return bit(1024, value != null, value != null && value != SiteDecision.ALLOW)
        PERMISSION_MASKS[feature]?.let { (deny, ask) ->
            return when (value) {
                SiteDecision.ALLOW -> bit(ask, false, false).bit(deny, true, false)
                SiteDecision.DENY -> bit(ask, false, false).bit(deny, true, true)
                SiteDecision.ASK -> bit(ask, true, true).bit(deny, false, false)
                else -> bit(deny or ask, false, false)
            }
        }
        return this
    }

    /** Projection is independent of master enable: disabled drafts still display their explicit values. */
    fun feature(feature: SiteFeature): Int? = when (feature) {
        SiteFeature.USER_AGENT -> userAgentChoice.takeIf { it != -1000 }
        SiteFeature.FONT_SIZE -> textSize.takeIf { it > 0 }
        SiteFeature.REDIRECTS -> if (enabledFlags and 1024 == 0) null else
            if (flags and 1024 == 0) SiteDecision.ALLOW else SiteDecision.ASK
        else -> BOOLEAN_MASKS[feature]?.let { mask ->
            if (enabledFlags and mask == 0) null else if (flags and mask != 0) SiteDecision.ALLOW else SiteDecision.DENY
        } ?: PERMISSION_MASKS[feature]?.let { (deny, ask) ->
            when {
                boolean(ask, false) -> SiteDecision.ASK
                boolean(deny, false) -> SiteDecision.DENY
                !boolean(deny, true) -> SiteDecision.ALLOW
                else -> null
            }
        }
    }

    fun projection(): Map<SiteFeature, Int> = SiteFeature.entries.mapNotNull { feature -> this.feature(feature)?.let { feature to it } }.toMap()

    /** Native ba.b.equals deliberately compares only its five known fields. */
    fun sameNative(other: NativeSiteConfig): Boolean = userAgentChoice == other.userAgentChoice &&
        customUserAgent == other.customUserAgent && enabledFlags == other.enabledFlags && flags == other.flags && textSize == other.textSize

    private fun bit(mask: Int, explicit: Boolean, value: Boolean) = copy(
        enabledFlags = if (explicit) enabledFlags or mask else enabledFlags and mask.inv(),
        flags = if (value) flags or mask else flags and mask.inv())

    companion object {
        val BOOLEAN_MASKS = mapOf(SiteFeature.JAVASCRIPT to 2, SiteFeature.IMAGES to 4, SiteFeature.DESKTOP to 8,
            SiteFeature.ADBLOCK to 16, SiteFeature.INCOGNITO to 32, SiteFeature.BACK_CACHE to 131072)
        val PERMISSION_MASKS = mapOf(SiteFeature.CLIPBOARD to (64 to 128), SiteFeature.OPEN_APPS to (256 to 512),
            SiteFeature.MICROPHONE to (2048 to 4096), SiteFeature.CAMERA to (8192 to 16384), SiteFeature.LOCATION to (65536 to 32768))

        fun fromProjection(enabled: Boolean, values: Map<SiteFeature, Int>, custom: String? = null): NativeSiteConfig {
            var result = NativeSiteConfig().withEnabled(enabled)
            values.forEach { (feature, value) -> result = result.withFeature(feature, value) }
            return result.withUserAgent(result.userAgentChoice, custom)
        }
    }
}
