package moe.pure.browser.feature.settings.skin

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableContainer
import android.util.LruCache
import java.io.File

/** lb.b resolves caller resources; lb.a keeps one process cache, keyed only by the icon name. */
internal class NativeSkinIcons(private val context: Context) {
    init { initialize(context) }

    fun resolve(resource: Int, key: String? = null): Drawable? {
        if (!key.isNullOrEmpty()) return store.resolve(key)
        val name = context.resources.getResourceEntryName(resource)
        return if (name.startsWith("ref_ic_")) store.resolve(name.removePrefix("ref_")) else null
    }

    companion object {
        private val store = Store()
        private var initialized = false

        private fun initialize(context: Context) {
            if (!initialized) adoptSelection(context)
        }

        /** Browser view creation adopts P1; changing a directory deliberately does not clear lb.a. */
        fun adoptSelection(context: Context) {
            initialized = true
            val name = context.getSharedPreferences("browser", Context.MODE_PRIVATE).getString("skin", null)
            if (!name.isNullOrEmpty()) {
                val root = context.getExternalFilesDir("skins") ?: File(context.filesDir, "skins")
                store.directory = if (root.exists() || root.mkdirs()) File(root, name).path else null
            }
        }
    }

    private class Store {
        var directory: String? = null
        private val cache = LruCache<String, Drawable.ConstantState>(32)

        fun resolve(name: String): Drawable? {
            val path = directory
            if (path.isNullOrEmpty() || !name.startsWith("ic_")) return null
            val drawable = cache.get(name)?.newDrawable() ?: NativeSkinBitmap.load(path + File.separator + name + ".png")
            drawable?.constantState?.let { cache.put(name, it) }
            return if (drawable is DrawableContainer) drawable.constantState?.newDrawable() ?: drawable else drawable
        }
    }
}
