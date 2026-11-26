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
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import moe.via.browser.R
import moe.via.browser.feature.settings.SettingRow
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.feature.settings.ui.SettingsRowAdapter
import moe.via.browser.feature.site.data.SharedSiteData
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
import moe.via.browser.ui.pages.RecordPageScope
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
    private var shared: SharedSiteData? = null
    private var search = ""
    private var request = 0L
    private var loading = true
    private var loadFailed = false
    private var clearing = false
    private var selecting = false
    private val selectedSites = linkedSetOf<String>()
    private var titleView: TextView? = null
    private var selectionAction: TextView? = null
    private var footer: SiteDataFooterView? = null
    private var statusView: TextView? = null
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
        val status = NativeTypography.text(activity).apply {
            textSize = 14f
            setTextColor(activity.palette.secondary)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            visibility = View.GONE
        }
        statusView = status
        val actions = SiteDataFooterView(activity, ::toggleSort, ::confirmClearSites)
        footer = actions
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            isFocusableInTouchMode = true
            addView(searchField, LinearLayout.LayoutParams(-1, dp(40)).apply {
                setMargins(dp(14), dp(10), dp(14), dp(10))
            })
            addView(status, LinearLayout.LayoutParams(-1, -2))
            addView(recycler, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(actions, LinearLayout.LayoutParams(-1, -2))
        }
        val page = PageScaffold(activity, getString(R.string.site_data_title), {
            if (!consumeBack()) closePage()
        }, appliesWindowInsets)
        titleView = page.titleView
        selectionAction = page.toolbar.addTextAction(getString(R.string.site_data_select_sites)) {
            if (selecting) finishSelection() else beginSelection()
        }
        NativeToolbarTap.bind(page.toolbar) { recycler.smoothScrollToPosition(0) }
        page.showContent(content, bottomInsets = actions)
        content.requestFocus()
        return page.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        jobs = NativeViewJobs()
        reload()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) reload()
    }

    private fun reload() {
        val loader = jobs ?: return
        val generation = ++request
        loading = true
        loadFailed = false
        render()
        fun submit(reported: Map<String, Long>) {
            if (request != generation) return
            loader.launch({ store.snapshot(reported) }, failed = {
                if (request == generation) {
                    loading = false
                    loadFailed = true
                    render()
                }
            }) { result ->
                if (request == generation) {
                    entries = result.entries
                    shared = result.shared
                    selectedSites.retainAll(entries.mapTo(mutableSetOf()) { it.site })
                    loading = false
                    render()
                }
            }
        }
        runCatching { store.reportedOrigins(::submit) }.onFailure { submit(emptyMap()) }
    }

    private fun render() {
        val rows = adapter ?: return
        val canSelect = store.canClearSite
        titleView?.text = if (selecting) getString(R.string.site_data_selected_count, selectedSites.size)
            else getString(R.string.site_data_title)
        selectionAction?.apply {
            setText(if (selecting) R.string.site_data_cancel_selection else R.string.site_data_select_sites)
            visibility = if (selecting || canSelect) View.VISIBLE else View.GONE
            isEnabled = !clearing && (selecting || entries.isNotEmpty())
            alpha = if (isEnabled) 1f else .45f
        }
        footer?.render(sortBySize, selecting, selectedSites.isNotEmpty(), clearing, shared)
        val query = search.trim()
        val filtered = entries.withIndex().filter { (_, entry) ->
            entry.site.contains(query, ignoreCase = true) ||
                entry.addresses.any { it.origin.contains(query, ignoreCase = true) }
        }
        val status = when {
            clearing -> R.string.site_data_clearing
            loading && entries.isEmpty() -> R.string.site_data_loading
            loadFailed -> R.string.site_data_load_failed
            filtered.isEmpty() -> R.string.site_data_empty
            else -> null
        }
        statusView?.apply {
            text = status?.let(::getString)
            visibility = if (status == null) View.GONE else View.VISIBLE
        }
        rows.submit(buildList {
            val displayed = if (sortBySize) filtered.sortedByDescending { it.value.bytes } else filtered
            displayed.forEach { (index, entry) ->
                val detail = formatSize(entry.bytes)
                val site = entry.site
                if (selecting) {
                    add(SettingsItem.toggle(index + 2, site, site in selectedSites, detail,
                        enabled = !clearing, updateCheckOnClick = false) { checked ->
                        if (checked) selectedSites.add(site) else selectedSites.remove(site)
                    })
                } else {
                    val longPress: (() -> Unit)? = if (canSelect) ({ beginSelection(site) }) else null
                    add(SettingsItem.Entry(index + 2, SettingRow.Link(site, detail,
                        enabled = !clearing, onLongClick = longPress) { showSite(entry) }))
                }
            }
        })
    }

    private fun toggleSort() {
        if (clearing) return
        sortBySize = !sortBySize
        preferences.edit().putBoolean(SORT_BY_SIZE, sortBySize).apply()
        render()
        list?.scrollToPosition(0)
    }

    private fun formatSize(bytes: Long) = Formatter.formatShortFileSize(requireContext(), bytes)

    private fun beginSelection(site: String? = null) {
        if (clearing || !store.canClearSite || entries.isEmpty()) return
        selecting = true
        if (site != null) selectedSites.add(site)
        render()
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

    private fun showSite(entry: SiteDataEntry) {
        if (clearing) return
        (view?.findFocus() as? EditText)?.let { input ->
            window.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(input.windowToken, 0)
            input.clearFocus()
        }
        RecordPageScope.of(this)?.push(SiteDataDetailsFragment::class.java, SiteDataDetailsFragment.arguments(entry))
    }

    override fun onDestroyView() {
        request++
        jobs?.close(); jobs = null
        dialogs?.dispose(); dialogs = null
        list?.dispose(); list?.adapter = null; list = null
        titleView = null
        selectionAction = null
        footer = null
        statusView = null
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
