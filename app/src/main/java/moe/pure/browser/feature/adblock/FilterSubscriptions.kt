package moe.pure.browser.feature.adblock

import android.content.Context
import moe.pure.browser.feature.adblock.data.NativeFilterSubscriptionStore
import java.io.File

/** UI lists are shallow copies, published through the original process subscription source. */
class FilterSubscriptions(context: Context) : AutoCloseable {
    private val data = NativeFilterSubscriptionStore.get(context)
    private var local: MutableList<FilterSubscription>? = null
    var changed = false
        private set
    fun initialize(presets: List<FilterPreset>) {
        local = if (data.missing) presets.map { new(it.url, it.title) }.toMutableList() else ArrayList(data.all())
    }
    fun all(): List<FilterSubscription> = local ?: data.all()
    private fun editable(): MutableList<FilterSubscription> = local ?: data.all()
    private fun new(url: String, title: String?) = FilterSubscription(url, title, filePath = data.newPath())
    fun add(url: String, title: String? = null): FilterSubscription {
        editable().firstOrNull { it.url == url }?.let { return it }
        return new(url, title).also { editable().add(it); changed = true }
    }
    fun save(item: FilterSubscription) {
        val items = editable()
        val index = items.indexOfFirst { it.id == item.id }
        if (index < 0) items.add(item) else items[index] = item
        changed = false
        publish()
    }
    fun setEnabled(id: String, enabled: Boolean) { editable().first { it.id == id }.enabled = enabled; changed = true }
    fun setUrl(id: String, url: String) { editable().first { it.id == id }.url = url; changed = true }
    fun remove(id: String) {
        val item = editable().firstOrNull { it.id == id } ?: return
        editable().remove(item); item.filePath?.let { File(it).delete() }; changed = true
    }
    fun markChanged() { changed = true }
    fun consumeChanged(): Boolean = changed.also { changed = false }
    fun publish(): Boolean = data.publish(editable())
    fun file(id: String): File = File(requireNotNull(all().first { it.id == id }.filePath))
    fun texts(): List<File> = all().filter(FilterSubscription::enabled).mapNotNull { it.filePath?.let(::File) }.filter(File::isFile)
    override fun close() = Unit
}
