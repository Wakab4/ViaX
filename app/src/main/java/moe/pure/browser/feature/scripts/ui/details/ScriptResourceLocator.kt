package moe.pure.browser.feature.scripts.ui.details

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.PathInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.ui.components.dp
import java.lang.ref.WeakReference

/** g6.y.W/a/c/X: defer layout, use the literal 20px offset, then shake the target row. */
internal object ScriptResourceLocator {
    fun scroll(list: RecyclerView, position: Int) {
        val reference = WeakReference(list)
        list.post {
            val current = reference.get() ?: return@post
            val layout = current.layoutManager
            if (layout is LinearLayoutManager) layout.scrollToPositionWithOffset(position, 20)
            else current.scrollToPosition(position)
            current.post highlight@ {
                val row = reference.get()?.findViewHolderForAdapterPosition(position)?.itemView ?: return@highlight
                ObjectAnimator.ofFloat(row, "translationX", 0f, row.dp(24).toFloat(), 0f).apply {
                    repeatCount = 2; repeatMode = ValueAnimator.RESTART; duration = 280L
                    interpolator = PathInterpolator(.2f, .2f, .8f, .8f); start()
                }
            }
        }
    }
}
