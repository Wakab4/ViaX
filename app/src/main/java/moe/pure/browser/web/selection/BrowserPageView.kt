package moe.pure.browser.web.selection

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import moe.pure.browser.R
import org.json.JSONTokener
import java.lang.ref.WeakReference
import java.util.Locale

/** WebView selection actions. All methods and callbacks run on the view's UI thread. */
class BrowserPageView(context: Context, val pageId: Int = 0) : WebView(context) {
    private var forwardBudget = Int.MAX_VALUE
    var pageAccentColor = 0

    override fun saveState(outState: android.os.Bundle): android.webkit.WebBackForwardList? =
        super.saveState(outState)?.also { outState.putInt("COLOR", pageAccentColor) }

    override fun restoreState(inState: android.os.Bundle): android.webkit.WebBackForwardList? {
        pageAccentColor = inState.getInt("COLOR", pageAccentColor)
        return super.restoreState(inState)
    }

    /** t4.b.o/r: a suspended outer-page boundary is distinct from engine forward history. */
    fun markForwardBoundary() { forwardBudget = 0 }
    fun clearForwardBoundary() { forwardBudget = Int.MAX_VALUE }
    override fun canGoForward(): Boolean = forwardBudget > 0 && super.canGoForward()
    override fun goBack() {
        if (forwardBudget < Int.MAX_VALUE && canGoBack()) forwardBudget++
        super.goBack()
    }
    override fun goForward() {
        if (canGoForward()) forwardBudget--
        super.goForward()
    }
    interface Interaction {
        fun touch(source: WebView, event: android.view.MotionEvent): Boolean
        fun down(hasUrl: Boolean)
        fun overScrolled(clampedX: Boolean)
        fun released(source: WebView)
    }
    var interaction: Interaction? = null
    interface SelectionActions {
        fun search(text: String)
        fun find(text: String)
    }

    private var disposed = false
    private var currentMode: ActionMode? = null
    private var documentGeneration = 0L
    private var requestGeneration = 0L
    private var webSearchLocales: String? = null
    private var webSearchLabels: Set<String> = emptySet()

    var selectionActions: SelectionActions? = null
        set(value) {
            if (disposed) {
                field = null
            } else if (field !== value) {
                cancelSelection()
                field = value
            }
        }

    override fun startActionMode(callback: ActionMode.Callback): ActionMode? =
        prepareMode(super.startActionMode(wrap(callback)))

    override fun startActionMode(callback: ActionMode.Callback, type: Int): ActionMode? =
        prepareMode(super.startActionMode(wrap(callback), type))

    /** Call when a navigation starts, including a reload of the same URL. Keeps actions installed. */
    fun cancelSelection() {
        documentGeneration++
        requestGeneration++
        val previous = currentMode
        currentMode = null
        previous?.finish()
    }

    /** Call before releasing the WebView. Idempotent; destroy() also performs this cleanup. */
    fun disposeSelection() {
        if (disposed) return
        disposed = true
        cancelSelection()
        selectionActions = null
    }

    override fun onPause() {
        cancelSelection()
        super.onPause()
    }

    override fun onDetachedFromWindow() {
        cancelSelection()
        super.onDetachedFromWindow()
    }

