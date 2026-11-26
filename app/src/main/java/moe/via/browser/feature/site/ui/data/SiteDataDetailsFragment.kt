package moe.via.browser.feature.site.ui.data

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import moe.via.browser.R
import moe.via.browser.feature.pageinfo.SiteCookies
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.feature.settings.ui.SettingsRowAdapter
import moe.via.browser.feature.site.data.SiteDataAddress
import moe.via.browser.feature.site.data.SiteDataEntry
import moe.via.browser.feature.site.data.SiteDataStore
import moe.via.browser.ui.components.DividerLayout
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.via.browser.ui.components.recycler.NativeRecyclerView
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.settings.NativeSettingViews
import moe.via.browser.ui.tasks.NativeViewJobs
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.fonts.NativeTypography

class SiteDataDetailsFragment : SettingsRecordPage() {
    private data class CookieKey(val name: String, val domain: String, val path: String, val partition: String?)
    private data class Cookie(val address: SiteDataAddress, val name: String, val info: SiteCookies.Entry?) {
        val key = CookieKey(name, info?.domain ?: address.host, info?.path.orEmpty(),
            address.origin.substringBefore("://").takeIf { info?.partitioned == true })
    }

    private data class Content(
        val list: NativeRecyclerView,
        val adapter: SettingsRowAdapter,
        val deleteAction: TextView,
        val clearAction: TextView,
    )

