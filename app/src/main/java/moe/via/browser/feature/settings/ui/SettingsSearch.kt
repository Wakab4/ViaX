package moe.via.browser.feature.settings.ui

import moe.via.browser.R
import moe.via.browser.feature.ai.ui.settings.AiSettingsEntry
import moe.via.browser.feature.reader.ui.ReaderSettingsEntry
import moe.via.browser.feature.search.ui.settings.SearchSettingsEntry
import moe.via.browser.feature.settings.SettingRow
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.SettingsRoute
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.settings.NativeSettingViews
import moe.via.browser.ui.theme.ThemedActivity
import java.text.Normalizer
import java.util.Locale

internal class SettingsSearch(
    private val activity: ThemedActivity,
    private val pageRows: (SettingsRoute) -> List<SettingsItem>,
    private val open: (SettingsRoute, Int?) -> Unit,
    private val beforeOpen: () -> Unit,
) {
    private data class Result(
        val title: String,
        val path: String,
        val terms: String,
        val open: () -> Unit,
    ) {
        val normalizedTitle = normalize(title)
        val searchableText = normalize("$title $path $terms")
    }

    private var index: List<Result>? = null

    fun invalidate() { index = null }

    fun rows(query: String, labsEnabled: Boolean): List<SettingsItem> {
        val text = normalize(query).trim()
        if (text.isEmpty()) return pageRows(SettingsRoute.ROOT)
        val words = text.split(whitespace)
        val candidates = index ?: buildIndex(labsEnabled).also { index = it }
        val matches = candidates.withIndex()
            .filter { (_, result) -> words.all { it in result.searchableText } }
            .sortedBy { (_, result) ->
                when {
                    result.normalizedTitle == text -> 0
                    result.normalizedTitle.startsWith(text) -> 1
                    text in result.normalizedTitle -> 2
                    else -> 3
                }
            }
        if (matches.isEmpty()) return listOf(SettingsItem.content("no-results", Unit) {
            NativeSettingViews.label(activity, detail = true).apply {
                setText(R.string.settings_search_empty)
                setPaddingRelative(dp(16), dp(16), dp(16), dp(16))
            }
        })
        return matches.map { (index, result) ->
            SettingsItem.link(-1 - index, result.title, result.path) {
                beforeOpen()
                result.open()
            }
        }
    }

    private fun buildIndex(labsEnabled: Boolean): List<Result> = buildList {
        val cache = activity.getString(R.string.settings_clear_cache)
        val cookies = activity.getString(R.string.site_data_cookies)
        val aliases = mapOf(
            activity.getString(R.string.web_dns_title) to "DoH",
            activity.getString(R.string.settings_site_data) to "${activity.getString(R.string.site_data_title)} $cache $cookies",
            activity.getString(R.string.ref_menu_clear_data) to "$cache $cookies",
            activity.getString(R.string.ref_ua_title) to "UA User Agent",
        )
        fun addPage(route: SettingsRoute, parent: SettingsRoute? = null) {
            val path = listOfNotNull(parent, route.takeUnless { it == SettingsRoute.ROOT })
                .map { activity.getString(it.titleRes) }
            var section: String? = null
            for (item in pageRows(route)) {
                when (item) {
                    is SettingsItem.Section -> section = item.row.title
                    is SettingsItem.Entry -> {
                        val row = item.row
                        val (title, detail) = when (row) {
                            is SettingRow.Link -> row.title to row.detail
                            is SettingRow.Toggle -> row.title to row.detail
                            is SettingRow.Radio -> row.title to row.detail
                            else -> continue
                        }
                        val terms = "$detail ${aliases[title].orEmpty()}"
                        val location = (path + listOfNotNull(section)).joinToString(" › ")
                        if (route == SettingsRoute.ROOT && row is SettingRow.Link) {
                            add(Result(title, location, terms, row.onClick))
                        } else {
                            add(Result(title, location, terms) { open(route, item.id) })
                        }
                    }
                    is SettingsItem.Content -> Unit
                }
            }
        }

        fun addFields(
            path: String,
            vararg titles: Int,
            terms: String = "",
            openPage: () -> Unit,
        ) {
            titles.forEach { add(Result(activity.getString(it), path, terms, openPage)) }
        }

        addPage(SettingsRoute.ROOT)
        addPage(SettingsRoute.GENERAL)
        addPage(SettingsRoute.ADVANCED)
        addPage(SettingsRoute.PRIVACY)
        addPage(SettingsRoute.NIGHT, SettingsRoute.GENERAL)
        addPage(SettingsRoute.TOOLBAR, SettingsRoute.GENERAL)
        addPage(SettingsRoute.OPERATIONS, SettingsRoute.GENERAL)
        addPage(SettingsRoute.LONG_PRESS_MENU, SettingsRoute.GENERAL)
        if (labsEnabled) addPage(SettingsRoute.LABS)

        addFields(
            activity.getString(R.string.proxy_title),
            R.string.proxy_enable, R.string.proxy_host, R.string.proxy_port, R.string.proxy_username,
            R.string.proxy_password, R.string.proxy_import_title,
        ) { open(SettingsRoute.PROXY, null) }
        addFields(activity.getString(R.string.proxy_title), R.string.proxy_type, terms = "HTTP HTTPS SOCKS SOCKS5") {
            open(SettingsRoute.PROXY, null)
        }
        val general = activity.getString(R.string.settings_general)
        addFields("$general › ${activity.getString(R.string.ref_search_page_title)}",
            R.string.ref_search_page_engines, R.string.ref_search_page_suggestions, R.string.ref_search_page_toolbar) {
            SearchSettingsEntry.open(activity)
        }
        addFields("$general › ${activity.getString(R.string.ref_ai_page_settings)}",
            R.string.ref_ai_page_backends, R.string.ref_ai_page_prompts) {
            AiSettingsEntry.open(activity)
        }
        addFields("$general › ${activity.getString(R.string.ref_reader_settings_title)}",
            R.string.ref_reader_settings_font_size, R.string.ref_reader_settings_confirmation, R.string.ref_reader_settings_css) {
            ReaderSettingsEntry.open(activity)
        }
    }

    private companion object {
        val whitespace = Regex("\\s+")
        fun normalize(text: String) = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
    }
}
