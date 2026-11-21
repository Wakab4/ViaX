package moe.pure.browser.feature.library.dialog

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.ui.components.SkinIconBinder
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.touchFeedback
import moe.pure.browser.ui.theme.palette
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.feature.library.ui.recycler.NativeRecyclerEffects
import moe.pure.browser.feature.library.ui.recycler.RecyclerScrollState
import moe.pure.browser.ui.components.recycler.NativeRecyclerEdgeEffect

/** The reference layout/d.xml: 6 dp top inset, 48 dp header, list and full-height empty view. */
internal class BookmarksDialogView(private val activity: Activity, onManager: () -> Unit) : AutoCloseable {
    private val icons = SkinIconBinder(activity)
    val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(6), 0, 0) }
    val title = NativeTypography.text(activity).apply {
        id = R.id.ref_library_dialog_title
        text = "书签"; textSize = 16f; NativeTypography.style(this, android.graphics.Typeface.BOLD)
        setTextColor(activity.palette.text); gravity = Gravity.CENTER_VERTICAL
        setSingleLine(); ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(0, dp(8), 0, dp(8))
    }
    val list = RecyclerView(activity).apply {
        id = android.R.id.list
        NativeRecyclerEffects.install(this, topDivider = true)
    }
    private val empty = NativeTypography.text(activity).apply {
        text = "¯\\_(ツ)_/¯"; contentDescription = "空空如也…"; textSize = 14f
        setTextColor(activity.palette.secondary); gravity = Gravity.CENTER
        visibility = View.GONE
    }
    init {
        val header = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(title, LinearLayout.LayoutParams(0, -1, 1f).apply { marginStart = activity.dp(16); marginEnd = activity.dp(16) })
        header.addView(ImageView(activity).apply {
            id = R.id.ref_library_dialog_manager
            icons.image(this, R.drawable.ref_ic_open_external)
            imageTintList = android.content.res.ColorStateList.valueOf(activity.palette.icon)
            contentDescription = "外部打开"; setPadding(dp(14), dp(14), dp(14), dp(14))
            background = activity.touchFeedback(); setOnClickListener { onManager() }
        }, LinearLayout.LayoutParams(activity.dp(48), -1).apply { marginStart = activity.dp(2); marginEnd = activity.dp(2) })
        root.addView(header, LinearLayout.LayoutParams(-1, activity.dp(48)))
        root.addView(FrameLayout(activity).apply {
            addView(list, FrameLayout.LayoutParams(-1, -1)); addView(empty, FrameLayout.LayoutParams(-1, -1))
        }, LinearLayout.LayoutParams(-1, 0, 1f))
    }
    fun showEmpty(value: Boolean) { list.visibility = if (value) View.GONE else View.VISIBLE; empty.visibility = if (value) View.VISIBLE else View.GONE }
    fun scroll() = RecyclerScrollState.capture(list).let { BookmarkDialogState.Scroll(it.position, it.offset) }
    override fun close() { list.stopScroll(); list.itemAnimator?.endAnimations(); (list.edgeEffectFactory as? NativeRecyclerEdgeEffect)?.close(); list.adapter = null; icons.close() }
}
