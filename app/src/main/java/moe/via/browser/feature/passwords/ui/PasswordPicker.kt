package moe.via.browser.feature.passwords.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.passwords.data.PasswordRepository
import moe.via.browser.feature.passwords.model.PasswordSites
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.surface
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.dialog.DialogLayoutOwner
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.palette
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.via.browser.ui.tasks.NativeViewJobs

/** The selector exposes only an entry ID. Its caller owns device confirmation and decryption. */
class PasswordPicker(private val activity: ThemedActivity) {
    private val repositoryDelegate = lazy { PasswordRepository(activity.applicationContext) }
    private val repository by repositoryDelegate
    private val handler = Handler(Looper.getMainLooper())
    private var dialog: PickerWindow? = null
    private var jobs: NativeViewJobs? = null
    private var disposed = false
    private var pending: Runnable? = null

    fun show(currentUrl: String, onSelectedId: (String) -> Unit) {
        if (disposed || activity.isDestroyed) return
        dismiss()
        val window = PickerWindow(activity, { text -> query(text, currentUrl, false) }, { id ->
            dismiss(); onSelectedId(id)
        })
        dialog = window
        jobs = NativeViewJobs()
        activity.dialogScope.show(window) {
            window.release()
            if (dialog === window) { dialog = null; jobs?.close(); jobs = null; pending?.let(handler::removeCallbacks); pending = null }
        }
        query("", currentUrl, true)
    }

    private fun query(text: String, currentUrl: String, locate: Boolean) {
        pending?.let(handler::removeCallbacks)
        val target = dialog ?: return
        val scope = jobs ?: return
        val action = Runnable {
            scope.launch({ repository.all(text) }) { entries ->
                target.adapter.submit(entries); target.empty(entries.isEmpty())
                if (locate) target.locate(currentUrl)
            }
        }
        pending = action
        if (locate) action.run() else handler.postDelayed(action, 100)
    }

    fun dismiss() {
        jobs?.close(); jobs = null; pending?.let(handler::removeCallbacks); pending = null
        dialog?.dismiss(); dialog = null
    }
    fun back(): Boolean { if (dialog == null) return false; dismiss(); return true }
    fun dispose() {
        disposed = true; dismiss(); handler.removeCallbacksAndMessages(null)
        if (repositoryDelegate.isInitialized()) BrowserExecutors.persistence.execute { repository.close() }
    }

    private class PickerWindow(
        private val activity: ThemedActivity,
        changed: (String) -> Unit,
        private val choose: (String) -> Unit
    ) : Dialog(activity, R.style.Theme_ViaBrowser_Dialog), DialogLayoutOwner {
        val adapter = PasswordRecordAdapter(activity, ::chooseAt, null, selectableRows = false)
        private val title = NativeTypography.text(activity).apply {
            setText(R.string.ref_password_csv_saved); textSize = 16f; NativeTypography.style(this, android.graphics.Typeface.BOLD); setTextColor(activity.palette.text)
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), 0, 0, 0)
        }
        private val input = NativeTypography.edit(activity).apply {
            hint = "搜索"; textSize = 16f; NativeTypography.style(this, android.graphics.Typeface.BOLD)
            setTextColor(activity.palette.text); setHintTextColor(activity.palette.hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setSingleLine(); setPadding(dp(16), 0, 0, 0); setBackgroundColor(Color.TRANSPARENT)
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO; isSaveEnabled = false; visibility = View.GONE
        }
        private val icon = ImageView(activity).apply {
            setImageResource(R.drawable.ref_ic_search); imageTintList = ColorStateList.valueOf(activity.palette.icon)
            contentDescription = "搜索"; setPadding(dp(13), dp(13), dp(13), dp(13)); background = activity.touchFeedback()
        }
        private val list = RecyclerView(activity).apply {
            id = android.R.id.list; layoutManager = LinearLayoutManager(activity); NativeRecyclerEdgeEffect.install(this)
            adapter = this@PickerWindow.adapter
        }
        private val empty = PasswordSettingRows.empty(activity).apply { textSize = 14f; visibility = View.GONE }
        private var searching = false
        private var released = false

        init {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            val root = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL; setPadding(0, dp(6), 0, 0)
                background = activity.surface(activity.palette.panel, 18)
            }
            val header = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
            val captions = FrameLayout(activity).apply {
                addView(title, FrameLayout.LayoutParams(-1, -1)); addView(input, FrameLayout.LayoutParams(-1, -1))
            }
            header.addView(captions, LinearLayout.LayoutParams(0, activity.dp(48), 1f))
            header.addView(icon, LinearLayout.LayoutParams(activity.dp(48), activity.dp(48)))
            root.addView(header, LinearLayout.LayoutParams(-1, -2))
            val host = FrameLayout(activity).apply {
                addView(list, FrameLayout.LayoutParams(-1, -1)); addView(empty, FrameLayout.LayoutParams(-1, -1))
            }
            root.addView(host, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
            icon.setOnClickListener { toggleSearch() }
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed(s?.toString().orEmpty()) }
                override fun afterTextChanged(s: Editable?) = Unit
            })
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                setDimAmount(.4f)
            }
        }
        override fun show() { super.show(); updateGeometry() }
        override fun dismiss() { release(); super.dismiss() }
        private fun chooseAt(position: Int) { adapter.entry(position)?.let { choose(it.id) } }
        fun release() { if (!released) { released = true; list.stopScroll(); list.itemAnimator?.endAnimations(); (list.edgeEffectFactory as? NativeRecyclerEdgeEffect)?.close(); list.adapter = null } }
        override fun updateGeometry() {
            val metrics = activity.resources.displayMetrics
            val side = minOf(metrics.widthPixels, metrics.heightPixels)
            window?.setLayout(minOf(side * 4 / 5, activity.dp(450)), minOf(side * 4 / 5, activity.dp(600)))
        }
        fun empty(value: Boolean) {
            empty.visibility = if (value) View.VISIBLE else View.GONE
            list.visibility = if (value) View.GONE else View.VISIBLE
        }
        fun locate(url: String) {
            val authority = PasswordSites.authority(url).ifEmpty { url }
            var position = adapter.rows.indexOfFirst { it is PasswordRecordAdapter.Row.Group && it.site == authority }
            if (position < 0) {
                val base = PasswordSites.baseDomain(if (url.contains("://")) url else "https://$url")
                if (base.isNotEmpty() && base != authority) position = adapter.rows.indexOfFirst {
                    it is PasswordRecordAdapter.Row.Group && it.site.contains(base)
                }
            }
            if (position >= 0) list.scrollToPosition(position)
        }
        private fun toggleSearch() {
            searching = !searching
            title.animate().cancel(); input.animate().cancel()
            if (searching) {
                title.animate().alpha(0f).setDuration(100).withEndAction { title.visibility = View.INVISIBLE }.start()
                input.alpha = 0f; input.visibility = View.VISIBLE; input.animate().alpha(1f).setDuration(100).start()
                input.requestFocus(); input.post { activity.getSystemService(InputMethodManager::class.java)?.showSoftInput(input, 0) }
            } else {
                input.setText(""); activity.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(input.windowToken, 0)
                title.visibility = View.VISIBLE; title.animate().alpha(1f).setDuration(100).start()
                input.animate().alpha(0f).setDuration(100).withEndAction { input.visibility = View.GONE }.start()
            }
            icon.setImageResource(if (searching) R.drawable.ref_ic_close else R.drawable.ref_ic_search)
            icon.contentDescription = if (searching) "取消" else "搜索"
        }
    }
}
