package moe.pure.browser.feature.menu

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.pure.browser.feature.menu.ui.MenuGridAdapter
import moe.pure.browser.feature.menu.ui.NativeMenuSnap
import moe.pure.browser.ui.components.SkinIconBinder
import kotlin.math.ceil

/** i8.k: column-major adapter positions keep five visual columns on every orientation. */
internal class MenuPager(context: Context, entries: List<MenuEntry?>, icons: SkinIconBinder,
    clicked: (MenuEntry, Boolean) -> Unit, private val onPage: (Int) -> Unit,
    private val advanced: () -> Unit) : RecyclerView(context), AutoCloseable {
    private val rows = if (entries.size > 5) 2 else 1
    private val pageSize = rows * 5
    private val cells = buildList {
        repeat((entries.size + pageSize - 1) / pageSize) { page ->
            repeat(5) { column -> repeat(rows) { row -> add(entries.getOrNull(page * pageSize + column + row * 5)) } }
        }
    }
    private val spring = NativeRecyclerEdgeEffect()
    private val snap = NativeMenuSnap { position ->
        page = ceil(position.toDouble() / pageSize).toInt(); onPage(page)
    }
    var page = 0
        private set
    val pages get() = (entriesCount + pageSize - 1) / pageSize
    private val entriesCount = entries.size
    private var closed = false

    init {
        layoutManager = GridLayoutManager(context, rows, HORIZONTAL, false)
        itemAnimator = null; setHasFixedSize(true)
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS; edgeEffectFactory = spring
        isHorizontalScrollBarEnabled = false; isVerticalScrollBarEnabled = false
        adapter = MenuGridAdapter(cells, icons, clicked)
        addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            val remainder = view.width % 5
            if (view.paddingEnd != remainder) view.setPaddingRelative(view.paddingStart, view.paddingTop, remainder, view.paddingBottom)
        }
        if (pages > 1) snap.attachToRecyclerView(this)
    }

    fun nextPage() {
        if (closed || cells.size <= pageSize) return
        val last = (layoutManager as? GridLayoutManager)?.findLastCompletelyVisibleItemPosition() ?: NO_POSITION
        if (last == NO_POSITION) return
        var target = last + pageSize
        if (target >= cells.size) { target = 0; page = 0 } else page++
        scrollToPosition(target); onPage(page); advanced()
    }

    fun readerState(mode: Int) {
        if (closed) return
        val position = cells.indexOfFirst { it?.action == MenuAction.READER }
        val entry = cells.getOrNull(position) ?: return
        if (entry.enabled) { entry.selected = mode == 3; adapter?.notifyItemChanged(position) }
    }

    override fun close() {
        if (closed) return
        closed = true; stopScroll(); snap.attachToRecyclerView(null); spring.close(); adapter = null
    }
}