    override fun destroy() {
        val currentInteraction = interaction
        interaction = null
        currentInteraction?.released(this)
        disposeSelection()
        super.destroy()
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.actionMasked == android.view.MotionEvent.ACTION_DOWN) interaction?.down(url != null)
        val consumed = interaction?.touch(this, event) == true
        return consumed || super.onTouchEvent(event)
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        interaction?.overScrolled(clampedX)
    }

    private fun wrap(callback: ActionMode.Callback): ActionMode.Callback =
        if (callback is SelectionCallback && callback.owner === this) callback else SelectionCallback(callback)

    private fun prepareMode(mode: ActionMode?): ActionMode? {
        if (mode == null) return null
        if (disposed) {
            mode.finish()
            return null
        }
        adoptMode(mode)
        rebuildMenu(mode, mode.menu)
        return mode
    }

    private fun adoptMode(mode: ActionMode) {
        if (currentMode === mode) return
        requestGeneration++
        val previous = currentMode
        currentMode = mode
        previous?.finish()
    }

    private fun rebuildMenu(mode: ActionMode, menu: Menu): Boolean {
        val hadActions = menu.findItem(SEARCH_ITEM_ID) != null || menu.findItem(FIND_ITEM_ID) != null
        menu.removeItem(SEARCH_ITEM_ID)
        menu.removeItem(FIND_ITEM_ID)
        if (disposed || selectionActions == null || !settings.javaScriptEnabled) return hadActions

        val labels = SelectionMenuText.forLocale(resources.configuration.locales.get(0))
        val hiddenLabels = localizedWebSearchLabels() + labels.nativeSearch +
            context.getString(R.string.ref_search_widget_entry_label)
        val copyLabel = context.getString(android.R.string.copy)
        val selectAllLabel = context.getString(android.R.string.selectAll)
        var hasCopy = false
        var order = 0
        var changed = hadActions
        for (index in menu.size() - 1 downTo 0) {
            val item = menu.getItem(index)
            val title = item.title?.toString()
            if (title.equals(selectAllLabel, ignoreCase = true)) {
                order = item.order
            } else if (title.equals(copyLabel, ignoreCase = true)) {
                hasCopy = true
            }
            val intent = item.intent
            val ownProcessText = intent?.action == Intent.ACTION_PROCESS_TEXT &&
                intent.component?.packageName == context.packageName
            if (hiddenLabels.any { title.equals(it, ignoreCase = true) } ||
                intent?.action == Intent.ACTION_WEB_SEARCH || ownProcessText
            ) {
                if (item.isVisible) changed = true
                item.isVisible = false
            }
        }
        // Native requires the localized Copy item, even if the platform currently hides it.
        if (hasCopy) {
            menu.add(0, SEARCH_ITEM_ID, order, labels.search).setOnMenuItemClickListener {
                readSelection(mode, search = true)
            }
            menu.add(0, FIND_ITEM_ID, order, labels.find).setOnMenuItemClickListener {
                readSelection(mode, search = false)
            }
            changed = true
        }
        if (changed) mode.invalidateContentRect()
        return changed
    }

    private fun localizedWebSearchLabels(): Set<String> {
        val locales = resources.configuration.locales.toLanguageTags()
        if (webSearchLocales == locales) return webSearchLabels
        val labels = linkedSetOf("Web search")
        runCatching {
            val provider = WebView.getCurrentWebViewPackage()?.packageName ?: return@runCatching
            val providerResources = context.createPackageContext(provider, 0)
                .createConfigurationContext(resources.configuration).resources
            val id = providerResources.getIdentifier("websearch", "string", provider)
            if (id != 0) labels.add(providerResources.getString(id))
        }
        runCatching {
            val id = resources.getIdentifier("websearch", "string", "android")
            if (id != 0) labels.add(resources.getString(id))
        }
        webSearchLocales = locales
        webSearchLabels = labels
        return labels
    }

    private fun readSelection(mode: ActionMode, search: Boolean): Boolean {
        val actions = selectionActions ?: return true
        if (disposed || currentMode !== mode || !settings.javaScriptEnabled) return true
        val request = ++requestGeneration
        val document = documentGeneration
        val originalUrl = url
        val ownerRef = WeakReference(this)
        val modeRef = WeakReference(mode)
        val actionsRef = WeakReference(actions)
        try {
            evaluateJavascript(SELECTION_SCRIPT) { result ->
                val owner = ownerRef.get() ?: return@evaluateJavascript
                val originatingMode = modeRef.get() ?: return@evaluateJavascript
                val originatingActions = actionsRef.get() ?: return@evaluateJavascript
                owner.acceptSelection(originatingMode, originatingActions, request, document, originalUrl, search, result)
            }
        } catch (_: RuntimeException) {
            if (currentMode === mode && requestGeneration == request) cancelSelection()
        }
        return true
    }

    private fun acceptSelection(
        mode: ActionMode,
        actions: SelectionActions,
        request: Long,
        document: Long,
        originalUrl: String?,
        search: Boolean,
        result: String?
    ) {
        if (disposed || currentMode !== mode || selectionActions !== actions ||
            requestGeneration != request || documentGeneration != document || url != originalUrl
        ) return
        val text = runCatching { JSONTokener(result ?: "null").nextValue() as? String }.getOrNull().orEmpty()
        val acceptedRequest = ++requestGeneration
        currentMode = null
        mode.finish()
        // Finishing the platform mode may itself navigate, dispose, or create another selection.
        if (text.isEmpty() || disposed || selectionActions !== actions || documentGeneration != document ||
            requestGeneration != acceptedRequest || url != originalUrl
        ) return
        if (search) actions.search(text) else actions.find(text)
    }

    private inner class SelectionCallback(private val delegate: ActionMode.Callback) : ActionMode.Callback2() {
        val owner: BrowserPageView get() = this@BrowserPageView

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            if (disposed || !delegate.onCreateActionMode(mode, menu)) return false
            adoptMode(mode)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val changed = delegate.onPrepareActionMode(mode, menu)
            return if (!disposed && currentMode === mode) rebuildMenu(mode, menu) || changed else changed
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = when (item.itemId) {
            SEARCH_ITEM_ID -> readSelection(mode, search = true)
            FIND_ITEM_ID -> readSelection(mode, search = false)
            else -> delegate.onActionItemClicked(mode, item)
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            if (currentMode === mode) {
                currentMode = null
                requestGeneration++
            }
            delegate.onDestroyActionMode(mode)
        }

        override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
            val callback = delegate
            if (callback is ActionMode.Callback2) callback.onGetContentRect(mode, view, outRect)
            else super.onGetContentRect(mode, view, outRect)
        }
    }

    private companion object {
        val SEARCH_ITEM_ID = View.generateViewId()
        val FIND_ITEM_ID = View.generateViewId()
        const val SELECTION_SCRIPT = "(function(){return window.getSelection?window.getSelection().toString():window.document.getSelection?window.document.getSelection().toString():window.document.selection?window.document.selection.createRange().text:\"\"})();"
    }
}

