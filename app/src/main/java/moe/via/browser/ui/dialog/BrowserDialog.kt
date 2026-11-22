package moe.via.browser.ui.dialog

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.app.Dialog
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** Shared reference dialog surface. Features supply values and actions, not window geometry. */
class BrowserDialog(private val activity: Activity, title: CharSequence? = null, message: CharSequence? = null) : Dialog(activity, R.style.Theme_ViaBrowser_Dialog), DialogLayoutOwner {
    private val body = LayoutInflater.from(activity).inflate(R.layout.browser_dialog, null)
    private val titleView = body.findViewById<TextView>(R.id.dialog_title)
    private val messageView = body.findViewById<TextView>(R.id.dialog_message)
    private val scroll = body.findViewById<DialogScrollView>(R.id.dialog_message_scroll)
    private val content = body.findViewById<FrameLayout>(R.id.dialog_content)
    private val fields = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private val list = body.findViewById<DialogListView>(R.id.dialog_list)
    private var choices: DialogChoices? = null
    private var point: DialogAnchor? = null
    private var hasForm = false
    private var hasCustom = false
    private var fixedWidthDp: Int? = null
    val isAnchored get() = point != null
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(body)
        list.dividerHeight = -1
        titleView.text = title; titleView.visibility = if (title == null) View.GONE else View.VISIBLE
        titleView.setTextColor(activity.palette.text); NativeTypography.style(titleView, android.graphics.Typeface.BOLD); titleView.setLines(1); titleView.maxLines = 2
        messageView.text = message; messageView.visibility = if (message == null) View.GONE else View.VISIBLE
        messageView.setTextColor(activity.palette.text); messageView.setTextIsSelectable(true)
        if (message is SpannableString) messageView.movementMethod = LinkMovementMethod.getInstance()
        setCanceledOnTouchOutside(true)
        window?.setDimAmount(.4f)
    }
    fun field(value: String = "", hint: String = "", lines: Int = 1): EditText {
        check(!hasCustom) { "A dialog cannot mix a custom view and generated form fields" }
        if (!hasForm) { content.addView(fields); content.visibility = View.VISIBLE; hasForm = true }
        return DialogField.append(activity, fields, value, hint, lines)
    }
    fun custom(view: View): BrowserDialog {
        check(!hasForm); content.removeAllViews(); content.addView(view); content.visibility = View.VISIBLE; hasCustom = true; return this
    }
    fun checkbox(label: CharSequence, checked: Boolean = false): CheckBox {
        val row = body.findViewById<LinearLayout>(R.id.dialog_checkbox_row)
        val check = body.findViewById<CheckBox>(R.id.dialog_checkbox)
        val caption = body.findViewById<TextView>(R.id.dialog_checkbox_label)
        caption.text = label; caption.setTextColor(activity.palette.secondary); caption.visibility = View.VISIBLE
        check.isChecked = checked; check.visibility = View.VISIBLE
        row.setOnClickListener { check.isChecked = !check.isChecked }
        return check
    }
    fun positive(label: CharSequence, action: (BrowserDialog) -> Boolean = { true }): BrowserDialog = button(R.id.dialog_positive, label, action)
    fun negative(label: CharSequence = "取消", action: (BrowserDialog) -> Boolean = { true }): BrowserDialog = button(R.id.dialog_negative, label, action)
    fun neutral(label: CharSequence, action: (BrowserDialog) -> Boolean = { true }): BrowserDialog = button(R.id.dialog_neutral, label, action)
    val selectedIndices get() = choices?.selection.orEmpty()
    fun items(labels: List<CharSequence>, action: (Int) -> Unit): BrowserDialog = configureChoices(labels, DialogChoiceMode.ACTION, emptySet(), action)
    fun singleChoice(labels: List<CharSequence>, selected: Int, highlighted: Boolean = false, action: ((Int) -> Unit)? = null): BrowserDialog =
        configureChoices(labels, if (highlighted) DialogChoiceMode.HIGHLIGHT_SINGLE else DialogChoiceMode.SINGLE, setOf(selected), action)
    fun multipleChoice(labels: List<CharSequence>, selected: Set<Int>, highlighted: Boolean = false): BrowserDialog =
        configureChoices(labels, if (highlighted) DialogChoiceMode.HIGHLIGHT_MULTIPLE else DialogChoiceMode.MULTIPLE, selected, null)
    private fun configureChoices(labels: List<CharSequence>, mode: DialogChoiceMode, selected: Set<Int>, action: ((Int) -> Unit)?): BrowserDialog {
        val adapter = DialogChoices(activity, labels, mode, selected, messageView.visibility == View.VISIBLE)
        choices = adapter; list.adapter = adapter; list.visibility = if (labels.isEmpty()) View.GONE else View.VISIBLE
        list.setOnItemClickListener { _, _, index, _ ->
            adapter.select(index)
            if (!adapter.multiple && action != null) { action(index); dismiss() }
        }
        adapter.selection.singleOrNull()?.let { index -> list.post { list.setSelectionFromTop(index, 0) } }
        return this
    }
    fun anchoredAt(x: Int, y: Int): BrowserDialog { point = DialogAnchor(x, y); return this }
    fun widthDp(value: Int): BrowserDialog { require(value > 0); fixedWidthDp = value; return this }
    fun anchoredTo(view: View): BrowserDialog {
        val location = IntArray(2); view.getLocationOnScreen(location)
        return anchoredAt(location[0] + view.width, location[1] + view.height / 2)
    }
    private fun button(id: Int, label: CharSequence, action: (BrowserDialog) -> Boolean): BrowserDialog {
        body.findViewById<TextView>(id).apply {
            text = label; visibility = View.VISIBLE
            setOnClickListener { if (action(this@BrowserDialog)) dismiss() }
        }
        return this
    }
    override fun show() {
        updateGeometry()
        super.show()
        updateGeometry()
        window?.apply {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility = activity.window.decorView.systemUiVisibility
        }
        moe.via.browser.ui.dialog.SettingsWindowEffects.attachDialog(this)
    }
    override fun updateGeometry() {
        val anchor = point
        val width: Int
        val limit: Int
        if (anchor == null) {
            val geometry = DialogLayoutPolicy.centered(activity, hasForm)
            width = fixedWidthDp?.let { requested ->
                val metrics = activity.resources.displayMetrics
                minOf(activity.dp(requested), minOf(metrics.widthPixels, metrics.heightPixels) - activity.dp(32)).coerceAtLeast(1)
            } ?: geometry.width
            limit = geometry.contentHeightLimit
            window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        } else {
            val geometry = DialogLayoutPolicy.anchored(activity, hasForm, anchor, fixedWidthDp?.let(activity::dp))
            width = geometry.width; limit = geometry.contentHeightLimit
            window?.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                attributes = attributes.apply { gravity = geometry.gravity; x = geometry.x; y = geometry.y }
            }
            if (choices?.count?.let { it > 0 } == true) {
                titleView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, activity.resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
                messageView.textSize = 12f
            }
        }
        scroll.heightLimit = if (choices?.count?.let { it > 0 } == true) limit / 3 else limit
        list.heightLimit = if (messageView.visibility == View.VISIBLE) limit * 2 / 3 else limit
        if (messageView.visibility == View.VISIBLE && !hasForm && !hasCustom && choices == null) messageView.minHeight = activity.dp(76)
        window?.setLayout(width, -2)
    }
}
