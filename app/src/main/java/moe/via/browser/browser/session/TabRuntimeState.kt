package moe.via.browser.browser.session

import java.util.concurrent.ConcurrentHashMap

/** Feature-owned RAM state, with no Views or Activity references and no database serialization. */
class TabRuntimeState {
    class Key<T : Any>
    private val values = ConcurrentHashMap<Key<*>, Any>()
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: Key<T>, create: () -> T): T = values.getOrPut(key, create) as T
}
