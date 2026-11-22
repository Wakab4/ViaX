package moe.via.browser.browser.session.state

import android.os.Bundle
import android.os.Parcel

/** r4.d's TITLE/URL/LIST/CUR envelope, containing t4.c WebView bundles rather than one raw bundle. */
object TabNativeStateCodec {
    data class PageList(val pages: List<Bundle>, val current: Int)
    private data class Pages(val values: List<Bundle>, val current: Int)

    fun metadata(url: String, title: String): ByteArray = encode(Bundle().apply {
        putString("URL", url); putString("TITLE", title)
    })

    fun decode(outer: ByteArray?): Bundle? = readBundle(outer)?.takeIf {
        it.containsKey("URL") || it.containsKey("TITLE") || it.containsKey("LIST") || it.containsKey("CUR")
    }

    fun current(outer: ByteArray?): Bundle? {
        val pages = decode(outer)?.let(::pages) ?: return null
        return pages.values.getOrNull(pages.current)?.let(::Bundle)
    }

    fun pageList(outer: ByteArray?): PageList? = decode(outer)?.let(::pages)?.let {
        PageList(it.values.map(::Bundle), it.current)
    }

    /** A copied/reopened tab is a new callback source, even when its Native navigation state is copied. */
    @Suppress("DEPRECATION")
    fun forkPageIdentities(outer: ByteArray?): ByteArray? {
        val copy = decode(outer) ?: return outer
        copy.getParcelableArray("LIST")?.forEach { if (it is Bundle) it.remove(TabPageIdentity.KEY) }
        return encode(copy)
    }

    fun wrapPages(values: List<Bundle>, current: Int, title: String, url: String): ByteArray {
        if (values.isEmpty()) return metadata(url, title)
        val index = current.coerceIn(values.indices)
        val first = maxOf(0, index - 6)
        val end = minOf(values.size, index + 4)
        return encode(Bundle().apply {
            putString("TITLE", title); putString("URL", url)
            putParcelableArray("LIST", values.subList(first, end).map(::Bundle).toTypedArray())
            putInt("CUR", index - first)
        })
    }

    fun wrapCurrent(rawBundle: Bundle, title: String, url: String, scrollX: Int = 0, scrollY: Int = 0,
        previousOuter: ByteArray? = null): ByteArray {
        val stored = decode(previousOuter)?.let(::pages)
        val values = stored?.values?.toMutableList() ?: mutableListOf()
        val current = if (values.isEmpty()) 0 else stored!!.current
        val page = Bundle(rawBundle).apply {
            if (url.isNotEmpty()) putString("url", url)
            val scroll = scrollX.toLong() or (scrollY.toLong() shl 32)
            if (scroll != 0L) putLong("scroll", scroll) else remove("scroll")
        }
        if (values.isEmpty()) values.add(page) else values[current] = page
        val first = maxOf(0, current - 6)
        val end = minOf(values.size, current + 4)
        return encode(Bundle().apply {
            putString("TITLE", title); putString("URL", url)
            putParcelableArray("LIST", values.subList(first, end).toTypedArray())
            putInt("CUR", current - first)
        })
    }

    private fun readBundle(bytes: ByteArray?): Bundle? {
        if (bytes == null || bytes.isEmpty()) return null
        val parcel = Parcel.obtain()
        return try {
            parcel.unmarshall(bytes, 0, bytes.size); parcel.setDataPosition(0)
            parcel.readBundle(ClassLoader.getSystemClassLoader())?.also { it.size() }
        } catch (_: RuntimeException) { null }
        finally { parcel.recycle() }
    }

    private fun encode(bundle: Bundle): ByteArray {
        val parcel = Parcel.obtain()
        return try { parcel.writeBundle(bundle); parcel.marshall() } finally { parcel.recycle() }
    }

    @Suppress("DEPRECATION")
    private fun pages(bundle: Bundle): Pages {
        val raw = bundle.getParcelableArray("LIST").orEmpty()
        val requested = bundle.getInt("CUR", -1)
        val values = mutableListOf<Bundle>()
        var current = requested
        raw.forEachIndexed { index, value ->
            if (index == requested) current = values.size
            if (value is Bundle) values.add(value)
        }
        return Pages(values, if (values.isEmpty()) 0 else current.coerceIn(values.indices))
    }
}
