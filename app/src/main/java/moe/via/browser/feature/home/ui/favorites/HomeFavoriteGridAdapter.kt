package moe.via.browser.feature.home.ui.favorites

import android.graphics.Color
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.feature.home.data.HomeFavoritesRepository
import moe.via.browser.feature.home.data.HomeIconStore
import moe.via.browser.feature.home.model.HomeFavorite
import moe.via.browser.feature.home.model.HomepageConfig
import moe.via.browser.ui.components.dp

/** qa.j0/y5.f bind the current row position; ItemTouchHelper owns the drag gesture. */
internal class HomeFavoriteGridAdapter(
    private val items: List<HomeFavorite>,
    private val config: HomepageConfig,
    private val icons: HomeIconStore,
    private val edit: (Int) -> Unit,
    private val remove: (Int) -> Unit
) : RecyclerView.Adapter<HomeFavoriteGridAdapter.Holder>() {
    class Holder(val tile: HomeFavoriteTileView) : RecyclerView.ViewHolder(tile)

    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(HomeFavoriteTileView(parent.context).apply { layoutParams = RecyclerView.LayoutParams(-1, -2) }).also { holder ->
            holder.tile.setOnClickListener { edit(holder.bindingAdapterPosition) }
            holder.tile.removeClick = { remove(holder.bindingAdapterPosition) }
        }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val context = holder.tile.context
        val bitmap = if (config.favoriteIconDisabled) null else icons.load(item.url,
            context.dp(config.favoriteItemWidth), context.dp(config.favoriteItemHeight), context.dp(config.favoriteItemRadius))
        holder.tile.bind(item, config, bitmap, Color.parseColor(HomeFavoritesRepository.color(item.url)))
    }
}
