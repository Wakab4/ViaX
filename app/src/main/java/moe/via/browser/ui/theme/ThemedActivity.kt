package moe.via.browser.ui.theme

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.AssetManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import moe.via.browser.R
import moe.via.browser.core.localization.SettingsContext
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.components.insets.NativePageInsets
import moe.via.browser.ui.components.insets.NativeSystemBarAppearance
import moe.via.browser.ui.pages.PageStackController
import moe.via.browser.ui.pages.PageBackDispatcher
import moe.via.browser.ui.pages.PredictiveBackPreview
import java.lang.ref.WeakReference
import moe.via.browser.storage.preferences.BrowserNightMode
import moe.via.browser.storage.preferences.BrowserThemeOwner

abstract class ThemedActivity : FragmentActivity(), BrowserThemeOwner {
    override val browserNightOverride: Boolean? get() = null
    val dialogScope = DialogScope()
    private var attachedLanguage = ""
    private var recreatingForSettings = false
    protected val settingsRecreationPending get() = recreatingForSettings
    private var attachedNight = false
    private var windowResources: Resources? = null
    private var pages: PageStackController? = null
    private var backDispatcher: PageBackDispatcher? = null
    private val nestedPages = mutableListOf<WeakReference<PageStackController>>()
    internal val pageStack get() = pages ?: error("Page stack is not attached")
    internal val hasRecordPage get() = pages?.top != null
    internal val currentRecordStack get() = pages?.takeIf { it.isPresented && it.top != null }
        ?: nestedPages.asReversed().mapNotNull { it.get() }.firstOrNull { it.isPresented && it.top != null } ?: pages
    internal val hasActiveRecordInput get() = currentRecordStack?.top != null
    internal fun registerRecordPageStack(stack: PageStackController): AutoCloseable {
        val reference = WeakReference(stack); nestedPages.add(reference)
        return AutoCloseable { nestedPages.remove(reference) }
    }
    override fun getResources(): Resources = windowResources ?: super.getResources()
    override fun getAssets(): AssetManager = windowResources?.assets ?: super.getAssets()
    private fun updateWindowResources() {
        val configuration = Configuration(baseContext.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (BrowserNightMode.isNight(this@ThemedActivity)) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        windowResources = baseContext.createConfigurationContext(configuration).resources
    }
    override fun setTheme(resId: Int) {
        // The framework sets the manifest theme after attach and before onCreate.
        updateWindowResources()
        super.setTheme(resId)
    }
    protected open fun onSystemBack() { finish() }
    internal open fun beginBrowserPredictiveBack(): PredictiveBackPreview? = null
    protected open fun onRecordPageVisibilityChanged(showing: Boolean) { applySystemBarAppearance() }
    internal fun recordPageVisibilityChanged(showing: Boolean) = onRecordPageVisibilityChanged(showing)
    /** Return true only when the existing page has repainted its own views for this theme. */
    protected open fun onBrowserNightChanged(night: Boolean): Boolean = false
    protected open fun onSystemNightModeSynchronized() = Unit

    /** Native browser night changes keep the current WebViews and update their window in place. */
    protected fun applyBrowserNightTheme() {
        attachedNight = palette.night
        setTheme(if (attachedNight) R.style.Theme_ViaBrowser_Night else R.style.Theme_ViaBrowser)
        applySystemBarAppearance()
        pages?.themeChanged()
        nestedPages.mapNotNull { it.get() }.toList().forEach { it.themeChanged() }
    }
    override fun attachBaseContext(newBase: Context) {
        if (BrowserNightMode.synchronize(newBase)) onSystemNightModeSynchronized()
        val night = newBase.palette.night
        val localized = SettingsContext.wrap(newBase, night)
        attachedLanguage = SettingsContext.signature(localized)
        attachedNight = night
        SettingsContext.applyProcessLocale(localized)
        super.attachBaseContext(localized)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        updateWindowResources()
        attachedNight = palette.night
        val night = palette.night
        setTheme(if (night) R.style.Theme_ViaBrowser_Night else R.style.Theme_ViaBrowser)
        super.onCreate(savedInstanceState)
        BrowserMessages.bind(this)
        pages = PageStackController(this).also { it.attach() }
        if (Build.VERSION.SDK_INT >= 33) {
            backDispatcher = PageBackDispatcher(this, ::dispatchSystemBack).also { it.install() }
        }
        NativePageInsets.prepareWindow(window)
        applySystemBarAppearance()
    }

    private fun dispatchSystemBack() { if (currentRecordStack?.handleBack() != true) onSystemBack() }
    override fun setContentView(view: View) { super.setContentView(view); pages?.attach(view) }
    override fun setContentView(view: View, params: ViewGroup.LayoutParams) { super.setContentView(view, params); pages?.attach(view) }
    override fun setContentView(layoutResID: Int) { super.setContentView(layoutResID); pages?.attach() }

    override fun onResume() {
        super.onResume()
        if (BrowserNightMode.synchronize(this)) onSystemNightModeSynchronized()
        if (attachedLanguage != SettingsContext.signature(this)) {
            recreateForSettings()
        } else if (attachedNight != palette.night && !onBrowserNightChanged(palette.night)) {
            recreateForSettings()
        } else {
            SettingsContext.applyProcessLocale(this)
        }
    }

    internal fun recreateForSettings() {
        if (recreatingForSettings || isFinishing || isDestroyed) return
        recreatingForSettings = true
        window.decorView.post { if (!isFinishing && !isDestroyed) recreate() }
    }

    protected fun applySystemBarAppearance() {
        val stack = currentRecordStack
        if (stack?.isPresented == true && stack.top?.applyWindowAppearance() == true) return
        val colors = palette
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = colors.canvas
        NativeSystemBarAppearance.light(window, !colors.night)
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        backDispatcher?.cancel()
        super.onConfigurationChanged(newConfig)
        if (BrowserNightMode.synchronize(this)) onSystemNightModeSynchronized()
        updateWindowResources()
        if (attachedLanguage != SettingsContext.signature(this)) {
            recreateForSettings()
        } else if (attachedNight != palette.night && !onBrowserNightChanged(palette.night)) {
            recreateForSettings()
        }
        window.decorView.post { if (!isDestroyed) dialogScope.updateGeometry() }
    }
    // API 28–32 still dispatch this method; newer Android uses the registered platform callback.
    @SuppressLint("GestureBackNavigation")
    @Deprecated("Legacy dispatch for Android 12 and below")
    final override fun onBackPressed() = dispatchSystemBack()
    override fun onDestroy() {
        backDispatcher?.close(); backDispatcher = null
        pages?.close(); pages = null
        nestedPages.clear()
        BrowserMessages.destroyed(this)
        dialogScope.dispose()
        super.onDestroy()
    }
}
