package moe.pure.browser.ui.components

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import android.view.View
import moe.pure.browser.feature.settings.skin.NativeSkinIcons
import java.util.WeakHashMap

/** Views retain their sizing/tint; drawable lookup uses the shared native process cache. */
class SkinIconBinder(private val context: Context) : AutoCloseable {
    private data class Binding(val resource: Int, val size: Int? = null, val tint: Int? = null)
    private val bindings = WeakHashMap<View, Binding>()
    private val provider = NativeSkinIcons(context)
    fun image(view: ImageView, resource: Int) {
        bindings[view] = Binding(resource)
        apply(view, bindings.getValue(view))
    }
    fun top(view: TextView, resource: Int, size: Int, tint: Int) {
        bindings[view] = Binding(resource, size, tint)
        apply(view, bindings.getValue(view))
    }
    fun refresh() { bindings.toMap().forEach { (view, binding) -> apply(view, binding) } }
    private fun apply(view: View, binding: Binding) {
        val drawable = (provider.resolve(binding.resource) ?: context.getDrawable(binding.resource))?.mutate() ?: return
        when (view) {
            is ImageView -> view.setImageDrawable(drawable)
            is TextView -> {
                binding.tint?.let(drawable::setTint)
                val size = binding.size ?: 0
                drawable.setBounds(0, 0, size, size)
                view.setCompoundDrawables(null, drawable, null, null)
            }
        }
    }
    override fun close() { bindings.clear() }
}
