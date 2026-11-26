package moe.via.browser.feature.settings.pages

import android.os.SystemClock
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import moe.via.browser.R
import moe.via.browser.feature.library.ui.NativeLibrarySearch
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.settings.SettingsRoute
import moe.via.browser.storage.preferences.ActionBinding
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.ui.*
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.components.recycler.NativeRecyclerView
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import androidx.recyclerview.widget.LinearLayoutManager

/** Standard settings content delegates values to page providers and navigation to its Fragment. */
internal class SettingsPageContent(private val activity: ThemedActivity, private val route: SettingsRoute,
    private val binding: ActionBinding?, private val entry: Int?, applyInsets: Boolean, private val back: () -> Unit,
    private val navigate: (SettingsRoute, ActionBinding?, Int?) -> Unit, private val external: (SettingsCommand) -> Unit) : AutoCloseable {
    private val settings = BrowserSettings(activity)
    private val page = PageScaffold(activity, activity.getString(route.titleRes), back, applyInsets)
    private val host: SettingsHost = SettingsHost(activity, settings, { navigate(it, null, null) }, ::render, ::command)
    private val adapter = SettingsRowAdapter(activity, ::render).apply { searchTarget = entry }
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
    private val operations by lazy { OperationSettingsPage(host) { navigate(SettingsRoute.ACTIONS, it, null) } }
    private val night by lazy { NightSettingsPage(host) }
    private val ssl by lazy { SslSettingsPage(host) }
    private val longPressMenu by lazy { LongPressMenuSettingsPage(host) }
    private val search by lazy {
        SettingsSearch(activity, ::rows, { destination, target -> navigate(destination, null, target) }, ::hideSearchKeyboard)
    }
    private val searchInput = if (route != SettingsRoute.ROOT) null else NativeTypography.edit(activity).apply {
        id = R.id.settings_search_field
    }
    private var searchStyle: NativeLibrarySearch? = null
    private var headerTaps = 0
    private var lastHeaderTap = 0L
    val view get() = page.root

    init {
        val input = searchInput
        if (input == null) page.showContent(list, bottomInsets = list)
        else {
            val content = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                isFocusableInTouchMode = true
                defaultFocusHighlightEnabled = false
                addView(input, LinearLayout.LayoutParams(-1, -2))
                addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
            }
            searchStyle = NativeLibrarySearch(input)
            input.setHint(R.string.settings_search_hint)
            input.imeOptions = EditorInfo.IME_ACTION_SEARCH
            input.setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_SEARCH) { hideSearchKeyboard(); true } else false
            }
            input.doAfterTextChanged {
                renderSearch()
                list.scrollToPosition(0)
            }
            page.showContent(content, bottomInsets = list)
            content.requestFocus()
        }
        if (route in setOf(SettingsRoute.GENERAL, SettingsRoute.PRIVACY, SettingsRoute.NIGHT, SettingsRoute.OPERATIONS)) {
            list.itemAnimator = null
        }
        NativeToolbarTap.bind(page.toolbar) { list.smoothScrollToPosition(0) }
        if (route == SettingsRoute.ROOT) page.titleView.setOnClickListener { headerTapped() }
        if (route == SettingsRoute.SSL) page.addTextAction(activity.getString(R.string.settings_help), ssl::help)
        if (route == SettingsRoute.LONG_PRESS_MENU) page.addTextAction(activity.getString(R.string.ref_menu_reset), longPressMenu::reset)
    }
    fun render() {
        if (searchInput == null) adapter.submit(rows(route))
        else {
            search.invalidate()
            renderSearch()
        }
    }

    private fun renderSearch() {
        adapter.submit(search.rows(searchInput?.text?.toString().orEmpty(), settings.preferences.labsEnabled))
    }

    private fun hideSearchKeyboard() {
        val input = searchInput ?: return
        activity.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(input.windowToken, 0)
        input.clearFocus()
    }

    private fun rows(route: SettingsRoute): List<SettingsItem> = when (route) {
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

    fun created(restored: Boolean) {
        render()
        if (entry != null && !restored) {
            val position = adapter.positionOf(entry)
            if (position >= 0) (list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, activity.dp(12))
        }
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
        searchStyle?.close()
        searchStyle = null
        list.dispose()
        list.adapter = null
    }
}
