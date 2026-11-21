package moe.pure.browser.browser.closed

/** ua's window-local UUID and generation are separate from an undo notice's captured command. */
internal class RecentClosedState {
    private var generation = 0
    private var latest: String? = null
    val available: Boolean get() = !latest.isNullOrEmpty()

    fun beginSingleClose(): Int = ++generation
    fun completed(generation: Int, persistentId: String?) {
        if (this.generation == generation) latest = persistentId
    }
    fun invalidate() { latest = null; generation++ }
    fun take(): String? {
        val value = latest?.takeIf(String::isNotEmpty) ?: return null
        invalidate()
        return value
    }
}
