package moe.via.browser.app.startup

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import kotlin.system.exitProcess
import moe.via.browser.app.lifecycle.NativeAppRestart
import moe.via.browser.browser.ui.engine.EngineAvailabilityController
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.firstrun.FirstRunController
import moe.via.browser.feature.search.data.remote.RemoteSearchConfig
import moe.via.browser.webview.compat.WebEngineStartupException

internal class BrowserStartup(
    private val activity: Activity,
    private val customTabMode: Boolean,
    preferences: SharedPreferences,
    showGate: (View) -> Unit,
    private val initialize: (Bundle?) -> Unit,
    private val databaseError: (Throwable) -> Unit,
) : AutoCloseable {
    var started = false
        private set
    var generation = 0L
        private set

    private var pendingState: Bundle? = null
    private val database = lazy { BrowserDatabaseStartup(activity.applicationContext) }
    private val remoteSearch = lazy { RemoteSearchConfig(activity.applicationContext) }
    private val firstRun = lazy {
        FirstRunController(activity, preferences, object : FirstRunController.Host {
            override fun showContent(view: View) = showGate(view)
            override fun beginBrowser() = start(pendingState)
            override fun exit() = activity.finish()
        })
    }
    private val engine = lazy {
        EngineAvailabilityController(activity, object : EngineAvailabilityController.Host {
            override fun showContent(view: View) = showGate(view)
            override fun retryBrowser() { NativeAppRestart.restart(activity) }
            override fun exit() { activity.finish(); exitProcess(0) }
        })
    }

    fun create(state: Bundle?) {
        pendingState = state
        if (state != null) startRemoteSearch()
        if (!firstRun.value.showIfNeeded(customTabMode, state)) start(state)
    }

    fun ready(expectedGeneration: Long = generation): Boolean =
        started && generation == expectedGeneration && !activity.isFinishing && !activity.isDestroyed

    private fun start(state: Bundle?) {
        if (started || activity.isFinishing || activity.isDestroyed) return
        startRemoteSearch()
        if (!database.value.ready) {
            database.value.prepare({ start(state) }) { error ->
                NativeLog.error("BrowserDatabase", "Cannot open browser database", error)
                databaseError(error)
                activity.finish()
            }
            return
        }
        if (engine.value.showIfUnavailable(state)) return
        started = true
        generation++
        initialize(state)
    }

    private fun startRemoteSearch() {
        if (!customTabMode) remoteSearch.value.start()
    }

    fun engineUnavailable(error: WebEngineStartupException, resetWindow: () -> Unit) {
        started = false
        generation++
        resetWindow()
        NativeLog.error("BrowserEngine", "Cannot construct WebView", error)
        engine.value.showInitializationFailure()
    }

    fun back() {
        if (!firstRun.isInitialized() || !firstRun.value.back()) activity.finish()
    }

    fun resume() {
        if (firstRun.isInitialized()) firstRun.value.resume()
        if (engine.isInitialized()) engine.value.resume()
    }

    fun pause() {
        if (firstRun.isInitialized()) firstRun.value.pause()
        if (engine.isInitialized()) engine.value.pause()
    }

    fun save(outState: Bundle) {
        outState.putBoolean("browser_started", started)
        if (firstRun.isInitialized()) firstRun.value.save(outState)
        if (engine.isInitialized()) engine.value.save(outState)
    }

    override fun close() {
        generation++
        if (firstRun.isInitialized()) firstRun.value.dispose()
        if (engine.isInitialized()) engine.value.dispose()
        if (remoteSearch.isInitialized()) remoteSearch.value.close()
        if (database.isInitialized()) database.value.dispose()
    }
}
