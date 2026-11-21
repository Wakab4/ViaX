package moe.pure.browser.feature.useragent.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.useragent.UserAgentRepository
import java.util.concurrent.FutureTask

/** Each worker owns its database helper; a replaced window never closes a running helper. */
internal object UserAgentOperations {
    private val main = Handler(Looper.getMainLooper())
    fun <T> run(context: Context, operation: (UserAgentRepository) -> T, completed: (Result<T>) -> Unit): FutureTask<Unit> {
        val app = context.applicationContext
        lateinit var task: FutureTask<Unit>
        task = FutureTask<Unit> {
            val result = runCatching { UserAgentRepository(app).use(operation) }
            if (!task.isCancelled) main.post { if (!task.isCancelled) completed(result) }
            Unit
        }
        BrowserExecutors.persistence.execute(task)
        return task
    }
}
