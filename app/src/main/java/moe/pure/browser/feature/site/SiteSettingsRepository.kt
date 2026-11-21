package moe.pure.browser.feature.site

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.data.BrowserStore
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.feature.site.data.NativeSiteConfig
import moe.pure.browser.feature.site.data.NativeSiteStores
import moe.pure.browser.feature.site.data.SiteDraft
import moe.pure.browser.feature.site.data.SiteCommitResult
import moe.pure.browser.feature.site.data.SiteRenameResult
import moe.pure.browser.feature.site.data.TemporarySiteConfig

/** Rules use explicit overrides; an absent row inherits the current global setting. */
class SiteSettingsRepository(context: Context, private val store: BrowserStore = BrowserStore(context),
    private val preferences: SharedPreferences = context.getSharedPreferences("browser", Context.MODE_PRIVATE)) : AutoCloseable {
    private val db get() = store.writableDatabase
    private val cache get() = NativeSiteStores.get(db)
    private val globalPreferences = BrowserPreferences(preferences)

    fun all(): List<SiteConfiguration> = cache.snapshot(db).toSortedMap().map { (key, value) -> project(key, value) }
    fun keys(): List<String> = cache.snapshot(db).keys.sorted()
    fun get(domain: String): SiteConfiguration? = getNative(domain)?.let { project(domain, it) }
    fun getNative(key: String, persistedOnly: Boolean = false): NativeSiteConfig? = cache.get(db, key, persistedOnly)
    /** Native h() reloads persisted records; temporary overrides never enter a backup snapshot. */
    fun snapshot(): Map<String, NativeSiteConfig> { cache.reload(db); return cache.snapshot(db) }

    fun forUrl(url: String): SiteConfiguration? = get(NativeSiteKeys.fromUrl(url))

    fun resolve(url: String, feature: SiteFeature, settings: BrowserSettings): Int {
        val config = forUrl(url)
        return if (feature.singleSite && config?.enabled == true) config.values[feature] ?: global(feature, settings) else global(feature, settings)
    }

    fun global(feature: SiteFeature, settings: BrowserSettings): Int {
        val prefs = settings.preferences
        return when (feature) {
            SiteFeature.FONT_SIZE -> settings.textZoom
            SiteFeature.USER_AGENT -> settings.userAgentId
            SiteFeature.IMAGES -> when (settings.imageMode) { 1 -> SiteDecision.DENY; 2 -> SiteDecision.WIFI; else -> SiteDecision.ALLOW }
            SiteFeature.JAVASCRIPT -> boolean(prefs.javascript)
            SiteFeature.DESKTOP -> boolean(prefs.desktop)
            SiteFeature.COOKIES -> boolean(prefs.cookies)
            SiteFeature.THIRD_PARTY_COOKIES -> boolean(prefs.thirdPartyCookies)
            SiteFeature.ADBLOCK -> boolean(prefs.adblock)
            SiteFeature.INCOGNITO -> boolean(prefs.incognito)
            SiteFeature.POPUPS -> if (prefs.popupAllowed) SiteDecision.ALLOW else SiteDecision.ASK
            SiteFeature.REDIRECTS -> if (prefs.redirectsAllowed) SiteDecision.ALLOW else SiteDecision.ASK
            SiteFeature.CAMERA -> if (prefs.cameraAsk) SiteDecision.ASK else SiteDecision.DENY
            SiteFeature.MICROPHONE -> if (prefs.microphoneAsk) SiteDecision.ASK else SiteDecision.DENY
            SiteFeature.LOCATION -> if (prefs.locationEnabled) SiteDecision.ASK else SiteDecision.DENY
            SiteFeature.CLIPBOARD -> prefs.clipboardPolicy
            SiteFeature.OPEN_APPS -> prefs.openAppsPolicy
            SiteFeature.VIBRATION -> boolean(prefs.vibrationAllowed)
            SiteFeature.BACK_CACHE -> boolean(prefs.backCache)
        }
    }

    fun setGlobal(feature: SiteFeature, value: Int) {
        val prefs = globalPreferences
        when (feature) {
            SiteFeature.FONT_SIZE -> prefs.textZoom = value
            SiteFeature.USER_AGENT -> prefs.userAgentId = value
            SiteFeature.IMAGES -> prefs.imageMode = when (value) { SiteDecision.DENY -> 1; SiteDecision.WIFI -> 2; else -> 0 }
            SiteFeature.JAVASCRIPT -> prefs.javascript = value == SiteDecision.ALLOW
            SiteFeature.DESKTOP -> prefs.desktop = value == SiteDecision.ALLOW
            SiteFeature.COOKIES -> prefs.cookies = value == SiteDecision.ALLOW
            SiteFeature.THIRD_PARTY_COOKIES -> prefs.thirdPartyCookies = value == SiteDecision.ALLOW
            SiteFeature.ADBLOCK -> prefs.adblock = value == SiteDecision.ALLOW
            SiteFeature.INCOGNITO -> prefs.incognito = value == SiteDecision.ALLOW
            SiteFeature.POPUPS -> prefs.popupAllowed = value == SiteDecision.ALLOW
            SiteFeature.REDIRECTS -> prefs.redirectsAllowed = value == SiteDecision.ALLOW
            SiteFeature.CAMERA -> prefs.cameraAsk = value != SiteDecision.DENY
            SiteFeature.MICROPHONE -> prefs.microphoneAsk = value != SiteDecision.DENY
            SiteFeature.LOCATION -> prefs.locationEnabled = value != SiteDecision.DENY
            SiteFeature.CLIPBOARD -> prefs.clipboardPolicy = value
            SiteFeature.OPEN_APPS -> prefs.openAppsPolicy = value
            SiteFeature.VIBRATION -> prefs.vibrationAllowed = value == SiteDecision.ALLOW
            SiteFeature.BACK_CACHE -> prefs.backCache = value == SiteDecision.ALLOW
        }
    }

    fun editDraft(key: String, launchFlags: Int = 0): SiteDraft {
        val source = cache.editBase(db, key)
        var config = source.copy(customUserAgent = source.customUserAgent?.takeIf(String::isNotEmpty),
            textSize = source.textSize.coerceAtLeast(0))
        if (config.isEmpty() && launchFlags and 4 != 0) config = config.withEnabled(true)
        return SiteDraft(key, launchFlags, config)
    }

    fun pendingCommit(draft: SiteDraft): SiteCommitResult {
        val old = getNative(draft.key, persistedOnly = true)
        val differs = if (old == null) !draft.config.isEmpty() else !old.sameNative(draft.config)
        val flags = (if (differs || draft.launchFlags and 1 != 0) 1 else 0) or (draft.resultFlags and 2)
        return SiteCommitResult(draft.key, differs, flags)
    }

    fun commit(draft: SiteDraft): SiteCommitResult {
        val result = pendingCommit(draft)
        if (result.changed) {
            if (draft.config.isEmpty()) cache.delete(db, draft.key) else cache.put(db, draft.key, draft.config)
        }
        if (result.changed) changed(true)
        return result
    }

    fun rename(oldKey: String, newKey: String): SiteRenameResult {
        val result = try { cache.rename(db, oldKey, newKey) } catch (_: Exception) { SiteRenameResult.FAILED }
        if (result == SiteRenameResult.RENAMED) changed(true)
        return result
    }

    fun writeFeatureKey(key: String, feature: SiteFeature, value: Int?, customUserAgent: String? = null): Boolean {
        if (key.isEmpty()) return false
        var config = cache.editBase(db, key).withFeature(feature, value)
        if (value != null) config = config.withEnabled(true)
        if (feature == SiteFeature.USER_AGENT && value != null)
            config = config.withUserAgent(value, customUserAgent)
        val result = if (config.isEmpty()) cache.delete(db, key) else cache.put(db, key, config)
        changed(feature in RELOAD_FEATURES)
        return result
    }

    /** GP jb.u/jb.v2 remove O(0), retaining the camera/microphone rule in storage. */
    fun removePermissionPageKey(key: String, feature: SiteFeature): Boolean = writeFeatureKey(key,
        if (feature == SiteFeature.CAMERA || feature == SiteFeature.MICROPHONE) SiteFeature.CLIPBOARD else feature, null)

    fun setEnabled(domain: String, enabled: Boolean) {
        val config = cache.editBase(db, domain).withEnabled(enabled)
        if (config.isEmpty()) cache.delete(db, domain) else cache.put(db, domain, config)
        changed(true)
    }
    fun set(domain: String, feature: SiteFeature, value: Int?) { writeFeatureKey(domain, feature, value) }
    fun remove(domain: String) { cache.delete(db, domain); changed(true) }
    fun remove(domain: String, feature: SiteFeature) { writeFeatureKey(domain, feature, null) }
    fun setRedirectKey(key: String, allow: Boolean) { writeFeatureKey(key, SiteFeature.REDIRECTS, if (allow) SiteDecision.ALLOW else SiteDecision.ASK) }
    fun setImageKey(key: String, allow: Boolean) { writeFeatureKey(key, SiteFeature.IMAGES, if (allow) SiteDecision.ALLOW else SiteDecision.DENY) }
    fun removeRedirectKey(key: String) { writeFeatureKey(key, SiteFeature.REDIRECTS, null) }
    fun removeImageKey(key: String) { writeFeatureKey(key, SiteFeature.IMAGES, null) }
    fun setUserAgent(domain: String, id: Int, text: String?) {
        cache.put(db, domain, cache.editBase(db, domain).withEnabled(true).withUserAgent(id, text))
        changed(true)
    }
    fun installTemporary(key: String, config: NativeSiteConfig, expiresAt: Long = 0, mode: Int = 0) =
        cache.installTemporary(key, TemporarySiteConfig(config, expiresAt, mode))
    fun removeTemporary(key: String) = cache.removeTemporary(key)
    private fun changed(reload: Boolean) {
        preferences.edit().putLong("site.revision", System.nanoTime()).apply {
            if (reload) putLong("site.reload_revision", System.nanoTime())
        }.apply()
    }
    private fun project(key: String, config: NativeSiteConfig) = SiteConfiguration(key, config.enabled,
        config.projection(), config.customUserAgent, config)
    private fun boolean(enabled: Boolean) = if (enabled) SiteDecision.ALLOW else SiteDecision.DENY
    override fun close() = store.close()
    companion object {
        /** Called while the startup gate already owns an opened database on its worker. */
        fun preload(db: SQLiteDatabase) { NativeSiteStores.get(db).reload(db) }
        private val RELOAD_FEATURES = setOf(SiteFeature.USER_AGENT, SiteFeature.DESKTOP, SiteFeature.IMAGES, SiteFeature.JAVASCRIPT, SiteFeature.VIBRATION, SiteFeature.ADBLOCK)
    }
}
