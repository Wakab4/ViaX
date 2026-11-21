package moe.pure.browser.feature.engine

import android.app.Activity
import android.os.Bundle
import android.view.View

/** Gate before any browser WebView is created, including a custom tab's page. */
class EngineAvailabilityController(private val activity: Activity, private val host: Host) {
    interface Host {
        fun showContent(view: View)
        /** Native code 0 explicitly restarts its process; the host owns restart and session boundaries. */
        fun retryBrowser()
        fun exit()
    }
    private var page: EngineFailurePage? = null
    private var code: Int? = null
    private var disposed = false
    val isShowing get() = code != null && !disposed

    fun showIfUnavailable(savedState: Bundle? = null): Boolean {
        if (disposed) return false
        val restored = savedState?.getInt(STATE_CODE, -1)?.takeIf { it == 0 || it == 1 }
        if (restored != null) { show(restored); return true }
        code?.let { show(it); return true }
        val availability = EngineAvailabilityProbe.current()
        if (availability.available) return false
        show(if (availability.status == EngineProviderStatus.MISSING) 1 else 0)
        return true
    }

    /** For an actual provider preparation/construction failure caught by the browser owner. */
    fun showInitializationFailure() { if (!disposed) show(0) }

    fun save(outState: Bundle) { code?.let { outState.putInt(STATE_CODE, it) } }
    fun save(): Bundle = Bundle().also(::save)

    fun back(): Boolean {
        if (!isShowing) return false
        host.exit(); return true
    }

    fun pause() { page?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS }
    fun resume() { page?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO }

    fun dispose() {
        disposed = true
        page?.setOnClickListener(null)
        page = null; code = null
    }

    private fun show(value: Int) {
        code = value
        val body = page ?: EngineFailurePage(activity, ::primaryAction, host::exit).also { page = it }
        body.render(value)
        host.showContent(body)
    }

    private fun primaryAction() {
        if (disposed) return
        if (code == 1) {
            // No automatic resume/retry: Native keeps the missing-provider page until a restart.
            runCatching { EngineInstallAction.open(activity) }.onFailure { show(0) }
        } else {
            code = null; page = null
            host.retryBrowser()
        }
    }

    companion object { private const val STATE_CODE = "pure.engine.code" }
}
