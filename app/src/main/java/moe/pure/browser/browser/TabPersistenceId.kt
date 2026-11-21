package moe.pure.browser.browser

/** Runtime IDs stay on the UI thread; the serialized writer assigns the independent database UUID. */
class TabPersistenceId(initial: String? = null, private val inherited: TabPersistenceId? = null) {
    @Volatile private var assigned = initial
    fun get(): String? = assigned ?: inherited?.get()
    fun bind(value: String) { assigned = value }

    // Undo can run before the queued close allocates its UUID. The next worker operation
    // resolves that allocation, then binds independently so later deletion can create a new row.
    fun fork() = TabPersistenceId(inherited = this)
}