/** sr/jc/st labels from GP 7.3.3 resources; absent translations inherit the APK's English default. */
private object SelectionMenuText {
    data class Labels(val search: String, val find: String = "Find", val nativeSearch: String = "Search in Via")
    private val english = Labels("Search")
    private val localized = mapOf(
        "ar" to Labels("بحث", "بحث", "بحث في Via"),
        "az" to Labels("Axtarış"), "be" to Labels("Пошук"), "bn" to Labels("অনুসন্ধান"),
        "cs" to Labels("Vyhledávání"), "da" to Labels("Søg", nativeSearch = "Søg i Via"),
        "de" to Labels("Suche"), "es" to Labels("Buscar", "Encontrar", "Buscar en Via"),
        "fa" to Labels("جستجو", "یافتن", "جستجو با ویا"), "fil" to Labels("Hanapin"),
        "fr" to Labels("Rechercher"), "hi" to Labels("खोज"), "hr" to Labels("Traži"),
        "hu" to Labels("Keresés"), "id" to Labels("Cari", "Cari", "Cari di Via"),
        "it" to Labels("Cerca"), "ja" to Labels("検索", nativeSearch = "Viaで検索する"),
        "ko" to Labels("검색", "찾기", "Via에서 검색"), "lt" to Labels("Paieška"),
        "mr" to Labels("शोध", "शोधा", "Via मध्ये शोधा"), "ne" to Labels("खोज्नुहोस्"),
        "pl" to Labels("Szukaj", "Znajdź", "Wyszukaj w Vii"), "pt" to Labels("Pesquisar"),
        "pt-BR" to Labels("Pesquisar", "Localizar", "Pesquisar no Via"), "ro" to Labels("Caută"),
        "ru" to Labels("Поиск", "Найти", "Поиск в Via"), "sk" to Labels("Vyhľadávanie"),
        "sl" to Labels("Išči", "Najdi", "Išči v Via"), "ta" to Labels("தேடு"),
        "th" to Labels("ค้นหา", "ค้นหา", "ค้นหาใน Via"), "tr" to Labels("Arama"),
        "uk" to Labels("Пошук", "Знайти", "Шукати у Via"), "ur" to Labels("تلاش کریں", "تلاش کریں", "Via میں تلاش کریں"),
        "uz" to Labels("Qidirish"), "vi" to Labels("Tìm kiếm", "Tìm kiếm", "Tìm kiếm trong Via"),
        "zh-CN" to Labels("搜索", "查找", "在 Via 中搜索"),
        "zh-TW" to Labels("搜尋", "尋找", "在 Via 中搜尋")
    )

    fun forLocale(locale: Locale): Labels {
        val language = if (locale.language == "in") "id" else locale.language
        val country = when {
            language != "zh" -> locale.country
            locale.script == "Hant" || locale.country in setOf("TW", "HK", "MO") -> "TW"
            else -> "CN"
        }
        return localized["$language-$country"] ?: localized[language] ?: english
    }
}
