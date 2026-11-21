package moe.pure.browser.data

import android.os.Process
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/** Serialized persistence survives Activity replacement and preserves write/read ordering. */
object BrowserExecutors {
    private val ioIds = AtomicLong()
    /** Native g7.a.c cached IO policy; independent from the ordered state writer below. */
    val io by lazy {
        val ioPriority = (Integer.getInteger("rx3.io-priority", Thread.NORM_PRIORITY) ?: Thread.NORM_PRIORITY).coerceIn(1, 10)
        Executors.newCachedThreadPool { command ->
            Thread(command, "browser-io-${ioIds.incrementAndGet()}").apply {
                priority = ioPriority; isDaemon = true
            }
        }
    }
    val network = Executors.newFixedThreadPool(2) { command ->
        Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); command.run() }, "browser-network")
    }
    val persistence = Executors.newSingleThreadExecutor { command ->
        Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND); command.run() }, "browser-persistence")
    }
}
