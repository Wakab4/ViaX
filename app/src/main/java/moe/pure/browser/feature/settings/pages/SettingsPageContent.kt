package moe.pure.browser.feature.settings.pages

import android.os.SystemClock
import android.view.View
import moe.pure.browser.R
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.settings.SettingsRoute
import moe.pure.browser.feature.settings.data.ActionBinding
import moe.pure.browser.feature.settings.ui.*
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.ui.components.recycler.NativeRecyclerView
import moe.pure.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import androidx.recyclerview.widget.LinearLayoutManager

/** Standard settings content delegates values to page providers and navigation to its Fragment. */
internal class SettingsPageContent(private val activity: ThemedActivity, private val route: SettingsRoute,
    private val binding: ActionBinding?, applyInsets: Boolean, private val back: () -> Unit,
    private val navigate: (SettingsRoute, ActionBinding?) -> Unit, private val external: (SettingsCommand) -> Unit) : AutoCloseable {
    private val settings = BrowserSettings(activity)
    private val page = PageScaffold(activity, route.title, back, applyInsets)
    private val host: SettingsHost = SettingsHost(activity, settings, { navigate(it, null) }, ::render, ::command)
    private val adapter: SettingsRowAdapter = SettingsRowAdapter(activity, ::render)
    private val list = NativeRecyclerView(activity).apply {
        id = R.id.settings_page_list
        layoutManager = LinearLayoutManager(activity)
        clipToPadding = false
        NativeRecyclerEdgeEffect.install(this)
        this.adapter = this@SettingsPageContent.adapter
    }
    private val core by lazy { CoreSettingsPages(host) }
    private val general by lazy { GeneralSettingsPage(host) }
    private val toolbar by lazy { ToolbarSettingsPage(host) }
    private val operations by lazy { OperationSettingsPage(host) { navigate(SettingsRoute.ACTIONS, it) } }
    private val night by lazy { NightSettingsPage(host) }
    private val ssl by lazy { SslSettingsPage(host) }
    private val longPressMenu by lazy { LongPressMenuSettingsPage(host) }
    private var headerTaps = 0
    private var lastHeaderTap = 0L
    val view get() = page.root

    init {
        page.showContent(list, bottomInsets = list)
        if (route in setOf(SettingsRoute.GENERAL, SettingsRoute.PRIVACY, SettingsRoute.NIGHT, SettingsRoute.OPERATIONS)) {
            list.itemAnimator = null
        }
        NativeToolbarTap.bind(page.toolbar) { list.smoothScrollToPosition(0) }
        if (route == SettingsRoute.ROOT) page.titleView.setOnClickListener { headerTapped() }
        if (route == SettingsRoute.SSL) page.addTextAction("帮助", ssl::help)
        if (route == SettingsRoute.LONG_PRESS_MENU) page.addTextAction("重置", longPressMenu::reset)
    }
    fun render() {
        val rows = when (route) {
            SettingsRoute.ROOT -> core.root()
            SettingsRoute.GENERAL -> general.rows()
            SettingsRoute.PRIVACY -> core.privacy()
            SettingsRoute.ADVANCED -> core.advanced()
            SettingsRoute.LABS -> core.labs()
            SettingsRoute.TOOLBAR -> toolbar.rows()
            SettingsRoute.NIGHT -> night.rows()
            SettingsRoute.SSL -> ssl.rows()
            SettingsRoute.OPERATIONS -> operations.rows()
            SettingsRoute.KEYBOARD -> operations.keyboard()
            SettingsRoute.ACTIONS -> operations.actions(requireNotNull(binding))
            SettingsRoute.LONG_PRESS_MENU -> longPressMenu.rows()
            else -> error("This settings route owns a separate page: $route")
        }
        adapter.submit(rows)
    }
    fun created(restored: Boolean) {
        render()
        if (route == SettingsRoute.ACTIONS && !restored) list.post {
            val selected = adapter.selectedPosition()
            if (selected >= 0) list.scrollToPosition(selected)
        }
    }
    private fun headerTapped() {
        val now = SystemClock.elapsedRealtime()
        headerTaps = if (now - lastHeaderTap < 500) headerTaps + 1 else 1
        lastHeaderTap = now
        if (headerTaps == 5) { settings.preferences.labsEnabled = !settings.preferences.labsEnabled; headerTaps = 0; render() }
    }
    private fun command(value: SettingsCommand) {
        when (value) {
            SettingsCommand.HOMEPAGE -> core.homepage()
            SettingsCommand.LANGUAGE -> LanguageSettingsDialog(host).show()
            SettingsCommand.SSL -> ssl.open()
            SettingsCommand.ACTION_CHOSEN -> back()
            else -> external(value)
        }
    }
    override fun close() {
        list.dispose()
        list.adapter = null
    }
}
