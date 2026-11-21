package moe.pure.browser.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import moe.pure.browser.R
import moe.pure.browser.ui.chrome.ChromeLayoutPolicy
import moe.pure.browser.ui.chrome.ChromeMode
import moe.pure.browser.ui.chrome.ChromeViewportLayout
import moe.pure.browser.ui.chrome.ChromeViewportState
import moe.pure.browser.ui.chrome.ChromeVisibilityController
import moe.pure.browser.ui.chrome.ChromeWindowInsets
import moe.pure.browser.ui.chrome.ChromeProgressState
import moe.pure.browser.ui.chrome.progress.NativePageProgress
import moe.pure.browser.ui.chrome.address.NativeAddressTextController
import moe.pure.browser.core.BrowserInternalPages
import moe.pure.browser.ui.theme.palette
import moe.pure.browser.feature.settings.data.ActionBinding
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.ui.components.SkinIconBinder
import moe.pure.browser.ui.components.dp

class BrowserChrome(private val activity: Activity, private val actions: Actions, private val customToolbar: View? = null,
    private val sharedContent: FrameLayout? = null) {
    val layoutSpec = ChromeLayoutPolicy.resolve(activity)
    interface Actions {
        fun onAddressEdit()
        fun onAddressMenu(anchor: View)
        fun onSearchEngine(anchor: View)
        fun onBack()
        fun onForward()
        fun onHome()
        fun onTabs()
        fun onMenu()
        fun onReload()
        fun onScan()
        fun onPageInfo()
        fun onLongPress(binding: ActionBinding): Boolean = false
        fun toolbarHost(): FrameLayout
        fun onInsetsChanged() {}
    }

    private val viewport = ChromeViewportLayout(activity)
    private val chromeMotion = ChromeVisibilityController(viewport) { actions.onInsetsChanged() }
    val root: View = viewport
    val content: FrameLayout
    val panelBackdrop = FrameLayout(activity)
    private val pageHost: FrameLayout
    /** The actual WebView parent; popovers use [content] and its bounded coordinates. */
    val pageFocusHost: ViewGroup get() = pageHost
    /** s6.X2 measures the favorite panel from the browser root, excluding its visible top bars. */
    fun favoritePanelHeight(topCss: Int): Int = viewport.height - activity.dp(topCss) -
        (if (viewport.topBars.visibility == View.VISIBLE) viewport.topBars.height else 0) + activity.dp(54) + activity.dp(4)
    private val searchToolbarHost: FrameLayout
    private val passwordToolbarHost: FrameLayout
    private var hasSearchToolbar = false
    private var hasPasswordToolbar = false
    private val pageTitle: TextView
    private val addressText: NativeAddressTextController
    private val topLeft: ImageView
    private val readerPulse by lazy { moe.pure.browser.ui.chrome.address.ReaderHintAnimation(topLeft) }
    private val topRight: ImageView
    private val addressBar: LinearLayout
    private val progress: ProgressBar
    private val loadingProgress: NativePageProgress
    private val backButton: View
    private val forwardButton: View
    private val tabsCount: TextView
    private var home = true
    private var currentUrl = ""
    private var readerHint = false
    private var gameMode = false
    private var chromeVisible = true
    val isChromeVisible get() = chromeMotion.isVisible
    val nativeControlsVisible get() = chromeVisible || revealButton?.visibility == View.VISIBLE
    var keyboardVisible = false
        private set
    private var tabStrip: View? = null
    private var gestureIndicator: View? = null
    private var revealButton: View? = null
    private var externalPanel = false
    private var panelBlur = false
    private var appearanceBackground: android.graphics.drawable.Drawable? = null
    private var blurredBackdrop: View? = null
    val hasContentOverlay: Boolean get() = externalPanel || (0 until content.childCount).any { index ->
        val child = content.getChildAt(index)
        child !== pageHost && child !== searchToolbarHost && child !== passwordToolbarHost &&
            child !== progress && child !== gestureIndicator && child !== revealButton && child.visibility == View.VISIBLE
    }
    private val preferences = BrowserPreferences(activity.getSharedPreferences("browser", Activity.MODE_PRIVATE))
    val tabStripAllowed: Boolean get() = customToolbar == null && when (preferences.toolbarLayout) {
        1, 2 -> true
        -1 -> layoutSpec.tablet
        else -> false
    }
    private val icons = SkinIconBinder(activity)
    private var progressLoading = false
    private var disposed = false
    private val backgroundSurfaces = mutableListOf<View>()
    private val foregroundSurfaces = mutableListOf<View>()

    init {
        // This temporary column only assembles the selected mode's controls; viewport owns geometry.
        val layout = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        viewport.setBackgroundColor(activity.palette.canvas)
        ViewCompat.setOnApplyWindowInsetsListener(viewport) { _, insets ->
            val result = ChromeWindowInsets.apply(viewport, insets, ChromeWindowInsets.State(
                gameMode, preferences.fullscreen, layoutSpec.topAnchored, tabStrip != null,
                if (tabStrip != null) viewport.floating else null
            ))
            keyboardVisible = result.keyboardVisible
            chromeMotion.keyboard(keyboardVisible)
            actions.onInsetsChanged()
            result.childInsets
        }

        val top = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 0)
            setBackgroundColor(activity.palette.canvas)
        }
        addressBar = top
        topLeft = ImageView(activity).apply {
            icons.image(this, R.drawable.ref_ic_search)
            imageTintList = android.content.res.ColorStateList.valueOf(activity.palette.icon)
            contentDescription = "搜索"
            setPadding(dp(13), 0, dp(13), 0)
            setOnClickListener { actions.onPageInfo() }
            setOnLongClickListener { actions.onSearchEngine(this); true }
        }
        top.addView(topLeft, LinearLayout.LayoutParams(dp(48), dp(48)))
        pageTitle = NativeTypography.text(activity).apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.page_info_title_size).toFloat())
            setTextColor(activity.palette.text)
            gravity = Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_LOCALE
            setSingleLine(true)
            ellipsize = null
            setFadingEdgeLength(dp(24))
            isHorizontalFadingEdgeEnabled = true
            setOnClickListener { startAddressEdit() }
            setOnLongClickListener { actions.onAddressMenu(this); true }
        }
        addressText = NativeAddressTextController(activity) { pageTitle.text = it }
        top.addView(pageTitle, LinearLayout.LayoutParams(0, dp(48), 1f))
        topRight = ImageView(activity).apply {
            icons.image(this, R.drawable.ref_ic_reload)
            imageTintList = android.content.res.ColorStateList.valueOf(activity.palette.icon)
            contentDescription = "刷新"
            setPadding(dp(13), 0, dp(13), 0)
            setOnClickListener {
                when {
                    home -> actions.onScan()
                    else -> actions.onReload()
                }
            }
        }
        top.addView(topRight, LinearLayout.LayoutParams(dp(48), dp(48)))
        layout.addView(top, LinearLayout.LayoutParams(-1, dp(48)))

        progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal)
        loadingProgress = NativePageProgress(progress)
        content = sharedContent ?: FrameLayout(activity)
        (content.parent as? ViewGroup)?.removeView(content)
        content.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) = actions.onInsetsChanged()
            override fun onChildViewRemoved(parent: View, child: View) = actions.onInsetsChanged()
        })
        pageHost = FrameLayout(activity)
        content.addView(pageHost, FrameLayout.LayoutParams(-1, -1))
        searchToolbarHost = FrameLayout(activity).apply { visibility = View.GONE; setBackgroundColor(activity.palette.canvas) }
        content.addView(searchToolbarHost, FrameLayout.LayoutParams(-1, dp(40), Gravity.BOTTOM))
        passwordToolbarHost = FrameLayout(activity).apply { visibility = View.GONE; setBackgroundColor(activity.palette.canvas) }
        content.addView(passwordToolbarHost, FrameLayout.LayoutParams(-1, dp(40), Gravity.BOTTOM))
        // h6.a.n(DIP,3) truncates applyDimension; the document owner's 2dp uses rounded y.h().
        content.addView(progress, FrameLayout.LayoutParams(-1, (3f * activity.resources.displayMetrics.density).toInt(), Gravity.TOP))
        layout.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottomHolder = FrameLayout(activity).apply { setBackgroundColor(activity.palette.canvas) }
        val bottom = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.TRANSPARENT)
        }
        backgroundSurfaces += top
        backgroundSurfaces += bottomHolder
        foregroundSurfaces += top
        foregroundSurfaces += bottom
        backButton = bottom.icon(R.drawable.ref_ic_back, "网页后退") { actions.onBack() }
        backButton.setOnLongClickListener { actions.onLongPress(ActionBinding.LONG_PRESS_BACK) }
        forwardButton = bottom.icon(R.drawable.ref_ic_forward, "网页前进") { actions.onForward() }
        forwardButton.setOnLongClickListener { actions.onLongPress(ActionBinding.LONG_PRESS_FORWARD) }
        bottom.icon(R.drawable.ref_ic_home, "主页") { actions.onHome() }
            .setOnLongClickListener { actions.onLongPress(ActionBinding.LONG_PRESS_HOME) }
        tabsCount = bottom.tabIcon()
        (tabsCount.parent as View).setOnLongClickListener { actions.onLongPress(ActionBinding.LONG_PRESS_TAB) }
        bottom.icon(R.drawable.ref_ic_menu, "菜单") { actions.onMenu() }
            .setOnLongClickListener { actions.onLongPress(ActionBinding.LONG_PRESS_MENU) }
        bottomHolder.addView(bottom, FrameLayout.LayoutParams(
            -1, dp(48), Gravity.CENTER))
        layout.addView(bottomHolder, LinearLayout.LayoutParams(-1, dp(48)))
        if (layoutSpec.combined) {
            backgroundSurfaces.clear()
            backgroundSurfaces += bottom
            bottom.setBackgroundColor(activity.palette.canvas)
            layout.removeView(top)
            bottomHolder.removeView(bottom)
            layout.removeView(bottomHolder)
            val addressHolder = FrameLayout(activity).apply { setPadding(dp(12), 0, 0, 0) }
            top.background = addressBackground()
            addressHolder.addView(top, FrameLayout.LayoutParams(-1, dp(48)))
            bottom.addView(addressHolder, 0, LinearLayout.LayoutParams(0, dp(48), 6f))
            bottom.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
                val width = ((right - left) / activity.resources.displayMetrics.density).toInt()
                val unit = when { width < 420 -> width / 7; width > 900 -> 72; else -> 60 }.coerceAtLeast(1)
                val slots = width / unit
                val showNavigation = slots > 8
                backButton.visibility = if (showNavigation) View.VISIBLE else View.GONE
                forwardButton.visibility = if (showNavigation) View.VISIBLE else View.GONE
                val weight = (slots - 3 - if (showNavigation) 2 else 0).coerceAtLeast(1).toFloat()
                val params = addressHolder.layoutParams as LinearLayout.LayoutParams
                if (params.weight != weight) { params.weight = weight; addressHolder.layoutParams = params }
            }
            layout.addView(bottom, if (layoutSpec.topAnchored) 0 else layout.childCount,
                LinearLayout.LayoutParams(-1, dp(48)))
        } else if (layoutSpec.mode == ChromeMode.DOUBLE_BOTTOM) {
            layout.removeView(top)
            layout.addView(top, layout.indexOfChild(bottomHolder), LinearLayout.LayoutParams(-1, dp(48)))
        }
        // The complete toolbar is the drag host's one child, including the combined address layout.
        val bars = if (layoutSpec.combined) listOf(bottom) else listOf(top, bottomHolder)
        bars.forEach { bar ->
            val parent = bar.parent as ViewGroup
            val index = parent.indexOfChild(bar)
            val params = bar.layoutParams
            parent.removeView(bar)
            val wrapper = actions.toolbarHost().apply { addView(bar, FrameLayout.LayoutParams(-1, -1)) }
            parent.addView(wrapper, index, params)
        }
        if (customToolbar != null) {
            (content.parent as? ViewGroup)?.removeView(content)
            (customToolbar.parent as? ViewGroup)?.removeView(customToolbar)
            layout.removeAllViews()
            layout.addView(customToolbar, LinearLayout.LayoutParams(-1, dp(48)))
            layout.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
            foregroundSurfaces.clear(); foregroundSurfaces += customToolbar
            backgroundSurfaces.clear(); backgroundSurfaces += customToolbar
        }
        val contentIndex = layout.indexOfChild(content)
        val above = (0 until contentIndex).map(layout::getChildAt)
        val below = (contentIndex + 1 until layout.childCount).map(layout::getChildAt)
        above.forEach { viewport.addToolbar(it, true) }
        below.forEach { viewport.addToolbar(it, false) }
        layout.removeView(content)
        viewport.install(pageHost, content, progress, searchToolbarHost, passwordToolbarHost, panelBackdrop)
        refreshViewportPolicy()
    }

    /** Page chrome metadata is separate from the Native URL/title callback sequence. */
    fun setPage(isHome: Boolean, url: String?) {
        home = isHome
        currentUrl = if (isHome) "" else url.orEmpty()
        updatePageIcon()
        topRight.visibility = View.VISIBLE
        updateLoadingAction()
    }

    fun addressUrlChanged(rawUrl: String?, mode: Int, effectiveTabBar: Boolean) = addressText.urlChanged(rawUrl, mode, effectiveTabBar)
    fun addressTitleChanged(title: String?, rawUrl: String?, mode: Int, effectiveTabBar: Boolean) = addressText.titleChanged(title, rawUrl, mode, effectiveTabBar)
    fun addressSelected(title: String?, rawUrl: String?, mode: Int, effectiveTabBar: Boolean) = addressText.selected(title, rawUrl, mode, effectiveTabBar)
    fun captureAddressText(): String = addressText.capture()
    fun restoreAddressText(value: String) = addressText.restore(value)

    fun setReaderAvailable(value: Boolean, animate: Boolean = false) {
        readerHint = value
        updatePageIcon()
        if (!value) readerPulse.cancel() else if (animate) readerPulse.start()
    }

    private fun updatePageIcon() {
        icons.image(topLeft, when {
            readerHint && !home -> R.drawable.ref_ic_reader
            home -> R.drawable.ref_ic_search
            currentUrl.startsWith("https://") -> R.drawable.ref_ic_lock
            else -> R.drawable.ref_ic_search
        })
        topLeft.contentDescription = if (readerHint && !home) "打开阅读模式" else "网站信息"
    }

    fun showPage(view: View) {
        if (view.parent === pageHost) return
        pageHost.removeAllViews()
        (view.parent as? ViewGroup)?.removeView(view)
        pageHost.addView(view, FrameLayout.LayoutParams(-1, -1))
    }

    fun setAppearance(background: android.graphics.drawable.Drawable?, foreground: Int?) {
        appearanceBackground = background ?: ColorDrawable(activity.palette.canvas)
        paintBackdrop()
        // The WebView may extend beneath the bars. Only an actual home image exposes the root;
        // a page's sampled ColorDrawable must also paint each visible toolbar surface.
        val surface = when (background) {
            null -> activity.palette.canvas
            is ColorDrawable -> background.color
            else -> Color.TRANSPARENT
        }
        backgroundSurfaces.forEach { it.setBackgroundColor(surface) }
        fun color(view: View) {
            when (view) {
                is ImageView -> view.imageTintList = android.content.res.ColorStateList.valueOf(foreground ?: activity.palette.icon)
                is TextView -> view.setTextColor(when (foreground) { null -> activity.palette.text; 0xFF232323.toInt() -> 0xDE000000.toInt(); else -> foreground })
                is ViewGroup -> for (index in 0 until view.childCount) color(view.getChildAt(index))
            }
        }
        foregroundSurfaces.forEach(::color)
    }

    fun externalPanel(edge: Int?, active: Boolean = edge != null) {
        externalPanel = active; viewport.externalOverlay(edge); actions.onInsetsChanged()
    }
    /** Native z8.l blurs the page/search strip and the independently drawn window background. */
    fun panelBlur(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < 31) return
        if (panelBlur == enabled && (!enabled || blurredBackdrop != null)) return
        if (appearanceBackground == null) appearanceBackground = root.background
        panelBlur = enabled
        val effect = if (enabled) android.graphics.RenderEffect.createBlurEffect(30f, 30f, android.graphics.Shader.TileMode.MIRROR) else null
        pageHost.setRenderEffect(effect); searchToolbarHost.setRenderEffect(effect)
        if (enabled && blurredBackdrop == null) blurredBackdrop = View(activity).apply {
            layoutParams = ViewGroup.LayoutParams(-1, -1); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }.also(viewport::backdrop)
        blurredBackdrop?.setRenderEffect(effect); paintBackdrop()
    }
    private fun paintBackdrop() {
        val desired = appearanceBackground ?: ColorDrawable(activity.palette.canvas)
        if (panelBlur) {
            root.background = null
            blurredBackdrop?.apply {
                background = desired.constantState?.newDrawable(activity.resources)?.mutate() ?: ColorDrawable(activity.palette.canvas)
                visibility = View.VISIBLE
            }
        } else {
            root.background = desired
            blurredBackdrop?.apply { background = null; visibility = View.GONE }
        }
    }

    /** A color change keeps the page host, active progress animation and toolbar gesture hosts. */
    fun refreshTheme() {
        setAppearance(null, null)
        if (layoutSpec.combined) addressBar.background = addressBackground()
        searchToolbarHost.setBackgroundColor(activity.palette.canvas)
        passwordToolbarHost.setBackgroundColor(activity.palette.canvas)
        icons.refresh()
    }

    private fun addressBackground() = LayerDrawable(arrayOf(GradientDrawable().apply {
        setColor(if (activity.palette.night) 0x22FFFFFF else 0x10000000)
        cornerRadius = dp(24).toFloat()
    })).apply { setLayerInset(0, dp(4), dp(4), dp(4), dp(4)) }

    fun updateNavigation(canBack: Boolean, canForward: Boolean, count: Int) {
        backButton.alpha = 1f
        forwardButton.alpha = 1f
        tabsCount.text = if (count > 99) ":)" else count.toString()
        tabsCount.tag = tabsCount.text
        (tabsCount.parent as View).contentDescription = "$count 个打开的标签"
    }

    fun updateProgress(value: Int, isHome: Boolean) {
        val internalPage = isHome || BrowserInternalPages.isInternal(activity, currentUrl)
        val displayed = (if (internalPage) 100 else value.coerceIn(0, 100)) + 20
        loadingProgress.update(displayed)
        progressLoading = displayed < 100
        updateLoadingAction()
    }

    private fun updateLoadingAction() {
        icons.image(topRight, when {
            home -> R.drawable.ref_ic_scan
            progressLoading -> R.drawable.ref_ic_close
            else -> R.drawable.ref_ic_reload
        })
        topRight.contentDescription = when { home -> "扫描二维码"; progressLoading -> "停止加载"; else -> "刷新" }
    }

    fun captureProgressState(): ChromeProgressState = ChromeProgressState(loadingProgress.capture(), progressLoading)

    fun restoreProgressState(state: ChromeProgressState) {
        progressLoading = state.loading
        loadingProgress.restore(state.animation)
        updateLoadingAction()
    }

    fun startAddressEdit() = actions.onAddressEdit()

    fun setTabStrip(view: View?) {
        if (tabStrip === view) return
        (tabStrip?.parent as? ViewGroup)?.removeView(tabStrip)
        tabStrip = view?.takeIf { tabStripAllowed }
        tabStrip?.let { strip ->
            (strip.parent as? ViewGroup)?.removeView(strip)
            strip.visibility = View.VISIBLE
            viewport.tabStrip(strip, layoutSpec.topAnchored, dp(36))
        }
        viewport.requestLayout()
    }

    fun setToolbarPreview(direction: Int) {
        tabsCount.text = when { direction < 0 -> "‹"; direction > 0 -> "›"; else -> tabsCount.tag?.toString() ?: "1" }
    }

    fun attachGestureIndicator(view: View) {
        viewport.decoration(view, gestureIndicator)
        (gestureIndicator?.parent as? ViewGroup)?.removeView(gestureIndicator)
        (view.parent as? ViewGroup)?.removeView(view)
        gestureIndicator = view
        viewport.floating.addView(view)
    }
    fun attachRevealButton(view: View) {
        viewport.decoration(view, revealButton)
        (revealButton?.parent as? ViewGroup)?.removeView(revealButton)
        (view.parent as? ViewGroup)?.removeView(view)
        revealButton = view
        viewport.floating.addView(view)
    }

    fun setChromeVisible(visible: Boolean, animated: Boolean = true) {
        if (gameMode || keyboardVisible) return
        chromeVisible = visible || customToolbar != null || !expandedByPreference()
        chromeMotion.show(chromeVisible, animated)
    }

    fun refreshViewportPolicy() {
        chromeMotion.policy(expandedByPreference())
        if (!expandedByPreference() && !gameMode) chromeVisible = true
    }

    fun captureViewportState(): ChromeViewportState = chromeMotion.capture(isChromeVisible)

    fun restoreViewportState(state: ChromeViewportState) {
        chromeVisible = state.visible || customToolbar != null || !expandedByPreference()
        chromeMotion.restore(state.copy(visible = chromeVisible))
    }

    private fun expandedByPreference() = customToolbar == null && (preferences.fullscreen || preferences.autoHide > 0)

    fun setSearchToolbar(view: View?) {
        searchToolbarHost.removeAllViews()
        hasSearchToolbar = view != null
        if (view != null) {
            (view.parent as? ViewGroup)?.removeView(view)
            searchToolbarHost.addView(view, FrameLayout.LayoutParams(-1, -1))
        }
        updateSearchToolbarVisibility()
    }

    private fun updateSearchToolbarVisibility() {
        val visible = hasSearchToolbar && !gameMode
        searchToolbarHost.visibility = if (visible) View.VISIBLE else View.GONE
        val passwordsVisible = hasPasswordToolbar
        passwordToolbarHost.visibility = if (passwordsVisible) View.VISIBLE else View.GONE
        // Native C0 overlays B0 at the same bottom anchor; only the search strip reserves page space.
        pageHost.setPadding(0, 0, 0, if (visible) dp(40) else 0)
        viewport.requestLayout()
    }

    fun setPasswordToolbar(view: View?) {
        passwordToolbarHost.removeAllViews()
        hasPasswordToolbar = view != null
        if (view != null) {
            (view.parent as? ViewGroup)?.removeView(view)
            passwordToolbarHost.addView(view, FrameLayout.LayoutParams(-1, -1))
        }
        updateSearchToolbarVisibility()
    }

    fun setGameMode(enabled: Boolean) {
        if (customToolbar != null) return
        if (gameMode == enabled) { refreshViewportPolicy(); root.requestApplyInsets(); return }
        gameMode = enabled
        if (!enabled) chromeVisible = true
        updateSearchToolbarVisibility()
        chromeMotion.game(enabled)
        refreshViewportPolicy()
        root.requestApplyInsets()
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        readerPulse.cancel()
        panelBlur(false)
        icons.close()
        chromeMotion.dispose()
        loadingProgress.dispose()
        pageHost.removeAllViews(); panelBackdrop.removeAllViews()
        if (sharedContent == null) content.removeAllViews(); viewport.floating.removeAllViews()
    }
    fun refreshIcons() = icons.refresh()
    fun refreshTypography() = NativeTypography.tree(addressBar)
    fun setPageMask(alpha: Int) { pageHost.foreground = android.graphics.drawable.ColorDrawable(Color.argb(alpha.coerceIn(0, 255), 0, 0, 0)) }

    private fun LinearLayout.icon(drawable: Int, label: String, action: () -> Unit): View {
        val button = FrameLayout(activity).apply {
            isClickable = true
            isFocusable = true
            contentDescription = label
            setOnClickListener { action() }
        }
        button.addView(ImageView(activity).apply {
            icons.image(this, drawable)
            imageTintList = android.content.res.ColorStateList.valueOf(activity.palette.icon)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        addView(button, LinearLayout.LayoutParams(0, dp(48), 1f))
        return button
    }

    private fun LinearLayout.tabIcon(): TextView {
        val button = FrameLayout(activity).apply {
            isClickable = true
            isFocusable = true
            contentDescription = "标签页"
            setOnClickListener { actions.onTabs() }
        }
        button.addView(ImageView(activity).apply {
            icons.image(this, R.drawable.ref_ic_tabs)
            imageTintList = android.content.res.ColorStateList.valueOf(activity.palette.icon)
        },
            FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        val count = NativeTypography.text(activity).apply {
            text = "1"
            textSize = 11f
            NativeTypography.style(this, android.graphics.Typeface.BOLD)
            setTextColor(activity.palette.text)
            gravity = Gravity.CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        button.addView(count, FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        addView(button, LinearLayout.LayoutParams(0, dp(48), 1f))
        return count
    }

    private fun dp(value: Int) = (value * activity.resources.displayMetrics.density + 0.5f).toInt()
}
