package moe.via.browser.feature.site.ui.data

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import moe.via.browser.R
import moe.via.browser.feature.pageinfo.SiteCookies
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.host.SettingsRuntimeFragment
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.feature.settings.ui.SettingsCommand
import moe.via.browser.feature.settings.ui.SettingsRowAdapter
import moe.via.browser.feature.site.data.SiteDataAddress
import moe.via.browser.feature.site.data.SiteDataEntry
import moe.via.browser.feature.site.data.SiteDataStore
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.via.browser.ui.components.recycler.NativeRecyclerView
import moe.via.browser.ui.components.surface
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.tasks.NativeViewJobs
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.theme.palette

class SiteDataSettingsFragment : SettingsRecordPage() {
    private val window get() = requireActivity() as ThemedActivity
    private lateinit var store: SiteDataStore
    private var adapter: SettingsRowAdapter? = null
    private var list: NativeRecyclerView? = null
    private var dialogs: DialogScope? = null
    private var jobs: NativeViewJobs? = null
    private var entries = emptyList<SiteDataEntry>()
    private var search = ""
    private var request = 0L
    private var loading = true
    private var loadFailed = false
    private var clearing = false
    private var selecting = false
    private val selectedSites = linkedSetOf<String>()
    private var titleView: TextView? = null
    private var primaryAction: TextView? = null
    private val preferences get() = window.getSharedPreferences("browser", Context.MODE_PRIVATE)
    private var sortBySize = false

    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = window
        store = SiteDataStore(activity)
        sortBySize = preferences.getBoolean(SORT_BY_SIZE, false)
        dialogs = DialogScope()
        val rows = SettingsRowAdapter(activity, ::render)
        adapter = rows
        val recycler = NativeRecyclerView(activity).apply {
            id = R.id.settings_page_list
            layoutManager = LinearLayoutManager(activity)
            itemAnimator = null
            adapter = rows
            clipToPadding = false
            NativeRecyclerEdgeEffect.install(this)
        }
        list = recycler
        val searchField = NativeTypography.edit(activity).apply {
            setHint(R.string.site_data_search)
            setSingleLine(true)
            textSize = 14f
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setTextColor(activity.palette.text)
            setHintTextColor(activity.palette.hint)
            background = activity.surface(activity.palette.input, 20)
            setPadding(dp(16), 0, dp(16), 0)
            setText(search)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    search = s?.toString().orEmpty()
                    render()
                }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            isFocusableInTouchMode = true
            addView(searchField, LinearLayout.LayoutParams(-1, dp(42)).apply {
                setMargins(dp(14), dp(10), dp(14), dp(6))
            })
            addView(recycler, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        val page = PageScaffold(activity, getString(R.string.site_data_title), {
            if (!consumeBack()) closePage()
        }, appliesWindowInsets)
        titleView = page.titleView
        page.addIconAction(R.drawable.ref_ic_menu, getString(R.string.site_data_actions), ::showActions)
        primaryAction = page.toolbar.addTextAction(getString(R.string.site_data_global_clear)) {
            if (!clearing) {
                if (selecting) confirmClearSites()
                else SettingsRuntimeFragment.of(activity).perform(SettingsCommand.CLEAR_DATA)
            }
        }
        NativeToolbarTap.bind(page.toolbar) { recycler.smoothScrollToPosition(0) }
        page.showContent(content, bottomInsets = recycler)
        content.requestFocus()
        return page.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        jobs = NativeViewJobs()
        reload()
    }

    private fun reload() {
        val loader = jobs ?: return
        val generation = ++request
        loading = true
        loadFailed = false
        render()
        fun submit(reported: Map<String, Long>) {
            if (request != generation) return
            loader.launch({ store.entries(reported) }, failed = {
                if (request == generation) {
                    loading = false
                    loadFailed = true
                    render()
                }
            }) { result ->
                if (request == generation) {
                    entries = result
                    loading = false
                    render()
                }
            }
        }
        runCatching { store.reportedOrigins(::submit) }.onFailure { submit(emptyMap()) }
    }

