package moe.pure.browser.feature.download.open

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.pure.browser.feature.pdf.PdfPreview
import moe.pure.browser.feature.settings.context.SettingsWindowEffects
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.touchFeedback
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.ui.theme.palette
import java.lang.ref.WeakReference
import java.util.concurrent.FutureTask

/** p1 owns the query and selected command; an application is launched only after dismissal. */
class FileOpenDialogFragment : DialogFragment() {
    private var query: FutureTask<Unit>? = null
    private var epoch = 0L
    private var selected: FileOpenChoice? = null
    private var list: RecyclerView? = null
    private var empty: TextView? = null
    private var grid: FileOpenGrid? = null
    private var gridLayout: FileOpenGridLayout? = null
    private var edges: NativeRecyclerEdgeEffect? = null
    private var effects: AutoCloseable? = null
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setStyle(STYLE_NO_TITLE, R.style.Theme_PureBrowser_Dialog) }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, 0)
            addView(NativeTypography.text(context).apply {
                setText(R.string.ref_file_open_title); textSize = 16f; setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(context.palette.text); setSingleLine(true); ellipsize = android.text.TextUtils.TruncateAt.END
                textDirection = View.TEXT_DIRECTION_LOCALE; setPadding(0, dp(8), 0, dp(8))
            }, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(16), 0, dp(16), dp(6)) })
            val layout = FileOpenGridLayout(context).also { gridLayout = it }
            val adapter = FileOpenGrid(context) { choice -> selected = choice; dismiss() }.also { grid = it }
            addView(RecyclerView(context).apply {
                id = android.R.id.list; layoutManager = layout; this.adapter = adapter; clipToPadding = false
                setPadding(dp(8), 0, dp(8), 0); addItemDecoration(FileOpenGridSpacing(dp(4)))
                overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS; isVerticalScrollBarEnabled = false
                edgeEffectFactory = NativeRecyclerEdgeEffect().also { edges = it }
            }.also { list = it }, LinearLayout.LayoutParams(-1, -2))
            addView(NativeTypography.text(context).apply {
                setText(R.string.ref_file_open_empty); textSize = 12f; setTextColor(context.palette.secondary)
                gravity = Gravity.CENTER; setPadding(dp(16), dp(16), dp(16), dp(8)); visibility = View.GONE
                textDirection = View.TEXT_DIRECTION_LOCALE
            }.also { empty = it }, LinearLayout.LayoutParams(-1, -2))
            addView(NativeTypography.text(context).apply {
                setText(android.R.string.cancel); textSize = 14f; setTextColor(context.palette.accent)
                gravity = Gravity.CENTER; minWidth = dp(56); setSingleLine(true); ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(dp(16), dp(16), dp(16), dp(16)); background = context.touchFeedback(); setOnClickListener { dismiss() }
            }, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.END })
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val request = arguments?.let(FileOpenRequest::read) ?: return
        val ticket = ++epoch
        val app = requireContext().applicationContext
        val label = getString(R.string.ref_file_open_pdf)
        val owner = WeakReference(this)
        val main = Handler(Looper.getMainLooper())
        query = FutureTask<Unit> {
            val choices = runCatching { FileOpenQuery.load(app, request, label) }.getOrElse {
                moe.pure.browser.core.logging.NativeLog.warn("FileOpen", "Unable to query file applications", it); return@FutureTask
            }
            main.post { owner.get()?.takeIf { it.epoch == ticket && it.view != null }?.render(choices) }
        }.also(BrowserExecutors.persistence::execute)
    }
    private fun render(choices: List<FileOpenChoice>) {
        grid?.submit(choices); list?.visibility = if (choices.isEmpty()) View.GONE else View.VISIBLE
        empty?.visibility = if (choices.isEmpty()) View.VISIBLE else View.GONE
    }
    override fun onStart() {
        super.onStart()
        val metrics = resources.displayMetrics
        val context = requireContext()
        gridLayout?.apply { spanCount = maxOf(1, metrics.widthPixels / maxOf(1, context.dp(95))); maximumHeight = metrics.heightPixels * 2 / 3 }
        list?.requestLayout()
        dialog?.window?.apply {
            setLayout(minOf(context.dp(384), minOf(metrics.widthPixels, metrics.heightPixels) - context.dp(72)).coerceAtLeast(1), -2)
            setGravity(Gravity.CENTER); attributes = attributes.apply { dimAmount = .4f; windowAnimations = R.style.Animation_PureBrowser_Dialog }
        }
        dialog?.setCanceledOnTouchOutside(true)
        effects?.close(); effects = dialog?.let(SettingsWindowEffects::attachDialog)
    }
    override fun onDismiss(dialog: DialogInterface) {
        val action = selected; selected = null
        val request = arguments?.let(FileOpenRequest::read)
        val host = activity
        val source = parentFragment
        super.onDismiss(dialog)
        if (action == null || request == null || host == null || host.isFinishing || host.isDestroyed) return
        if (source != null && (!source.isAdded || source.parentFragmentManager.isStateSaved || action.kind == 2 && !source.isVisible)) return
        val opened = if (action.kind == 2) PdfPreview.open(host, FileOpenQuery.uri(request.path), null, source)
            else action.intent?.let { runCatching { host.startActivity(it); true }.getOrDefault(false) } == true
        if (!opened) BrowserMessages.show(host, R.string.ref_download_action_unavailable)
    }
    override fun onStop() { effects?.close(); effects = null; super.onStop() }
    override fun onDestroyView() {
        epoch++; query?.cancel(true); query = null
        list?.adapter = null; list = null; empty = null; grid = null; gridLayout = null
        edges?.close(); edges = null; effects?.close(); effects = null
        super.onDestroyView()
    }
    companion object {
        const val TAG = "native_file_open"
        internal fun create(request: FileOpenRequest) = FileOpenDialogFragment().apply { arguments = request.save() }
    }
}
