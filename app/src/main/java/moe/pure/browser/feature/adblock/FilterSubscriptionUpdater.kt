package moe.pure.browser.feature.adblock

import android.content.Context
import android.os.Handler
import android.os.Looper
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.adblock.update.NativeFilterUpdateBatch
import moe.pure.browser.feature.adblock.update.NativeFilterUpdatePreferences
import moe.pure.browser.feature.adblock.update.NativeFilterUpdateWorker

class FilterSubscriptionUpdater private constructor(context: Context) {
    private val app = context.applicationContext
    private val repository = FilterSubscriptions(app)
    private val automatic = NativeFilterUpdateBatch(app, repository, NativeFilterUpdatePreferences(app))
    private val main = Handler(Looper.getMainLooper())
    fun update(item: FilterSubscription, completed: (Result<Boolean>) -> Unit) {
        BrowserExecutors.io.execute {
            val result = try { Result.success(NativeFilterUpdateWorker.update(item)) } catch (error: Exception) { Result.failure(error) }
            main.post { completed(result) }
        }
    }
    fun maybeUpdate() = automatic.start()
    companion object {
        @Volatile private var instance: FilterSubscriptionUpdater? = null
        fun get(context: Context) = instance ?: synchronized(this) { instance ?: FilterSubscriptionUpdater(context.applicationContext).also { instance = it } }
    }
}
