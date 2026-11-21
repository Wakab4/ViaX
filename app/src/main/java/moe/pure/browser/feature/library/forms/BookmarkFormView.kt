package moe.pure.browser.feature.library.forms

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.content.res.ColorStateList
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.ui.components.SkinIconBinder
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.touchFeedback
import moe.pure.browser.ui.theme.palette

/** a8.g1.X2/d0.X2 share field layout, with their original bookmark/folder icon geometry. */
internal class BookmarkFormView(private val activity: Activity, val model: BookmarkFormModel,
    choose: (String) -> Unit, create: () -> Unit) : AutoCloseable {
    private val bookmark = model.request.kind == BookmarkFormKind.BOOKMARK
    private val icons = SkinIconBinder(activity)
    val root = RelativeLayout(activity).apply { setPadding(dp(16), dp(12), dp(16), 0) }
    val title = editor(R.string.ref_bookmark_form_title_hint, false)
    val address = editor(R.string.ref_bookmark_form_url_hint, true)
    val folder = NativeTypography.text(activity).apply {
        id = View.generateViewId(); hint = activity.getString(R.string.ref_bookmark_form_folder_hint)
        textSize = 14f; setTextColor(activity.palette.text); setHintTextColor(activity.palette.hint)
        setSingleLine(true); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(0, dp(10), 0, dp(10)); gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.ref_bookmark_form_folder); isFocusable = true
    }
    private val arrow = ImageView(activity).apply {
        id = View.generateViewId(); icons.image(this, R.drawable.ref_ic_next)
        imageTintList = ColorStateList.valueOf(activity.palette.icon)
        setPadding(dp(if (bookmark) 13 else 10), dp(if (bookmark) 0 else 10), dp(if (bookmark) 13 else 10), dp(if (bookmark) 0 else 10))
        background = activity.touchFeedback()
    }
    val favorite = CheckBox(activity).apply {
        id = View.generateViewId(); text = activity.getString(R.string.ref_bookmark_form_favorite)
        setButtonDrawable(R.drawable.dialog_checkbox); buttonTintList = null
        textSize = 12f; setTextColor(activity.palette.secondary); setPadding(dp(8), dp(8), dp(8), dp(8))
        visibility = if (bookmark && model.request.favoriteEnabled) View.VISIBLE else View.GONE
    }
    val list = RecyclerView(activity).apply {
        id = android.R.id.list; layoutManager = LinearLayoutManager(activity); itemAnimator = DefaultItemAnimator()
        isNestedScrollingEnabled = false; visibility = View.GONE; alpha = 0f
    }
    private val rows = BookmarkFolderAdapter(activity, true, choose, create)
    private var shownExpanded: Boolean? = null

    init {
        val titleBox = field(title)
        val urlBox = field(address)
        root.addView(titleBox, RelativeLayout.LayoutParams(-1, -2).apply { bottomMargin = activity.dp(12) })
        if (bookmark) root.addView(urlBox, RelativeLayout.LayoutParams(-1, -2).apply {
            addRule(RelativeLayout.BELOW, titleBox.id); bottomMargin = activity.dp(12)
        })
        val symbol = ImageView(activity).apply {
            id = View.generateViewId(); icons.image(this, R.drawable.ref_ic_folder)
            imageTintList = ColorStateList.valueOf(activity.palette.icon)
        }
        root.addView(symbol, RelativeLayout.LayoutParams(activity.dp(if (bookmark) 22 else 20), -2).apply {
            addRule(RelativeLayout.ALIGN_TOP, folder.id); addRule(RelativeLayout.ALIGN_BOTTOM, folder.id); marginEnd = activity.dp(16)
        })
        root.addView(folder, RelativeLayout.LayoutParams(-1, -2).apply {
            addRule(RelativeLayout.END_OF, symbol.id); addRule(RelativeLayout.START_OF, arrow.id)
            addRule(RelativeLayout.BELOW, if (bookmark) urlBox.id else titleBox.id); bottomMargin = activity.dp(12)
        })
        root.addView(arrow, RelativeLayout.LayoutParams(activity.dp(if (bookmark) 48 else 42), -2).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END); addRule(RelativeLayout.ALIGN_TOP, folder.id); addRule(RelativeLayout.ALIGN_BOTTOM, folder.id)
        })
        root.addView(favorite, RelativeLayout.LayoutParams(-1, -2).apply { addRule(RelativeLayout.BELOW, folder.id) })
        root.addView(list, RelativeLayout.LayoutParams(-1, -2).apply { addRule(RelativeLayout.BELOW, folder.id) })
        list.adapter = rows
        folder.setOnClickListener { model.expanded = !model.expanded; renderExpansion(true) }
        arrow.setOnClickListener { folder.performClick() }
        watch(title) { model.title = it }; watch(address) { model.url = it }
        favorite.setOnCheckedChangeListener { _, checked -> model.favorite = checked }
    }

    fun render() {
        if (title.text.toString() != model.title) title.setText(model.title)
        if (address.text.toString() != model.url) address.setText(model.url)
        favorite.isChecked = model.favorite
        folder.text = model.folders.firstOrNull { it.entry.id == model.selected }?.entry?.let {
            if (it.id.isEmpty()) activity.getString(R.string.ref_bookmark_form_root) else it.title
        }.orEmpty()
        rows.submit(model.folders, model.selected, bookmark || model.request.creationEnabled)
        renderExpansion(false)
    }
    private fun renderExpansion(animate: Boolean) {
        val expanded = model.expanded
        if (shownExpanded == expanded) return
        shownExpanded = expanded
        arrow.animate().cancel(); list.animate().cancel(); favorite.animate().cancel()
        if (!animate) {
            arrow.rotation = if (expanded) 180f else 0f
            list.visibility = if (expanded) View.VISIBLE else View.GONE; list.alpha = if (expanded) 1f else 0f
            favorite.visibility = if (!expanded && bookmark && model.request.favoriteEnabled) View.VISIBLE else View.GONE
            favorite.alpha = if (expanded) 0f else 1f
            return
        }
        val interpolator = AccelerateDecelerateInterpolator()
        arrow.animate().rotation(if (expanded) 180f else 0f).setDuration(140).setInterpolator(interpolator).start()
        if (expanded) list.visibility = View.VISIBLE
        list.animate().alpha(if (expanded) 1f else 0f).setDuration(140).setInterpolator(interpolator).withEndAction {
            if (!model.expanded) list.visibility = View.GONE
        }.start()
        if (bookmark && model.request.favoriteEnabled) {
            if (!expanded) favorite.visibility = View.VISIBLE
            favorite.animate().alpha(if (expanded) 0f else 1f).setDuration(140).setInterpolator(interpolator).withEndAction {
                if (model.expanded) favorite.visibility = View.GONE
            }.start()
        }
    }

    private fun editor(hint: Int, uri: Boolean) = NativeTypography.edit(activity).apply {
        id = View.generateViewId(); setHint(hint); textSize = 14f
        setTextColor(activity.palette.text); setHintTextColor(activity.palette.hint)
        setSingleLine(true); setHorizontallyScrolling(true); background = null; setPadding(0, 0, 0, 0)
        inputType = if (uri) InputType.TYPE_TEXT_VARIATION_URI else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        ellipsize = android.text.TextUtils.TruncateAt.END
        if (android.os.Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
        imeOptions = if (uri) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
        gravity = Gravity.CENTER_VERTICAL; textDirection = View.TEXT_DIRECTION_LOCALE
    }
    private fun field(input: EditText) = object : HorizontalScrollView(activity) {
        override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
            parent.requestDisallowInterceptTouchEvent(true); return super.dispatchTouchEvent(event)
        }
    }.apply {
        id = View.generateViewId(); isHorizontalScrollBarEnabled = false; isVerticalScrollBarEnabled = false; isFillViewport = true
        setPadding(0, dp(10), 0, dp(10)); setBackgroundResource(R.drawable.ref_bookmark_form_input)
        addView(input, FrameLayout.LayoutParams(-2, -2))
    }
    private fun watch(input: EditText, changed: (String) -> Unit) = input.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = changed(s?.toString().orEmpty())
        override fun afterTextChanged(s: Editable?) = Unit
    })
    override fun close() { arrow.animate().cancel(); list.animate().cancel(); favorite.animate().cancel(); list.itemAnimator?.endAnimations(); list.adapter = null; icons.close() }
}
