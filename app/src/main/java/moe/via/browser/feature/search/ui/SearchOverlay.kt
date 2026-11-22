package moe.via.browser.feature.search.ui

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import moe.via.browser.feature.search.model.SearchEngine
import moe.via.browser.feature.search.model.SearchSuggestion
import moe.via.browser.feature.search.model.SearchSuggestionType
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.theme.palette

/** Address UI only. Suggestions, saved data, URL interpretation and navigation belong to the controller. */
class SearchOverlay(
    private val activity: Activity,
    private val parent: () -> FrameLayout,
    private val callbacks: Callbacks
) {
    data class InputState(
        val url: String,
        val originalDisplay: String,
        val explicitInput: String? = null,
        val incognito: Boolean = false,
        val top: Boolean = true,
        val rounded: Boolean = false,
        val backgroundColor: Int? = null,
        val temporaryEngine: SearchEngine? = null,
        val engines: List<SearchEngine> = emptyList()
    )

    interface Callbacks {
        fun onInputChanged(text: String)
        fun onSubmit(text: String, temporaryEngineId: Int?)
        fun onSuggestion(item: SearchSuggestion, temporaryEngineId: Int?)
        fun onChooseEngine(anchor: View)
        fun onClearSearchHistory()
        fun onEngineCleared()
        fun onClosed()
    }

    private data class Buffer(val text: String, val start: Int, val end: Int, val scroll: Int = 0)
    private var state: InputState? = null
    private var host: FrameLayout? = null
    private var layer: RelativeLayout? = null
    private var edgeSurface: View? = null
    private var bar: SearchInputBar? = null
    private var list: SearchSuggestionList? = null
    private var adapter: SearchSuggestionAdapter? = null
    private var expanded: SearchExpandedInput? = null
    private var suggestions: List<SearchSuggestion> = emptyList()
    private var engine: SearchEngine? = null
    private var initialText = ""
    private var forceSubmit = false
    private var disposed = false
    private val dialogs = DialogScope()
    private val layoutChanged = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateGeometry() }

    val isShowing get() = layer != null
    val text get() = bar?.input?.text?.toString().orEmpty()
    val temporaryEngineId get() = engine?.id?.takeIf { it != 0 }

    fun show(input: InputState) {
        if (disposed) return
        if (isShowing) detach()
        state = input
        initialText = input.explicitInput?.takeIf(String::isNotEmpty) ?: input.originalDisplay
        forceSubmit = !input.explicitInput.isNullOrEmpty()
        val draft = SearchDraftCache.read(initialText)
        engine = draft?.engineId?.let { id -> input.engines.firstOrNull { it.id == id } } ?: input.temporaryEngine
        suggestions = emptyList()
        build(Buffer(draft?.text ?: initialText, 0, (draft?.text ?: initialText).length), null, selectAll = true)
    }

    fun setInputText(text: String, selectAll: Boolean = false) {
        bar?.setText(text, selectAll)
    }

    fun setEngineBadge(engine: SearchEngine?) {
        this.engine = engine?.takeIf { it.id != 0 }
        bar?.setEngine(this.engine)
    }

    fun setSuggestions(items: List<SearchSuggestion>) {
        suggestions = items.toList()
        adapter?.replace(suggestions)
        updateSurface()
        val target = list
        target?.post {
            if (list !== target) return@post
            if (suggestions.isNotEmpty()) target.setSelection(if (state?.top == true) 0 else suggestions.lastIndex)
        }
    }

    fun markGlobalEngineChanged() {
        forceSubmit = true
        if (isShowing) callbacks.onInputChanged(text)
    }

    /** Recreates views after the browser replaces its host, retaining input and the open expanded editor. */
    fun rebind(state: InputState? = null) {
        val current = bar?.input ?: return
        val buffer = Buffer(current.text.toString(), current.selectionStart, current.selectionEnd)
        val large = expanded?.let { Buffer(it.input.text.toString(), it.input.selectionStart,
            it.input.selectionEnd, it.scroll.scrollY) }
        detach()
        if (state != null) this.state = state
        build(buffer, large)
        dialogs.updateGeometry()
    }

    fun close(saveDraft: Boolean = true) {
        if (!isShowing) return
        if (saveDraft) SearchDraftCache.save(initialText, text, temporaryEngineId) else SearchDraftCache.clear()
        hideKeyboard()
        detach(animate = !disposed)
        state = null
        suggestions = emptyList()
        engine = null
        callbacks.onClosed()
    }

    fun back(): Boolean {
        if (!isShowing) return false
        if (expanded != null) {
            removeExpanded()
            focusInput()
        } else close()
        return true
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        close()
        dialogs.dispose()
    }

    private fun build(buffer: Buffer, large: Buffer?, selectAll: Boolean = false) {
        val input = state ?: return
        val target = parent()
        val root = RelativeLayout(activity).apply {
            clipToPadding = false
            setOnClickListener { close() }
            contentDescription = "取消"
        }
        layer = root
        host = target
        val safeArea = View(activity).apply {
            setBackgroundColor(input.backgroundColor ?: activity.palette.panel)
        }
        edgeSurface = safeArea
        root.addView(safeArea, RelativeLayout.LayoutParams(-1, 0).apply {
            addRule(if (input.top) RelativeLayout.ALIGN_PARENT_TOP else RelativeLayout.ALIGN_PARENT_BOTTOM)
        })
        val inputBar = SearchInputBar(activity, input.incognito,
            changed = { if (layer === root) callbacks.onInputChanged(it) },
            chooseEngine = { anchor -> hideKeyboard(); callbacks.onChooseEngine(anchor) },
            clear = { setSuggestions(emptyList()); bar?.setText("") },
            expand = ::openExpanded,
            submit = ::submit,
            clearEngine = ::clearEngine
        ).apply {
            isClickable = true
            setText(buffer.text, selectAll, notify = false)
            setEngine(engine)
            applyColors(input.backgroundColor)
        }
        bar = inputBar
        val rowAdapter = SearchSuggestionAdapter(activity, input.top) { fill(it) }
        adapter = rowAdapter
        val rows = SearchSuggestionList(activity).apply {
            adapter = rowAdapter
            divider = null
            dividerHeight = 0
            isStackFromBottom = !input.top
            isVerticalScrollBarEnabled = true
            scrollBarSize = dp(3)
            setFadingEdgeLength(dp(32))
            isVerticalFadingEdgeEnabled = true
            clipToOutline = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            setOnItemClickListener { _, _, index, _ -> suggestion(rowAdapter.getItem(index)) }
            setOnItemLongClickListener { _, _, index, _ ->
                if (rowAdapter.getItem(index).type != SearchSuggestionType.SEARCH_HISTORY) false
                else { confirmClearHistory(); true }
            }
        }
        list = rows
        root.addView(inputBar, RelativeLayout.LayoutParams(-1, activity.dp(48)).apply {
            addRule(if (input.top) RelativeLayout.ALIGN_PARENT_TOP else RelativeLayout.ALIGN_PARENT_BOTTOM)
        })
        root.addView(rows, 0, RelativeLayout.LayoutParams(-1, -2).apply {
            addRule(if (input.top) RelativeLayout.BELOW else RelativeLayout.ABOVE, inputBar.id)
            if (input.top) bottomMargin = activity.dp(72) else topMargin = activity.dp(72)
        })
        root.setOnApplyWindowInsetsListener { view, insets ->
            SearchOverlayInsets.apply(activity, view, target, insets)
            view.post { updateGeometry() }
            insets
        }
        if (Build.VERSION.SDK_INT >= 30) root.setWindowInsetsAnimationCallback(object :
            WindowInsetsAnimation.Callback(WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
            override fun onProgress(insets: WindowInsets, runningAnimations: MutableList<WindowInsetsAnimation>): WindowInsets {
                SearchOverlayInsets.apply(activity, root, target, insets)
                return insets
            }
        })
        target.addView(root, FrameLayout.LayoutParams(-1, -1))
        if (selectAll) SearchTransitions.show(root)
        target.addOnLayoutChangeListener(layoutChanged)
        setSuggestions(suggestions)
        if (large != null) showExpanded(large, animate = false)
        root.requestApplyInsets()
        root.post {
            if (layer !== root || !root.isAttachedToWindow) return@post
            updateGeometry()
            if (expanded == null) {
                val editor = inputBar.input
                editor.requestFocus()
                if (selectAll) editor.selectAll()
                else editor.setSelection(buffer.start.coerceIn(0, editor.length()), buffer.end.coerceIn(0, editor.length()))
                showKeyboard(editor)
            } else expanded?.input?.let(::showKeyboard)
            callbacks.onInputChanged(text)
        }
    }

    private fun clearEngine() {
        if (engine == null) return
        setEngineBadge(null)
        callbacks.onEngineCleared()
        callbacks.onInputChanged(text)
    }

    private fun fill(item: SearchSuggestion) {
        val value = item.value.ifEmpty { item.url.ifEmpty { item.title } }
        if (value.isNotEmpty()) bar?.setText(value)
    }

    private fun suggestion(item: SearchSuggestion) {
        if (item.type == SearchSuggestionType.CALCULATOR) { fill(item); return }
        val temporary = temporaryEngineId
        val value = item.value.ifEmpty { item.url.ifEmpty { item.title } }
        val shouldNavigate = item.type == SearchSuggestionType.TAB || forceSubmit || value != initialText ||
            temporary != null && (item.type == SearchSuggestionType.ONLINE || item.type == SearchSuggestionType.SEARCH_HISTORY)
        close(saveDraft = false)
        if (shouldNavigate) callbacks.onSuggestion(item, temporary)
    }

    private fun submit() {
        val value = text
        val temporary = temporaryEngineId
        val shouldNavigate = value.isNotEmpty() && (temporary != null || forceSubmit || value != initialText)
        close(saveDraft = false)
        if (shouldNavigate) callbacks.onSubmit(value, temporary)
    }

    private fun confirmClearHistory() {
        dialogs.show(BrowserDialog(activity, "清除搜索历史", "你确认清除搜索历史吗？")
            .positive("确定") {
                callbacks.onClearSearchHistory()
                setSuggestions(emptyList())
                bar?.setText("")
                true
            }.negative())
    }

    private fun openExpanded() {
        val input = bar?.input ?: return
        if (expanded != null) return
        showExpanded(Buffer(input.text.toString(), input.selectionStart, input.selectionEnd))
    }

    private fun showExpanded(buffer: Buffer, animate: Boolean = true) {
        val root = layer ?: return
        val editor = SearchExpandedInput(activity, buffer.text, buffer.start, buffer.end) { value, start, end, go ->
            removeExpanded()
            bar?.setText(value)
            bar?.input?.apply {
                val selection = start.coerceIn(0, length())
                setSelection(selection, end.coerceIn(selection, length()))
            }
            if (go) submit() else focusInput()
        }
        expanded = editor
        root.addView(editor.view, RelativeLayout.LayoutParams(-1, -1))
        if (animate) SearchTransitions.show(editor.view)
        bar?.visibility = View.GONE
        list?.visibility = View.GONE
        editor.view.post {
            if (expanded !== editor || !editor.view.isAttachedToWindow) return@post
            editor.scroll.scrollTo(0, buffer.scroll)
            showKeyboard(editor.input)
        }
    }

    private fun removeExpanded() {
        expanded?.view?.let { SearchTransitions.remove(it, animate = true) }
        expanded = null
        bar?.visibility = View.VISIBLE
        updateSurface()
    }

    private fun focusInput() {
        val editor = bar?.input ?: return
        editor.post { if (bar?.input === editor && expanded == null) showKeyboard(editor) }
    }

    private fun updateSurface() {
        val config = state ?: return
        val radius = activity.dp(18).toFloat()
        val empty = suggestions.isEmpty()
        val topRadius = if (config.rounded && (config.top || empty)) radius else 0f
        val bottomRadius = if (config.rounded && (!config.top || empty)) radius else 0f
        bar?.background = GradientDrawable().apply {
            setColor(config.backgroundColor ?: activity.palette.panel)
            cornerRadii = floatArrayOf(topRadius, topRadius, topRadius, topRadius,
                bottomRadius, bottomRadius, bottomRadius, bottomRadius)
        }
        layer?.background = GradientDrawable().apply {
            setColor(0x40808080)
            val top = if (config.rounded && config.top) radius else 0f
            val bottom = if (config.rounded && !config.top) radius else 0f
            cornerRadii = floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
        }
        list?.apply {
            visibility = if (empty || expanded != null) View.GONE else View.VISIBLE
            background = GradientDrawable().apply {
                setColor(activity.palette.panel)
                val top = if (!config.top) radius else 0f
                val bottom = if (config.top) radius else 0f
                cornerRadii = floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
            }
        }
    }

    private fun updateGeometry() {
        val root = layer ?: return
        val target = host ?: return
        SearchOverlayInsets.apply(activity, root, target, root.rootWindowInsets)
        edgeSurface?.let { surface ->
            val size = if (state?.top == true) root.paddingTop else root.paddingBottom
            if (surface.layoutParams.height != size) {
                surface.layoutParams = surface.layoutParams.apply { height = size }
            }
            surface.translationY = if (state?.top == true) -size.toFloat() else size.toFloat()
        }
        val limit = (root.height - root.paddingTop - root.paddingBottom - activity.dp(120)).coerceAtLeast(0)
        list?.let { if (it.heightLimit != limit) { it.heightLimit = limit; it.requestLayout() } }
    }

    private fun showKeyboard(input: View) {
        if (input.requestFocus()) activity.getSystemService(InputMethodManager::class.java)
            ?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val input = expanded?.input ?: bar?.input
        activity.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(input?.windowToken, 0)
    }

    private fun detach(animate: Boolean = false) {
        host?.removeOnLayoutChangeListener(layoutChanged)
        layer?.let { SearchTransitions.remove(it, animate) }
        layer = null
        edgeSurface = null
        host = null
        bar = null
        list = null
        adapter = null
        expanded = null
    }
}
