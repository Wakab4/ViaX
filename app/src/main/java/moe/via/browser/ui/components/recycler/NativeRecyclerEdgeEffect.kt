package moe.via.browser.ui.components.recycler

import android.graphics.Canvas
import android.widget.EdgeEffect
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.EdgeEffectFactory.DIRECTION_LEFT
import androidx.recyclerview.widget.RecyclerView.EdgeEffectFactory.DIRECTION_RIGHT
import androidx.recyclerview.widget.RecyclerView.EdgeEffectFactory.DIRECTION_TOP
import androidx.recyclerview.widget.RecyclerView.EdgeEffectFactory.DIRECTION_BOTTOM

/** d6.a uses the AndroidX spring engine and scales the list around the pulled edge, without a glow. */
internal class NativeRecyclerEdgeEffect : RecyclerView.EdgeEffectFactory(), AutoCloseable {
    companion object {
        fun install(view: RecyclerView) {
            view.overScrollMode = android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS
            view.edgeEffectFactory = NativeRecyclerEdgeEffect()
        }
    }
    private val edges = mutableListOf<SpringEdge>()
    override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect = SpringEdge(view, direction).also(edges::add)
    override fun close() { edges.forEach(SpringEdge::dispose); edges.clear() }

    private class SpringEdge(private val view: RecyclerView, private val direction: Int) : EdgeEffect(view.context) {
        private val horizontal get() = direction == DIRECTION_LEFT || direction == DIRECTION_RIGHT
        private var animation: SpringAnimation? = null
        private var pulling = false
        private var scale: Float
            get() = if (horizontal) view.scaleX else view.scaleY
            set(value) { if (horizontal) view.scaleX = value.coerceIn(.5f, 1.8f) else view.scaleY = value.coerceIn(.5f, 1.8f) }
        private fun pivot() {
            if (horizontal) view.pivotX = if (direction == DIRECTION_LEFT) 0f else view.width.toFloat()
            else view.pivotY = if (direction == DIRECTION_TOP) 0f else view.height.toFloat()
        }
        private fun spring() = SpringAnimation(view, if (horizontal) DynamicAnimation.SCALE_X else DynamicAnimation.SCALE_Y).apply {
            spring = SpringForce(1f).setDampingRatio(.75f).setStiffness(200f)
        }
        private fun pull(delta: Float) {
            val current = scale
            if (!pulling && current == 1f) { pulling = true; pivot() }
            val currentEdge = if (horizontal) { if (view.pivotX == 0f) DIRECTION_LEFT else DIRECTION_RIGHT }
                else if (view.pivotY == 0f) DIRECTION_TOP else DIRECTION_BOTTOM
            val change = (if (currentEdge != direction) -(current - 1f) else 1.05f - current) / .05f * delta * .15f
            if (change != 0f) scale = current + change
            animation?.cancel()
        }
        override fun draw(canvas: Canvas) = false
        override fun isFinished() = animation?.isRunning != true
        override fun onPull(deltaDistance: Float) { super.onPull(deltaDistance); pull(deltaDistance) }
        override fun onPull(deltaDistance: Float, displacement: Float) { super.onPull(deltaDistance, displacement); pull(deltaDistance) }
        override fun onAbsorb(velocity: Int) {
            super.onAbsorb(velocity); pivot(); animation?.cancel()
            animation = spring().apply { setStartValue(1f + minOf(.05f, velocity / 20000f * .25f)); start() }
        }
        override fun onRelease() {
            super.onRelease(); pulling = false
            if (scale != 1f) animation = spring().apply { start() }
        }
        fun dispose() { animation?.cancel(); scale = 1f; pulling = false }
    }
}
