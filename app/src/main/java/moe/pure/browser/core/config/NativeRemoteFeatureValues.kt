package moe.pure.browser.core.config

/** Process feature configuration supplies fulltext availability and reader exclusions. */
internal object NativeRemoteFeatureValues {
    @Volatile private var document = RemoteFeatureDocument.EMPTY
    @Volatile var sites = NativeSiteExclusions(RemoteFeatureDocument.EMPTY)
        private set
    val fulltextAllowed: Boolean get() = document.getInt("fl1", 0) and 1 == 0
    fun activate(value: RemoteFeatureDocument) {
        document = value
        sites = NativeSiteExclusions(value)
    }
}