    private fun render() {
        val rows = adapter ?: return
        titleView?.text = if (selecting) getString(R.string.site_data_selected_count, selectedSites.size)
            else getString(R.string.site_data_title)
        primaryAction?.apply {
            text = getString(if (selecting) R.string.site_data_clear_selected else R.string.site_data_global_clear)
            isEnabled = !clearing && (!selecting || selectedSites.isNotEmpty())
            alpha = if (isEnabled) 1f else .45f
        }
        val query = search.trim()
        val filtered = entries.withIndex().filter { (_, entry) -> entry.address.origin.contains(query, ignoreCase = true) }
        rows.submit(buildList {
            if (selecting) add(SettingsItem.section(getString(R.string.site_data_selection_hint)))
            else add(SettingsItem.link(1, getString(R.string.site_data_open_address), getString(R.string.site_data_list_source), onClick = ::enterAddress))
            if (clearing) add(SettingsItem.section(getString(R.string.site_data_clearing)))
            else if (loading && entries.isEmpty()) add(SettingsItem.section(getString(R.string.site_data_loading)))
            else if (loadFailed) add(SettingsItem.section(getString(R.string.site_data_load_failed)))
            else if (filtered.isEmpty()) add(SettingsItem.section(getString(R.string.site_data_empty)))
            val displayed = if (sortBySize) filtered.sortedByDescending { it.value.reportedBytes ?: -1L } else filtered
            displayed.forEach { (index, entry) ->
                val detail = entry.reportedBytes?.let { bytes ->
                    getString(R.string.site_data_storage_reported, Formatter.formatShortFileSize(requireContext(), bytes))
                } ?: getString(if (sortBySize) R.string.site_data_unknown_size else R.string.site_data_recent)
                if (selecting) {
                    val site = entry.address.site
                    add(SettingsItem.toggle(index + 2, entry.address.origin, site in selectedSites, detail,
                        enabled = !clearing, updateCheckOnClick = false) { checked ->
                        if (checked) selectedSites.add(site) else selectedSites.remove(site)
                    })
                } else add(SettingsItem.link(index + 2, entry.address.origin, detail) { showSite(entry.address) })
            }
        })
    }

    private fun showActions() {
        if (clearing) return
        val sortAction = if (sortBySize) R.string.site_data_sort_recent else R.string.site_data_sort_size
        val actions = buildList {
            add(sortAction)
            if (selecting) add(R.string.site_data_cancel_selection)
            else if (store.canClearSite) add(R.string.site_data_select_sites)
            add(R.string.site_data_refresh)
        }
        dialogs?.show(BrowserDialog(window).items(actions.map(::getString)) { index ->
            when (actions[index]) {
                sortAction -> {
                    sortBySize = !sortBySize
                    preferences.edit().putBoolean(SORT_BY_SIZE, sortBySize).apply()
                    render()
                    list?.scrollToPosition(0)
                }
                R.string.site_data_select_sites -> {
                    selecting = true
                    render()
                }
                R.string.site_data_cancel_selection -> finishSelection()
                R.string.site_data_refresh -> reload()
            }
        })
    }

    private fun confirmClearSites() {
        val sites = selectedSites.toList()
        if (sites.isEmpty()) return
        val message = getString(R.string.site_data_select_sites_message) + "\n\n" + sites.joinToString("\n")
        dialogs?.show(BrowserDialog(window, getString(R.string.site_data_selected_count, sites.size), message)
            .positive(getString(R.string.site_data_clear_selected)) { clearSites(sites); true }
            .negative(getString(android.R.string.cancel)))
    }

    private fun finishSelection() {
        selecting = false
        selectedSites.clear()
        render()
    }

    internal override fun consumeBack(): Boolean {
        if (!selecting) return false
        if (!clearing) finishSelection()
        return true
    }

    internal override fun canNavigateBack() = !selecting && super.canNavigateBack()

    private fun clearSites(sites: List<String>) {
        val owner = view ?: return
        clearing = true
        render()
        store.clearSites(sites) { cleared ->
            if (view === owner) {
                clearing = false
                selectedSites.removeAll(cleared)
                if (selectedSites.isEmpty()) finishSelection()
                BrowserMessages.show(window, getString(R.string.site_data_batch_result, cleared.size, sites.size))
                reload()
            }
        }
    }

    private fun enterAddress() {
        if (clearing) return
        val dialog = BrowserDialog(window, getString(R.string.site_data_open_address))
        val field = dialog.field(search, "https://example.com/")
        field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        dialogs?.show(dialog.positive(getString(android.R.string.ok)) {
            val address = SiteDataAddress.parse(field.text.toString(), allowBareHost = true)
            if (address == null) {
                field.error = getString(R.string.site_data_invalid_address)
                false
            } else {
                showSite(address)
                true
            }
        }.negative(getString(android.R.string.cancel)))
    }

