package moe.via.browser.browser.session

import java.util.concurrent.atomic.AtomicInteger

/** Native o4.c allocates process-local integer handles separately from persistent UUIDs. */
internal object TabRuntimeIds {
    private val next = AtomicInteger(1)
    fun allocate(): String = next.getAndIncrement().toString()
    fun preserve(id: String): String {
        val value = id.toIntOrNull()?.takeIf { it > 0 } ?: return allocate()
        // 接收已有编号时推进分配计数器，防止新标签页分配到重复编号。
        next.updateAndGet { current -> maxOf(current, value + 1) }
        return id
    }
}
