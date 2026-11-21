package moe.pure.browser.feature.firstrun

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import moe.pure.browser.R
import moe.pure.browser.feature.firstrun.ui.FirstRunDocumentView
import moe.pure.browser.feature.firstrun.ui.FirstRunWelcomeView
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity

/** Owns the welcome/document stack before the host initializes any browser WebView or tab. */
class FirstRunController(
    private val activity: Activity,
    private val preferences: SharedPreferences,
    private val host: Host
) {
    interface Host {
        fun showContent(view: View)
        fun beginBrowser()
        fun exit()
    }

    private enum class Stage { BROWSER, WELCOME, DOCUMENT }
    private var stage = Stage.BROWSER
    private var welcome: FirstRunWelcomeView? = null
    private var document: FirstRunDocumentView? = null
    private var documentKind: FirstRunDocument? = null
    private var documentUrl: String? = null
    private var refusal: BrowserDialog? = null
    private var disposed = false
    private var began = false
    private var resumed = false
    val active get() = !disposed && stage != Stage.BROWSER

    fun showIfNeeded(customTabMode: Boolean, savedState: Bundle?): Boolean {
        if (disposed || customTabMode || savedState?.getBoolean("browser_started", false) == true) return false
        val restored = savedState?.getBundle(STATE)
        val restoredStage = restored?.getString("stage")
        if (restoredStage == Stage.BROWSER.name) return false
        val restoringWelcome = restoredStage == Stage.WELCOME.name || restoredStage == Stage.DOCUMENT.name
        if (!restoringWelcome && (preferences.all[AGREEMENT] as? Number)?.toInt()?.let { it >= 1 } == true) return false
        showWelcome()
        if (restoredStage == Stage.DOCUMENT.name) {
            val kind = FirstRunDocument.entries.firstOrNull { it.name == restored?.getString("document") }
            if (kind != null) {
                val url = restored?.getString("url")?.takeIf(kind::accepts) ?: kind.url(preferences)
                showDocument(kind, url)
            }
        }
        return true
    }

    fun save(outState: Bundle) {
        if (disposed) return
        outState.putBundle(STATE, Bundle().apply {
            putString("stage", stage.name)
            if (stage == Stage.DOCUMENT) {
                putString("document", documentKind?.name)
                putString("url", documentUrl)
            }
        })
    }

    fun back(): Boolean {
        if (!active) return false
        if (refusal?.isShowing == true) refusal?.dismiss()
        else if (stage == Stage.DOCUMENT) document?.back() ?: showWelcome()
        else host.exit()
        return true
    }

    fun resume() { resumed = true; document?.resume() }
    fun pause() { resumed = false; document?.pause() }

    fun dispose() {
        if (disposed) return
        disposed = true
        refusal?.dismiss(); refusal = null
        document?.dispose(); document = null
        welcome = null
        stage = Stage.BROWSER
    }

    private fun showWelcome() {
        if (disposed || began) return
        document?.dispose(); document = null
        documentKind = null; documentUrl = null
        stage = Stage.WELCOME
        val urls = FirstRunDocument.entries.associateWith { it.url(preferences) }
        val view = welcome ?: FirstRunWelcomeView(activity,
            openDocument = { kind -> if (stage == Stage.WELCOME && active) showDocument(kind, urls.getValue(kind)) },
            agree = ::accept,
            disagree = ::showRefusal
        ).also { welcome = it }
        host.showContent(view.root)
        view.root.requestApplyInsets()
    }

    private fun showDocument(kind: FirstRunDocument, url: String) {
        if (disposed || began || !kind.accepts(url)) return
        document?.dispose()
        stage = Stage.DOCUMENT
        documentKind = kind
        documentUrl = url
        val view = FirstRunDocumentView(activity, activity.getString(kind.title), url, ::showWelcome)
        document = view
        host.showContent(view.root)
        view.root.requestApplyInsets()
        view.start()
        if (resumed) view.resume() else view.pause()
    }

    private fun accept() {
        if (disposed || began || stage != Stage.WELCOME) return
        began = true
        welcome?.accepting()
        preferences.edit().putInt(AGREEMENT, 1).apply()
        refusal?.dismiss(); refusal = null
        document?.dispose(); document = null
        stage = Stage.BROWSER
        welcome = null
        host.beginBrowser()
    }

    private fun showRefusal() {
        if (disposed || stage != Stage.WELCOME || refusal?.isShowing == true) return
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_first_run_hint), activity.getString(R.string.ref_first_run_refusal))
            .positive(activity.getString(android.R.string.ok))
            .neutral(activity.getString(R.string.ref_first_run_exit)) { host.exit(); true }
        refusal = dialog
        if (activity is ThemedActivity) activity.dialogScope.show(dialog) { if (refusal === dialog) refusal = null }
        else {
            dialog.setOnDismissListener { if (refusal === dialog) refusal = null }
            dialog.show()
        }
    }

    companion object {
        private const val STATE = "pure.first_run"
        private const val AGREEMENT = "agreement2"
    }
}
