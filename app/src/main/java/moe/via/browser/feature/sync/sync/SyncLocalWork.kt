package moe.via.browser.feature.sync.sync

import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.sync.network.SyncOperation
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

/** Existing application persistence queue keeps imports ordered with local edits. */
internal fun <T> syncLocal(operation: SyncOperation, block: () -> T): T {
    operation.check()
    val task = FutureTask { operation.check(); block() }
    BrowserExecutors.persistence.execute(task)
    return try { task.get().also { operation.check() } }
    catch (error: InterruptedException) { task.cancel(false); Thread.currentThread().interrupt(); throw error }
    catch (error: ExecutionException) { throw error.cause ?: error }
}
