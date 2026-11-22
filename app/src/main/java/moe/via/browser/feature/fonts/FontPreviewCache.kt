package moe.via.browser.feature.fonts

import android.graphics.Typeface
import android.util.LruCache
import moe.via.browser.storage.font.FontEntry

/** hb.j2.u3/w3 use a page-local twelve-face cache and a separate failed-path set. */
internal class FontPreviewCache {
    private val faces = LruCache<String, Typeface>(12)
    private val failed = HashSet<String>()
    fun invalid(entry: FontEntry) = entry.name.isNotEmpty() && entry.path in failed
    fun invalidate(entry: FontEntry) { failed.remove(entry.path); faces.remove(entry.path) }
    fun typeface(entry: FontEntry): Typeface {
        if (entry.name.isEmpty() || entry.path in failed) return Typeface.DEFAULT
        faces[entry.path]?.let { return it }
        val face = try { Typeface.createFromFile(entry.path) } catch (error: Exception) { error.printStackTrace(); null }
        if (face == null) { failed.add(entry.path); return Typeface.DEFAULT }
        faces.put(entry.path, face)
        return face
    }
}
