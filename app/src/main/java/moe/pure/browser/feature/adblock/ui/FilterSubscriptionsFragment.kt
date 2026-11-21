package moe.pure.browser.feature.adblock.ui

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.adblock.AdblockEngine
import moe.pure.browser.feature.adblock.FilterDefaults
import moe.pure.browser.feature.adblock.FilterSubscription
import moe.pure.browser.feature.adblock.FilterSubscriptions
import moe.pure.browser.feature.adblock.update.NativeFilterUpdatePreferences
import moe.pure.browser.feature.adblock.update.NativeFilterUpdateWorker
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.tasks.NativeViewJobs
import moe.pure.browser.ui.theme.ThemedActivity

/** z7.q0 owns its shallow subscription list; only the update-all work outlives its view. */
class FilterSubscriptionsFragment : AdblockRecordPage() {
    private val repository by lazy { FilterSubscriptions(requireContext().applicationContext) }
    private val preferences by lazy { NativeFilterUpdatePreferences(requireContext()) }
    private val presets get() = FilterDefaults.forLanguage(requireContext(), resources.configuration.locales[0].language)
    private var page: AdblockListPage? = null
    private var adapter: FilterSubscriptionRows? = null
    private var subscriptionDialogs: FilterSubscriptionDialogs? = null
    private var batchJobs: NativeViewJobs? = null
    private var updating = false
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        return AdblockListPage(activity, getString(R.string.ref_adblock_page_subscriptions), appliesWindowInsets, ::closePage).also {
            page = it
            adapter = FilterSubscriptionRows(activity, ::interval, { item -> repository.setEnabled(item.id, !item.enabled) },
                { item, anchor -> subscriptionDialogs?.menu(item, anchor) }, ::detail)
            it.list.adapter = adapter
            it.page.addIconAction(R.drawable.ref_ic_add, getString(R.string.ref_adblock_page_new)) { subscriptionDialogs?.create(presets) }
            it.page.addTextAction(getString(R.string.ref_adblock_page_update), ::updateAll)
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository.initialize(presets)
        subscriptionDialogs = FilterSubscriptionDialogs(requireActivity() as ThemedActivity, dialogs, repository, ::render, { item ->
            RecordPageScope.of(this)?.push(FilterPreviewFragment::class.java, AdblockNavigation.previewArguments(item.filePath, item.title))
        }, ::updateOne)
        render()
    }
    private fun render() {
        val rows = repository.all()
        adapter?.submit(rows, getString(R.string.ref_adblock_page_automatic), intervalLabels()[intervalIndex()], getString(R.string.ref_adblock_page_subscriptions))
        page?.empty(rows.isEmpty())
    }
    private fun detail(item: FilterSubscription): String {
        val timestamp = item.updated
        val updated = if (timestamp == 0L) getString(R.string.ref_adblock_page_never)
            else getString(R.string.ref_adblock_page_updated_time, relative(timestamp))
        return getString(R.string.ref_adblock_page_subscription_detail,
            resources.getQuantityString(R.plurals.ref_adblock_page_rule_count, item.count, item.count), updated)
    }
    private fun relative(timestamp: Long): CharSequence {
        val now = System.currentTimeMillis()
        return if (now - timestamp < 60000L) getString(R.string.ref_script_page_just_now)
            else DateUtils.getRelativeTimeSpanString(timestamp, now, 60000, 65552)
    }
    private fun intervalLabels() = resources.getStringArray(R.array.ref_script_popup_intervals).toList()
    private fun intervalIndex(): Int {
        var selected = 0
        NativeFilterUpdatePreferences.INTERVALS.forEachIndexed { index, period -> if (preferences.interval >= period) selected = index }
        return selected
    }
    private fun interval() {
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_adblock_page_automatic))
            .singleChoice(intervalLabels(), intervalIndex()) { index -> preferences.interval = NativeFilterUpdatePreferences.INTERVALS[index]; render() })
    }
    private fun updateOne(item: FilterSubscription) {
        message(R.string.ref_adblock_page_update_started)
        work({ NativeFilterUpdateWorker.update(item) }, error = { NativeLog.error(null, it) }) { downloaded ->
            if (downloaded && repository.all().any { it.url.equals(item.url, true) }) render()
        }
    }
    private fun updateAll() {
        if (updating) return
        updating = true
        val enabled = repository.all().filter(FilterSubscription::enabled)
        if (enabled.isEmpty()) return
        preferences.completedAt = System.currentTimeMillis(); message(R.string.ref_adblock_page_download_started)
        val jobs = NativeViewJobs().also { batchJobs = it }
        var pending = enabled.size
        enabled.forEach { item -> jobs.launch({ NativeFilterUpdateWorker.update(item) }, failed = { error ->
            jobs.close(); NativeLog.error(null, error)
        }) { downloaded ->
            if (downloaded && repository.all().any { it.url.equals(item.url, true) }) { repository.markChanged(); if (view != null) render() }
            pending--
            if (pending == 0) {
                if (repository.changed) message(R.string.ref_adblock_page_updated)
                commitChanges(); updating = false; jobs.close(); batchJobs = null
            }
        } }
    }
    private fun commitChanges() {
        if (!repository.consumeChanged()) return
        val app = requireContext().applicationContext
        val data = repository
        BrowserExecutors.io.execute { if (data.publish()) AdblockEngine.changed(app) }
    }
    private fun message(id: Int) = BrowserMessages.show(requireActivity(), getString(id))
    override fun onPause() { commitChanges(); super.onPause() }
    override fun onDestroyView() { subscriptionDialogs = null; page?.close(); page = null; adapter = null; super.onDestroyView() }
    override fun onDestroy() { batchJobs?.close(); batchJobs = null; repository.close(); super.onDestroy() }
}
