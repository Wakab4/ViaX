package moe.pure.browser.feature.download

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.content.res.ColorStateList
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import moe.pure.browser.R
import moe.pure.browser.feature.download.ui.list.DownloadDateDecoration
import moe.pure.browser.feature.download.ui.list.DownloadRowIcons
import moe.pure.browser.feature.download.ui.list.DownloadRowStyle
import moe.pure.browser.feature.download.ui.list.DownloadRowText
import moe.pure.browser.feature.library.ui.recycler.NativeRecyclerEffects
import moe.pure.browser.ui.components.SkinIconBinder
import moe.pure.browser.ui.theme.palette

/** b1/y5 + DownloadTaskViewDelegate. Date sections do not consume adapter positions. */
class DownloadListAdapter(
    private val activity: Activity,
    private val state: DownloadPanelState,
    private val clicked: (DownloadTask) -> Unit,
    private val held: (DownloadTask, View) -> Unit,
    private val toggle: (DownloadTask) -> Unit
) : RecyclerView.Adapter<DownloadListAdapter.Holder>(), AutoCloseable {
    private data class Row(val task: DownloadTask, val editing: Boolean, val selected: Boolean)
    private var rows = emptyList<Row>()
    var visible: List<DownloadTask> = emptyList(); private set
    var beforeStructureChange: (() -> Unit)? = null
    private var list: RecyclerView? = null
    private var closed = false
    private val dates = DownloadDateDecoration(activity)
    private val controls = SkinIconBinder(activity)
    private val icons = DownloadRowIcons(activity, ::iconsChanged)

    init { setHasStableIds(true) }

    fun attach(list: RecyclerView) {
        if (closed || this.list === list) return
        detach()
        this.list = list
        NativeRecyclerEffects.install(list)
        (list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        list.addItemDecoration(dates)
        list.adapter = this
    }

    fun submit(tasks: List<DownloadTask>) {
        if (closed) return
        val query = state.query.trim { it <= ' ' }
        val filtered = tasks.filter {
            (state.category == DownloadCategory.ALL || DownloadCategory.of(it) == state.category) && it.name.contains(query)
        }
        if (filtered.isEmpty()) state.editing = false
        state.selected.retainAll(filtered.map { it.id }.toSet())
        val next = filtered.map { Row(it, state.editing, state.editing && it.id in state.selected) }
        val old = rows
        if (old.map { it.task.id } != next.map { it.task.id }) beforeStructureChange?.invoke()
        val difference = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = next.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = old[oldItemPosition].task.id == next[newItemPosition].task.id
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = old[oldItemPosition] == next[newItemPosition]
            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                val before = old[oldItemPosition]; val after = next[newItemPosition]
                return if (before.editing == after.editing && before.selected == after.selected && DownloadRowText.running(after.task) &&
                    before.task.copy(received = after.task.received, total = after.task.total,
                        speed = after.task.speed, updated = after.task.updated) == after.task) PROGRESS else null
            }
        })
        visible = filtered; rows = next
        dates.submit(filtered)
        difference.dispatchUpdatesTo(this)
        list?.invalidateItemDecorations()
    }

    fun selected(position: Int) = rows.getOrNull(position)?.selected == true

    /** n8.a updates one selection row without re-filtering or replacing adapter positions. */
    fun select(position: Int, selected: Boolean): Boolean {
        val row = rows.getOrNull(position) ?: return false
        if (closed || !state.editing || row.selected == selected) return false
        if (selected) state.selected.add(row.task.id) else state.selected.remove(row.task.id)
        rows = rows.toMutableList().apply { this[position] = row.copy(editing = true, selected = selected) }
        notifyItemChanged(position, SELECTION)
        return true
    }

    override fun getItemCount() = rows.size
    override fun getItemId(position: Int) = rows[position].task.id
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.download_row, parent, false).also { NativeTypography.tree(it) })

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        holder.bind(row.task, row.editing, row.selected)
    }
    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position)
        else {
            val row = rows[position]
            if (payloads.all { it == PROGRESS || it == ICONS }) {
                if (PROGRESS in payloads) holder.progress(row.task)
                if (ICONS in payloads) holder.icon(row.task)
            } else if (payloads.all { it == SELECTION }) {
                holder.selection(row.editing, row.selected)
            } else onBindViewHolder(holder, position)
        }
    }

    override fun onViewAttachedToWindow(holder: Holder) {
        super.onViewAttachedToWindow(holder)
        task(holder)?.let(holder::icon)
    }
    override fun onViewRecycled(holder: Holder) {
        holder.file.setImageDrawable(null)
        super.onViewRecycled(holder)
    }

    private fun task(holder: Holder): DownloadTask? = if (closed) null else rows.getOrNull(holder.bindingAdapterPosition)?.task

    private fun iconsChanged(id: Long?) {
        if (closed) return
        if (id == null) notifyItemRangeChanged(0, rows.size, ICONS)
        else rows.indexOfFirst { it.task.id == id }.takeIf { it >= 0 }?.let { notifyItemChanged(it, ICONS) }
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val file: ImageView = view.findViewById(R.id.download_icon)
        private val name: TextView = view.findViewById(R.id.download_name)
        private val detail: TextView = view.findViewById(R.id.download_detail)
        private val bar: ProgressBar = view.findViewById(R.id.download_progress)
        private val action: ImageView = view.findViewById(R.id.download_action)
        private val checked: CheckBox = view.findViewById(R.id.download_checked)
        private var actionAt = 0L

        init {
            view.background = DownloadRowStyle.pressed(activity)
            action.background = DownloadRowStyle.pressed(activity, round = true)
            bar.progressDrawable = DownloadRowStyle.progress(activity)
            view.setOnClickListener { task(this)?.let(clicked) }
            view.setOnLongClickListener { task(this)?.let { held(it, view); true } ?: false }
            action.setOnClickListener {
                val task = task(this) ?: return@setOnClickListener
                val now = SystemClock.elapsedRealtime()
                if (now - actionAt >= 500) { actionAt = now; toggle(task) }
            }
        }

        fun bind(task: DownloadTask, editing: Boolean, selected: Boolean) {
            name.text = task.name.ifEmpty { task.url }
            name.setTextColor(activity.palette.text)
            detail.setTextColor(activity.palette.secondary)
            icon(task); progress(task)
            val running = DownloadRowText.running(task)
            action.visibility = if (editing || task.state == DownloadState.COMPLETE) View.GONE else View.VISIBLE
            action.isEnabled = !editing
            controls.image(action, if (running) R.drawable.ref_ic_download_pause else R.drawable.ref_ic_download_resume)
            action.imageTintList = ColorStateList.valueOf(activity.palette.icon)
            action.contentDescription = activity.getString(if (running) R.string.ref_download_ui_pause else R.string.ref_download_ui_resume)
            selection(editing, selected)
        }

        fun selection(editing: Boolean, selected: Boolean) {
            checked.visibility = if (editing) View.VISIBLE else View.GONE
            checked.isChecked = selected
            itemView.isSelected = selected
            ViewCompat.setStateDescription(itemView, if (editing) activity.getString(
                if (selected) R.string.ref_download_selected else R.string.ref_download_not_selected) else null)
        }

        fun progress(task: DownloadTask) {
            detail.text = DownloadRowText.description(activity, task)
            bar.visibility = if (DownloadRowText.running(task)) View.VISIBLE else View.GONE
            bar.progress = if (task.total > 0) ((task.received * 1000) / task.total).toInt() else 1000
        }

        fun icon(task: DownloadTask) = icons.bind(file, task, itemView.isAttachedToWindow)
    }

    private fun detach() {
        list?.let {
            it.stopScroll(); it.itemAnimator?.endAnimations()
            it.removeItemDecoration(dates)
            (it.edgeEffectFactory as? AutoCloseable)?.close()
            if (it.adapter === this) it.adapter = null
        }
        list = null
    }

    override fun close() {
        if (closed) return
        closed = true; detach(); icons.close(); controls.close()
        beforeStructureChange = null
        rows = emptyList(); visible = emptyList()
    }

    companion object {
        private const val PROGRESS = 1
        private const val ICONS = 2
        private const val SELECTION = 3
    }
}
