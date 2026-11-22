package moe.via.browser.browser.session


data class TabSnapshot(
    val id: String = "",
    val url: String = "about:home",
    val title: String = "主页",
    val home: Boolean = true,
    val desktop: Boolean = false,
    val scrollX: Int = 0,
    val scrollY: Int = 0,
    val state: ByteArray? = null,
    val persistence: TabPersistenceId = TabPersistenceId(),
    val lastVisitedAt: Long = 0,
    /** Window-only configured source; never serialized into Native LIST/CUR or SQLite columns. */
    val literalStartUrl: String? = null
)
data class SavedSession(val tabs: List<TabSnapshot>, val selectedId: String?, val selectedPersistentId: String? = null)
data class RetainedSession(val session: SavedSession, val privateHistoryIds: Set<String>, val runtime: Map<String, TabRuntimeState> = emptyMap())
data class ClosedTab(val id: String, val tab: TabSnapshot, val closedAt: Long)

data class ClosedTabBatch(val tabs: List<TabSnapshot>, val selectedId: String?, val closedCount: Int)
