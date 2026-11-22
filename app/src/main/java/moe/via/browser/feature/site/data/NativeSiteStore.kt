package moe.via.browser.feature.site.data

import android.database.sqlite.SQLiteDatabase
import java.util.concurrent.ConcurrentHashMap

data class TemporarySiteConfig(val config: NativeSiteConfig, val expiresAt: Long = 0, val mode: Int = 0)

/** A process store per database, shared by feature facades and worker-owned backup codecs. */
internal object NativeSiteStores {
    private val stores = ConcurrentHashMap<String, NativeSiteStore>()
    fun get(db: SQLiteDatabase): NativeSiteStore = stores.getOrPut(db.path) { NativeSiteStore() }
}

internal class NativeSiteStore {
    private var loaded = false
    private var persisted = emptyMap<String, NativeSiteConfig>()
    private val temporary = HashMap<String, TemporarySiteConfig>()

    @Synchronized fun reload(db: SQLiteDatabase) { persisted = NativeSiteRows(db).read(); loaded = true }
    @Synchronized fun snapshot(db: SQLiteDatabase): Map<String, NativeSiteConfig> { ensure(db); return persisted.toMap() }

    @Synchronized fun get(db: SQLiteDatabase, key: String, persistedOnly: Boolean = false): NativeSiteConfig? {
        if (key.isEmpty()) return null
        ensure(db)
        val base = persisted[key]
        if (persistedOnly) return base
        val overlay = temporary[key] ?: return base
        if (overlay.expiresAt > 0 && System.currentTimeMillis() > overlay.expiresAt) { temporary.remove(key); return base }
        return when (overlay.mode) { 0 -> overlay.config; 1 -> merge(base, overlay.config); else -> base }
    }

    @Synchronized fun editBase(db: SQLiteDatabase, key: String): NativeSiteConfig {
        if (key.isEmpty()) return NativeSiteConfig()
        return get(db, key) ?: NativeSiteConfig()
    }

    @Synchronized fun put(db: SQLiteDatabase, key: String, config: NativeSiteConfig): Boolean {
        if (key.isEmpty() || config.isEmpty()) return false
        ensure(db)
        NativeSiteRows(db).transaction { NativeSiteRows(db).write(key, config) }
        persisted = persisted + (key to config)
        return true
    }

    @Synchronized fun delete(db: SQLiteDatabase, key: String): Boolean {
        if (key.isEmpty()) return false
        ensure(db)
        val existed = persisted.containsKey(key)
        NativeSiteRows(db).transaction { NativeSiteRows(db).delete(key) }
        persisted = persisted - key
        return existed
    }

    @Synchronized fun batch(db: SQLiteDatabase, entries: Map<String, NativeSiteConfig?>): Boolean {
        if (entries.isEmpty()) return false
        val rows = NativeSiteRows(db)
        rows.transaction { entries.forEach { (key, config) -> rows.write(key, config) } }
        reload(db)
        return true
    }

    @Synchronized fun rename(db: SQLiteDatabase, old: String, new: String): SiteRenameResult {
        if (old.isEmpty() || new.isEmpty()) return SiteRenameResult.INVALID
        if (old == new) return SiteRenameResult.UNCHANGED
        ensure(db)
        if (persisted.containsKey(new)) return SiteRenameResult.CONFLICT
        if (!persisted.containsKey(old)) return SiteRenameResult.MISSING
        val result = NativeSiteRows(db).rename(old, new)
        if (result == SiteRenameResult.RENAMED) reload(db)
        // ca.b.c does not move temporary overrides to the renamed persistent identity.
        return result
    }

    @Synchronized fun installTemporary(key: String, overlay: TemporarySiteConfig) {
        if (key.isNotEmpty() && !(overlay.expiresAt > 0 && System.currentTimeMillis() > overlay.expiresAt)) temporary[key] = overlay
    }
    @Synchronized fun removeTemporary(key: String) { temporary.remove(key) }
    private fun ensure(db: SQLiteDatabase) { if (!loaded) reload(db) }

    /** ba.c.a: only its listed fields merge; the GP incognito-to-desktop setter quirk is DEX-confirmed. */
    private fun merge(base: NativeSiteConfig?, overlay: NativeSiteConfig): NativeSiteConfig {
        if (base == null || !base.enabled) return overlay
        if (!overlay.enabled) return base
        var result: NativeSiteConfig = base
        if (base.userAgentChoice == -1000) result = result.copy(userAgentChoice = overlay.userAgentChoice,
            customUserAgent = overlay.customUserAgent)
        val features = listOf(moe.via.browser.feature.site.SiteFeature.JAVASCRIPT,
            moe.via.browser.feature.site.SiteFeature.IMAGES, moe.via.browser.feature.site.SiteFeature.DESKTOP,
            moe.via.browser.feature.site.SiteFeature.ADBLOCK)
        features.forEach { if (result.feature(it) == null && overlay.feature(it) != null) result = result.withFeature(it, overlay.feature(it)) }
        val incognito = moe.via.browser.feature.site.SiteFeature.INCOGNITO
        if (result.feature(incognito) == null && overlay.feature(incognito) != null)
            result = result.withFeature(moe.via.browser.feature.site.SiteFeature.DESKTOP, overlay.feature(incognito))
        val clipboard = moe.via.browser.feature.site.SiteFeature.CLIPBOARD
        if (result.feature(clipboard) == null && overlay.feature(clipboard) != null) result = result.withFeature(clipboard, overlay.feature(clipboard))
        return result.copy(textSize = if (base.textSize > 0) base.textSize else overlay.textSize.coerceAtLeast(0))
    }
}