    private val window get() = requireActivity() as ThemedActivity
    private val site by lazy { requireNotNull(requireArguments().getString(SITE)) }
    private val addresses by lazy {
        requireArguments().getStringArrayList(ADDRESSES).orEmpty()
            .mapNotNull { SiteDataAddress.parse(it) }.filter { it.site == site }
    }
    private lateinit var store: SiteDataStore
    private var content: Content? = null
    private var dialogs: DialogScope? = null
    private var jobs: NativeViewJobs? = null
    private var supportsDetails = false
    private var cookies = emptyList<Cookie>()
    private val selected = mutableSetOf<CookieKey>()
    private var loading = true
    private var loadFailed = false
    private var clearing = false

    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = window
        store = SiteDataStore(activity)
        supportsDetails = SiteCookies.supportsDetails
        dialogs = DialogScope()
        val page = PageScaffold(activity, site, ::closePage, appliesWindowInsets)
        val delete = page.toolbar.addTextAction(getString(R.string.site_data_delete_selected)) { deleteSelected() }
        val adapter = SettingsRowAdapter(activity, ::render)
        val list = NativeRecyclerView(activity).apply {
            id = R.id.settings_page_list
            layoutManager = LinearLayoutManager(activity)
            itemAnimator = null
            clipToPadding = false
            this.adapter = adapter
            NativeRecyclerEdgeEffect.install(this)
        }
        val clear = NativeTypography.text(activity).apply {
            setText(R.string.site_data_clear_site)
            textSize = 14f
            setTextColor(0xFFD64146.toInt())
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            minimumHeight = dp(48)
            setPaddingRelative(dp(16), dp(8), dp(16), dp(8))
            background = activity.touchFeedback()
            setOnClickListener { confirmClearSite() }
        }
        val footer = DividerLayout(activity, atBottom = false).apply {
            addView(clear, LinearLayout.LayoutParams(-1, -2))
            visibility = if (store.canClearSite) View.VISIBLE else View.GONE
        }
        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(footer, LinearLayout.LayoutParams(-1, -2))
        }
        content = Content(list, adapter, delete, clear)
        NativeToolbarTap.bind(page.toolbar) { list.smoothScrollToPosition(0) }
        page.showContent(body, bottomInsets = if (store.canClearSite) footer else list)
        return page.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        jobs = NativeViewJobs()
        reload()
    }

    private fun reload(after: (() -> Unit)? = null) {
        val loader = jobs ?: return
        val sources = addresses
        val details = supportsDetails
        loading = true
        loadFailed = false
        render()
        loader.launch({
            sources.flatMap { address ->
                if (details) SiteCookies.entries(address.url).map { Cookie(address, it.name, it) }
                else SiteCookies.names(address.url).map { Cookie(address, it, null) }
            }.distinctBy(Cookie::key)
        }, failed = {
            loading = false
            loadFailed = true
            render()
        }) { result ->
            cookies = result
            selected.retainAll(result.map(Cookie::key).toSet())
            loading = false
            render()
            after?.invoke()
        }
    }

    private fun render() {
        val page = content ?: return
        val editable = supportsDetails && !loading && !loadFailed && !clearing
        page.deleteAction.apply {
            visibility = if (supportsDetails && cookies.isNotEmpty()) View.VISIBLE else View.GONE
            isEnabled = editable && selected.isNotEmpty()
            alpha = if (isEnabled) 1f else .45f
        }
        page.clearAction.isEnabled = !clearing && !loading
        page.clearAction.alpha = if (page.clearAction.isEnabled) 1f else .45f
        page.adapter.submit(buildList {
            add(SettingsItem.section(getString(R.string.site_data_cookies)))
            val status = when {
                clearing -> R.string.site_data_clearing
                loadFailed -> R.string.site_data_cookies_load_failed
                loading && cookies.isEmpty() -> R.string.site_data_cookies_loading
                cookies.isEmpty() -> R.string.site_data_no_cookies
                else -> null
            }
            status?.let { add(message(it)) }
            cookies.forEachIndexed { index, cookie ->
                val name = cookie.name.ifEmpty { getString(R.string.site_data_unnamed_cookie) }
                val info = cookie.info
                if (info == null) add(readOnlyCookie(index, name, cookie.address.host))
                else {
                    val partition = if (info.partitioned) " · ${getString(R.string.site_data_partitioned_cookie)}" else ""
                    add(SettingsItem.toggle(index, name, cookie.key in selected, "${info.domain}${info.path}$partition",
                        enabled = editable, updateCheckOnClick = false) { checked ->
                        if (checked) selected.add(cookie.key) else selected.remove(cookie.key)
                    })
                }
            }
            if (!supportsDetails && cookies.isNotEmpty()) add(message(R.string.site_data_cookie_delete_unavailable))
            if (!store.canClearSite) add(message(R.string.site_data_site_clear_unavailable))
        })
    }

    private fun message(resource: Int) = SettingsItem.content(resource, resource) {
        NativeSettingViews.label(window, detail = true).apply {
            setText(resource)
            setPaddingRelative(dp(16), dp(8), dp(16), dp(12))
        }
    }

    private fun readOnlyCookie(index: Int, name: String, host: String) =
        SettingsItem.content(index, name to host) {
            NativeSettingViews.site(window).apply {
                (getChildAt(0) as TextView).text = name
                (getChildAt(1) as TextView).apply { text = host; visibility = View.VISIBLE }
                background = null
            }
        }

    private fun deleteSelected() {
        val owner = view ?: return
        if (clearing || loading || selected.isEmpty()) return
        val keys = selected.toSet()
        val pending = ArrayDeque(cookies.filter { it.key in keys }
            .groupBy { it.address.url }.entries)
        clearing = true
        render()
        fun deleteNext() {
            while (pending.isNotEmpty()) {
                val group = pending.removeFirst()
                try {
                    SiteCookies.delete(group.key, group.value.mapNotNull(Cookie::info), ::deleteNext)
                    return
                } catch (_: RuntimeException) {
                    // Reloading the list accounts for cookies that could not be deleted.
                }
            }
            if (view === owner) {
                clearing = false
                selected.clear()
                reload {
                    val count = keys.size - cookies.count { it.key in keys }
                    BrowserMessages.show(window, getString(R.string.site_data_cookie_result, count, keys.size))
                }
            }
        }
        deleteNext()
    }

    private fun confirmClearSite() {
        if (clearing || loading || !store.canClearSite) return
        dialogs?.show(BrowserDialog(window, getString(R.string.site_data_clear_site),
            getString(R.string.site_data_clear_site_message, site))
            .positive(getString(R.string.site_data_clear)) { clearSite(); true }
            .negative(getString(android.R.string.cancel)))
    }

    private fun clearSite() {
        val owner = view ?: return
        clearing = true
        render()
        runCatching {
            store.clearSite(site) {
                if (view === owner) {
                    clearing = false
                    BrowserMessages.show(window, R.string.site_data_site_cleared)
                    closePage()
                }
            }
        }.onFailure {
            clearing = false
            render()
            BrowserMessages.show(window, R.string.site_data_operation_failed)
        }
    }

    internal override fun consumeBack() = clearing

    internal override fun canNavigateBack() = !clearing && super.canNavigateBack()

    override fun onDestroyView() {
        jobs?.close(); jobs = null
        dialogs?.dispose(); dialogs = null
        content?.list?.apply { dispose(); adapter = null }
        content = null
        cookies = emptyList()
        selected.clear()
        clearing = false
        super.onDestroyView()
    }

    internal companion object {
        private const val SITE = "site"
        private const val ADDRESSES = "addresses"

        fun arguments(entry: SiteDataEntry) = Bundle().apply {
            putString(SITE, entry.site)
            putStringArrayList(ADDRESSES, ArrayList(entry.addresses.map(SiteDataAddress::url)))
        }
    }
}
