package moe.via.browser.feature.home.ui.favorites

import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.feature.home.data.HomeFavoritesRepository
import moe.via.browser.feature.home.data.HomeIconStore
import moe.via.browser.feature.home.data.HomePreferences
import moe.via.browser.feature.home.data.NativeHomeFavorites
import moe.via.browser.feature.home.model.HomeFavorite
import moe.via.browser.feature.home.model.HomepageConfig
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.via.browser.ui.message.MessageContent
import moe.via.browser.ui.message.MessageOwner
import moe.via.browser.ui.tasks.NativeViewJobs
import moe.via.browser.ui.theme.palette

/** qa.v0 keeps adapter mutations local: deletion and undo never reload/reindex the whole grid. */
class HomeFavoritesGridFragment : Fragment() {
    private val state get() = ViewModelProvider(requireParentFragment())[HomeFavoritesPanelState::class.java]
    private val items = ArrayList<HomeFavorite>()
    private lateinit var repository: HomeFavoritesRepository
    private lateinit var config: HomepageConfig
    private var jobs: NativeViewJobs? = null
    private var grid: RecyclerView? = null
    private var adapter: HomeFavoriteGridAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = NativeHomeFavorites.get(requireContext())
        config = HomePreferences(requireContext()).config()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        return RelativeLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(-1, -1)
            val title = NativeTypography.text(context).apply {
                id = View.generateViewId(); setText(R.string.ref_favorite_manage)
                gravity = Gravity.CENTER; textSize = 16f
                setTextColor(context.palette.text); NativeTypography.style(this, Typeface.BOLD)
                setBackgroundResource(R.drawable.ref_favorite_feedback)
                ellipsize = android.text.TextUtils.TruncateAt.END; setSingleLine(true)
                isHorizontalFadingEdgeEnabled = false
            }
            addView(title, RelativeLayout.LayoutParams(-2, context.dp(48)).apply { addRule(RelativeLayout.CENTER_HORIZONTAL) })
            addView(NativeTypography.text(context).apply {
                setText(R.string.ref_favorite_done); gravity = Gravity.CENTER
                setTextColor(context.palette.accent); textSize = 14f
                setPadding(context.dp(16), 0, context.dp(16), 0)
                setBackgroundResource(R.drawable.ref_favorite_feedback); setOnClickListener { state.finish(true) }
                ellipsize = android.text.TextUtils.TruncateAt.END; setSingleLine(true)
                isHorizontalFadingEdgeEnabled = false
            }, RelativeLayout.LayoutParams(-2, -2).apply {
                addRule(RelativeLayout.ALIGN_TOP, title.id); addRule(RelativeLayout.ALIGN_BOTTOM, title.id)
                addRule(RelativeLayout.ALIGN_PARENT_END)
            })
            addView(RecyclerView(context).also { grid = it }, RelativeLayout.LayoutParams(-1, -2).apply {
                addRule(RelativeLayout.BELOW, title.id)
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = checkNotNull(grid)
        val rows = HomeFavoriteGridAdapter(items, config, HomeIconStore(requireContext()),
            edit = { index -> if (index in items.indices) state.edit(items[index]) }, remove = ::remove)
        adapter = rows
        list.layoutManager = GridLayoutManager(requireContext(), 4)
        geometry(Resources.getSystem().configuration)
        list.itemAnimator = DefaultItemAnimator().apply { moveDuration = 100 }
        list.adapter = rows
        NativeRecyclerEdgeEffect.install(list)
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(51, 0) {
            private var dragging = false
            private var from = 0
            private var to = 0
            override fun onSelectedChanged(holder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(holder, actionState)
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE && holder != null) {
                    dragging = actionState == ItemTouchHelper.ACTION_STATE_DRAG
                    from = holder.bindingAdapterPosition; to = from
                }
                if (dragging && actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    dragging = false
                    if (from != to) {
                        HomePreferences(requireContext()).markDirty(html = true, style = false)
                        BrowserExecutors.io.execute { repository.move(from, to) }
                    }
                }
            }
            override fun onMove(recycler: RecyclerView, holder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                rows.notifyItemMoved(holder.bindingAdapterPosition, target.bindingAdapterPosition)
                return true
            }
            override fun onMoved(recycler: RecyclerView, holder: RecyclerView.ViewHolder, fromPos: Int,
                target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int) {
                super.onMoved(recycler, holder, fromPos, target, toPos, x, y)
                items.add(toPos, items.removeAt(fromPos)); to = toPos
            }
            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) { remove(holder.bindingAdapterPosition) }
        }).attachToRecyclerView(list)
        state.saved.observe(viewLifecycleOwner) { item ->
            val index = items.indexOfLast { it.id == item.id }
            if (index >= 0) { items[index] = item; rows.notifyItemChanged(index) }
        }
        jobs = NativeViewJobs().also { owner -> owner.launch({ repository.all() }, failed = { NativeLog.error(null, it) }) { values ->
            items.clear(); items.addAll(values); rows.notifyDataSetChanged()
        } }
    }

    private fun remove(index: Int) {
        if (index !in items.indices) return
        val item = items.removeAt(index)
        val deleted = HomeFavorite(url = item.url, title = item.title, folder = item.folder,
            position = item.position, updatedAt = item.updatedAt, createdAt = item.createdAt)
        adapter?.notifyItemRemoved(index)
        repository.delete(item.id)
        HomePreferences(requireContext()).markDirty(html = true, style = false)
        val activity = requireActivity()
        MessageOwner(activity).show(MessageContent(
            activity.getString(R.string.ref_favorite_deleted, item.title), activity.getString(R.string.ref_favorite_undo),
            clicked = {
                val result = repository.save(deleted)
                if (result > 0 && !isDetached) {
                    deleted.id = result; items.add(index, deleted); adapter?.notifyItemInserted(index)
                }
            }
        ))
    }

    private fun geometry(configuration: Configuration) {
        val list = grid ?: return
        val context = requireContext()
        val geometry = NativeFavoriteGeometry.grid(context, context.dp(configuration.screenWidthDp),
            context.dp(config.favoriteItemWidth), config.searchBarEnabled, state.passedCount)
        list.setPadding(geometry.padding, 0, geometry.padding, 0)
        (list.layoutManager as GridLayoutManager).spanCount = geometry.count
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        state.passedCount = 0; geometry(newConfig)
    }
    override fun onDestroyView() {
        (grid?.edgeEffectFactory as? NativeRecyclerEdgeEffect)?.close()
        jobs?.close(); jobs = null; grid?.adapter = null; adapter = null; grid = null
        super.onDestroyView()
    }
}