    private fun showSite(address: SiteDataAddress) {
        if (clearing) return
        val canClear = store.canClearSite
        val actions = buildList {
            add(getString(R.string.site_data_cookies))
            if (canClear) add(getString(R.string.site_data_clear_site))
        }
        val note = if (canClear) null else getString(R.string.site_data_site_clear_unavailable)
        dialogs?.show(BrowserDialog(window, address.origin, note).items(actions) { index ->
            if (index == 0) showCookies(address) else confirmClearSite(address)
        })
    }

    private fun showCookies(address: SiteDataAddress) {
        val title = getString(R.string.site_data_cookies)
        if (!SiteCookies.supportsDetails) {
            runCatching { SiteCookies.names(address.url) }.onSuccess { names ->
                val summary = if (names.isEmpty()) getString(R.string.site_data_no_cookies)
                else names.joinToString("\n") { it.ifEmpty { getString(R.string.site_data_unnamed_cookie) } }
                dialogs?.show(BrowserDialog(window, title,
                    "${address.url}\n\n$summary\n\n${getString(R.string.site_data_cookie_delete_unavailable)}")
                    .positive(getString(android.R.string.ok)))
            }.onFailure { notify(R.string.site_data_operation_failed) }
            return
        }
        runCatching { SiteCookies.entries(address.url) }.onSuccess { cookies ->
            if (cookies.isEmpty()) {
                dialogs?.show(BrowserDialog(window, title, "${address.url}\n\n${getString(R.string.site_data_no_cookies)}")
                    .positive(getString(android.R.string.ok)))
                return@onSuccess
            }
            val labels = cookies.map { cookie ->
                val name = cookie.name.ifEmpty { getString(R.string.site_data_unnamed_cookie) }
                val partition = if (cookie.partitioned) " · ${getString(R.string.site_data_partitioned_cookie)}" else ""
                "$name\n${cookie.domain}${cookie.path}$partition"
            }
            val message = "${address.url}\n${getString(R.string.site_data_cookie_selection)}"
            dialogs?.show(BrowserDialog(window, title, message)
                .multipleChoice(labels, emptySet())
                .positive(getString(R.string.site_data_delete_selected)) { dialog ->
                    val selected = dialog.selectedIndices.mapNotNull(cookies::getOrNull)
                    if (selected.isEmpty()) false else { deleteCookies(address, selected); true }
                }
                .negative(getString(android.R.string.cancel)))
        }.onFailure { notify(R.string.site_data_operation_failed) }
    }

    private fun deleteCookies(address: SiteDataAddress, selected: List<SiteCookies.Entry>) {
        val owner = view ?: return
        clearing = true
        runCatching {
            SiteCookies.delete(address.url, selected) {
                if (view === owner) {
                    clearing = false
                    runCatching { SiteCookies.entries(address.url) }.onSuccess { remaining ->
                        val count = selected.count { deleted -> remaining.none {
                            it.name == deleted.name && it.domain == deleted.domain && it.path == deleted.path &&
                                it.partitioned == deleted.partitioned
                        } }
                        BrowserMessages.show(window, getString(R.string.site_data_cookie_result, count, selected.size))
                    }.onFailure { notify(R.string.site_data_operation_failed) }
                }
            }
        }.onFailure {
            clearing = false
            notify(R.string.site_data_operation_failed)
        }
    }

    private fun confirmClearSite(address: SiteDataAddress) {
        dialogs?.show(BrowserDialog(window, getString(R.string.site_data_clear_site),
            getString(R.string.site_data_clear_site_message, address.site))
            .positive(getString(R.string.site_data_clear)) { clearSite(address); true }
            .negative(getString(android.R.string.cancel)))
    }

    private fun clearSite(address: SiteDataAddress) {
        val owner = view ?: return
        clearing = true
        runCatching {
            store.clearSite(address) {
                if (view === owner) {
                    clearing = false
                    notify(R.string.site_data_site_cleared)
                    reload()
                }
            }
        }.onFailure {
            clearing = false
            notify(R.string.site_data_operation_failed)
        }
    }

    private fun notify(message: Int) = BrowserMessages.show(window, message)

    override fun onDestroyView() {
        request++
        jobs?.close(); jobs = null
        dialogs?.dispose(); dialogs = null
        list?.dispose(); list?.adapter = null; list = null
        titleView = null
        primaryAction = null
        selecting = false
        selectedSites.clear()
        clearing = false
        adapter = null
        super.onDestroyView()
    }

    private companion object {
        const val SORT_BY_SIZE = "site_data_sort_by_size"
    }
}
