package moe.via.browser.feature.download.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.feature.download.DownloadCategory
import moe.via.browser.feature.library.ui.NativeLibrarySearch
import moe.via.browser.ui.components.DividerLayout
import moe.via.browser.ui.components.SkinIconBinder
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

/** o8.h/z + b1.X2: a complete page, with no browser panel or browser navigation ancestor. */
class DownloadManagerPage(private val activity: Activity, private val state: DownloadPanelState,
    private val actions: Actions, applyInsets: Boolean = true) : AutoCloseable {
    interface Actions {
        fun back()
        fun create()
        fun queryChanged(raw: String)
        fun categoryChanged(category: DownloadCategory)
        fun toggleEditing()
        fun selectAll()
        fun removeSelected()
    }

    private val main = Handler(Looper.getMainLooper())
    private val icons = SkinIconBinder(activity)
    private var closed = false
    private var pendingQuery = state.query
    private val query = Runnable { if (!closed) actions.queryChanged(pendingQuery) }
    val root = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(activity.palette.canvas)
        isFocusableInTouchMode = true
        if (applyInsets && Build.VERSION.SDK_INT >= 30) setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val ime = insets.getInsets(WindowInsets.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            insets
        }
    }
    private val search = NativeTypography.edit(activity).apply {
        id = R.id.download_manager_search
        layoutParams = LinearLayout.LayoutParams(-1, -2)
        setText(state.query)
    }
    private val searchStyle = NativeLibrarySearch(search)
    private val categories = DownloadCategoryStrip(activity, state.category) { category ->
        // b1's explicit category action reads the current field, independently of the debounce.
        state.query = pendingQuery
        actions.categoryChanged(category)
    }
    val list = RecyclerView(activity).apply { id = android.R.id.list }
    private val empty = NativeTypography.text(activity).apply {
        text = "¯\\_(ツ)_/¯"; textSize = 22f
        gravity = Gravity.CENTER; setTextColor(activity.palette.secondary)
        visibility = View.GONE
    }
    private val footer = DownloadFooterView(activity, state, actions)
    private val watcher = object : TextWatcher {
        override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) {
            pendingQuery = value?.toString().orEmpty()
            main.removeCallbacks(query); main.postDelayed(query, 100L)
        }
        override fun afterTextChanged(value: Editable?) = Unit
    }

    init {
        root.addView(toolbar(), LinearLayout.LayoutParams(-1, activity.dp(54)))
        search.hint = activity.getString(R.string.ref_download_ui_search)
        (search.layoutParams as LinearLayout.LayoutParams).apply {
            setMargins(activity.dp(14), activity.dp(10), activity.dp(14), activity.dp(4))
        }
        root.addView(search)
        root.addView(categories, LinearLayout.LayoutParams(-1, activity.dp(36)))
        root.addView(FrameLayout(activity).apply {
            addView(list, FrameLayout.LayoutParams(-1, -1))
            addView(empty, FrameLayout.LayoutParams(-1, -1))
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(footer, LinearLayout.LayoutParams(-1, -2))
        search.addTextChangedListener(watcher)
        root.requestFocus()
        root.post { if (!closed) root.requestApplyInsets() }
    }

    private fun toolbar() = DividerLayout(activity, atBottom = true).apply {
        gravity = Gravity.CENTER_VERTICAL
        addView(ImageView(activity).apply {
            imageTintList = ColorStateList.valueOf(activity.palette.icon)
            setPadding(dp(13), dp(13), dp(13), dp(13))
            contentDescription = activity.getString(R.string.ref_download_ui_up)
            background = activity.touchFeedback()
            icons.image(this, R.drawable.ref_ic_back)
            setOnClickListener { actions.back() }
        }, LinearLayout.LayoutParams(activity.dp(48), -1).apply {
            val edge = activity.resources.displayMetrics.density.toInt(); setMargins(edge, edge, edge, edge)
        })
        addView(NativeTypography.text(activity).apply {
            setText(R.string.ref_download_ui_title); textSize = 16f
            typeface = Typeface.create(typeface, Typeface.BOLD)
            setTextColor(activity.palette.text); gravity = Gravity.CENTER_VERTICAL
            setSingleLine(true); textDirection = View.TEXT_DIRECTION_LOCALE
        }, LinearLayout.LayoutParams(0, -2, 24f))
        addView(Space(activity), LinearLayout.LayoutParams(0, activity.dp(54), 1f))
    }

    fun render(visibleCount: Int) {
        if (closed) return
        val noRows = visibleCount == 0
        empty.visibility = if (noRows) View.VISIBLE else View.GONE
        list.visibility = if (noRows) View.GONE else View.VISIBLE
        categories.render(state.category, noRows)
        footer.render(visibleCount)
    }

    override fun close() {
        if (closed) return
        closed = true
        main.removeCallbacks(query)
        search.removeTextChangedListener(watcher)
        searchStyle.close(); icons.close()
    }
}
