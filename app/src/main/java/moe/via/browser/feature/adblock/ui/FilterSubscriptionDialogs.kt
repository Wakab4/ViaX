package moe.via.browser.feature.adblock.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.view.View
import moe.via.browser.R
import moe.via.browser.feature.adblock.FilterPreset
import moe.via.browser.feature.adblock.FilterSubscription
import moe.via.browser.feature.adblock.FilterSubscriptions
import moe.via.browser.feature.search.resolve.SearchUrlResolver
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.webview.compat.NativePageAddress

internal class FilterSubscriptionDialogs(private val activity: ThemedActivity, private val scope: DialogScope,
    private val repository: FilterSubscriptions, private val changed: () -> Unit,
    private val preview: (FilterSubscription) -> Unit, private val update: (FilterSubscription) -> Unit) {
    fun create(presets: List<FilterPreset>) {
        val remaining = presets.filter { candidate -> repository.all().none { it.url == candidate.url } }
        if (remaining.isEmpty()) { edit(); return }
        scope.show(BrowserDialog(activity, text(R.string.ref_adblock_page_add_subscription))
            .items(remaining.map { it.title } + text(R.string.ref_adblock_page_custom)) { index ->
                if (index == remaining.size) edit() else { repository.add(remaining[index].url, remaining[index].title); changed() }
            })
    }
    private fun edit(item: FilterSubscription? = null) {
        val dialog = BrowserDialog(activity, text(if (item == null) R.string.ref_adblock_page_add_subscription else R.string.ref_adblock_page_edit_subscription))
        val input = dialog.field(item?.url ?: "https://", text(R.string.ref_adblock_page_address), 3)
        dialog.setCanceledOnTouchOutside(false)
        scope.show(dialog.negative(text(android.R.string.cancel)).positive(text(android.R.string.ok)) {
            val value = input.text.toString()
            if (!SearchUrlResolver.isAddress(value)) { message(R.string.ref_adblock_page_invalid_url); return@positive true }
            val normalized = SearchUrlResolver.normalize(value)
            if (repository.all().any { row -> row.url.equals(normalized, true) }) { message(R.string.ref_adblock_page_exists); return@positive true }
            if (item == null) repository.add(normalized) else repository.setUrl(item.id, normalized)
            changed(); true
        })
    }
    fun menu(item: FilterSubscription, anchor: View) {
        val actions = listOf(R.string.ref_adblock_page_preview, R.string.ref_adblock_page_update,
            R.string.ref_adblock_page_edit, R.string.ref_adblock_page_copy_link, R.string.ref_adblock_page_delete).map(::text)
        scope.show(BrowserDialog(activity).items(actions) { index -> when (index) {
            0 -> preview(item)
            1 -> update(item)
            2 -> edit(item)
            3 -> {
                activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(null, item.url))
                message(R.string.ref_adblock_page_copied)
            }
            4 -> scope.show(BrowserDialog(activity, text(R.string.ref_adblock_page_delete), activity.getString(R.string.ref_adblock_page_delete_message,
                item.title ?: NativePageAddress.authority(item.url))).negative(text(android.R.string.cancel)).positive(text(android.R.string.ok)) {
                repository.remove(item.id); changed(); true
            })
        } }.anchoredTo(anchor))
    }
    private fun text(id: Int) = activity.getString(id)
    private fun message(id: Int) = BrowserMessages.show(activity, text(id))
}
