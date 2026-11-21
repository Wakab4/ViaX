package moe.pure.browser.feature.home.ui.customize

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.feature.home.ui.customize.palette.BackgroundPaletteAdapter
import moe.pure.browser.ui.components.NativeSeekBar
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.recycler.NativeLinearLayoutPagerManager
import kotlin.math.ceil

/** e9.k/layout r use real recycled five-column tools and an optional slider above them. */
internal class DesignToolPanel(context: Context, private val committed: (DesignTool, Int) -> Unit) : RelativeLayout(context) {
    private val toolsAdapter = DesignToolAdapter(context, ::activate)
    private val manager = NativeLinearLayoutPagerManager(context, RecyclerView.HORIZONTAL, 5)
    private val list = RecyclerView(context).apply {
        id = View.generateViewId(); setPadding(0, 0, 0, dp(12)); itemAnimator = null
        layoutManager = manager
        adapter = toolsAdapter
    }
    private val slider = NativeSeekBar(context).apply {
        visibility = View.GONE; setPadding(0, dp(9), 0, dp(12)); thumbOffset = 0
    }

    init {
        layoutParams = FrameLayout.LayoutParams(-1, -2)
        addView(slider, LayoutParams(-1, -2).apply {
            leftMargin = dp(24); rightMargin = dp(24); addRule(ABOVE, list.id)
        })
        addView(list, LayoutParams(-1, dp(96)).apply { addRule(ALIGN_PARENT_BOTTOM) })
    }

    fun focusTools() { list.requestFocus() }
    fun render(items: List<DesignTool>) {
        toolsAdapter.submit(items)
        manager.setCount(5)
        if (list.adapter !== toolsAdapter) list.adapter = toolsAdapter
    }

    fun updateTools(items: List<DesignTool>) { toolsAdapter.submit(items) }
    fun changed(position: Int) { toolsAdapter.changed(position) }
    fun bindBackgroundTouch() {
        list.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> view.parent!!.requestDisallowInterceptTouchEvent(view.canScrollHorizontally(-1))
                MotionEvent.ACTION_UP -> view.parent!!.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }
    fun showPalette(adapter: BackgroundPaletteAdapter, deferred: Boolean = false) {
        val show = Runnable {
            val columns = ceil((list.width / context.dp(56)).toDouble()).toInt()
            manager.setCount(maxOf(1, columns)); list.adapter = adapter
            list.scrollToPosition(maxOf(0, adapter.selectedPosition - columns / 2))
        }
        if (deferred) list.post(show) else show.run()
    }

    private fun activate(tool: DesignTool, position: Int) {
        if (tool is DesignTool.Number) {
            if (toolsAdapter.selectedPosition == position) {
                slider.visibility = View.GONE; toolsAdapter.selected(-1)
            } else {
                toolsAdapter.selected(position); showSlider(tool, position)
            }
        } else {
            if (toolsAdapter.selectedPosition >= 0) { slider.visibility = View.GONE; toolsAdapter.selected(-1) }
            if (tool is DesignTool.Toggle) { tool.value = !tool.value; toolsAdapter.changed(position) }
            committed(tool, position)
        }
    }

    private fun showSlider(tool: DesignTool.Number, position: Int) {
        slider.visibility = View.VISIBLE; slider.setOnSeekBarChangeListener(null)
        slider.max = tool.range.last - tool.range.first
        slider.progress = tool.value - tool.range.first
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                tool.value = progress + tool.range.first; toolsAdapter.changed(position)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) { committed(tool, position) }
        })
        slider.requestFocus()
    }
}
