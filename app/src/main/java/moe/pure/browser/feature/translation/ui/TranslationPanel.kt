package moe.pure.browser.feature.translation.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import moe.pure.browser.ui.message.BrowserMessages

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.feature.translation.TextTranslationRepository
import moe.pure.browser.feature.translation.TranslationDraftCache
import moe.pure.browser.feature.translation.TranslationLanguage
import moe.pure.browser.feature.translation.TranslationLanguages
import moe.pure.browser.feature.translation.TranslationResult
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.theme.palette

/** Browser overlay content. Its owner supplies placement, surface and enter/exit animation. */
class TranslationPanel(private val activity: Activity, private val close: () -> Unit) {
    val view: View = LayoutInflater.from(activity).inflate(R.layout.ref_translation_panel, null).also { NativeTypography.tree(it) }
    private val input = view.findViewById<EditText>(R.id.ref_translation_input)
    private val output = view.findViewById<TextView>(R.id.ref_translation_result)
    private val clear = view.findViewById<ImageView>(R.id.ref_translation_clear)
    private val copy = view.findViewById<ImageView>(R.id.ref_translation_copy)
    private val sourceLabel = view.findViewById<TextView>(R.id.ref_translation_source)
    private val targetLabel = view.findViewById<TextView>(R.id.ref_translation_target)
    private val dialogs = DialogScope()
    private val languages = TranslationLanguages(activity.resources.configuration.locales[0])
    private val repository = TextTranslationRepository()
    private val progress = TranslationProgress(view.findViewById<ProgressBar>(R.id.ref_translation_progress))
    private var source = languages.source("auto")
    private var target = languages.defaultTarget()
    private var result: TranslationResult? = null
    private var layoutState = 0
    private var disposed = false
    private var request = 0L

    init {
        view.accessibilityPaneTitle = "翻译"
        listOf(input, output, sourceLabel, targetLabel).forEach { it.setTextColor(activity.palette.text) }
        input.setHintTextColor(activity.palette.secondary)
        view.findViewById<View>(R.id.ref_translation_divider)
            .setBackgroundColor(if (activity.palette.night) 0x30111111 else 0x30EEEEEE)
        listOf(clear, copy, view.findViewById<ImageView>(R.id.ref_translation_swap)).forEach {
            it.imageTintList = ColorStateList.valueOf(activity.palette.icon)
            it.isFocusable = true
        }
        val submitButton = view.findViewById<ImageView>(R.id.ref_translation_submit)
        submitButton.background = submitBackground()
        submitButton.isFocusable = true
        submitButton.setOnClickListener { submit() }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(text: Editable?) { updateClear() }
        })
        input.setOnFocusChangeListener { _, focused ->
            if (focused) showState(INPUT) else if (result != null) showState(RESULT)
        }
        clear.setOnClickListener {
            if (input.length() == 0) close()
            else {
                input.setText("")
                showState(INPUT)
            }
        }
        output.setOnClickListener { showState(RESULT) }
        copy.setOnClickListener { copyResult() }
        view.findViewById<View>(R.id.ref_translation_swap).setOnClickListener { swapLanguages() }
        sourceLabel.setOnClickListener { chooseLanguage(true) }
        targetLabel.setOnClickListener { chooseLanguage(false) }
        restoreDraft()
        updateLanguages()
        updateClear()
        showState(INPUT)
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(attached: View) {
                attached.post {
                    if (disposed || !attached.isAttachedToWindow) return@post
                    val focused = if (layoutState == RESULT) output else input
                    focused.requestFocus()
                    focused.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
                }
            }
            override fun onViewDetachedFromWindow(detached: View) = Unit
        })
    }

    /** A null value keeps a newly restored draft; nonempty supplied text starts translation by default. */
    fun setInput(text: String?, translate: Boolean = !text.isNullOrEmpty()) {
        if (disposed || text == null) return
        input.setText(text)
        if (translate) submit()
    }

    fun submit() {
        if (disposed) return
        val version = ++request
        repository.cancel()
        val query = input.text.toString().trim()
        if (query.isEmpty()) {
            progress.finish()
            result = null
            showState(INPUT)
            return
        }
        progress.begin()
        repository.translate(query, source.code, target.code) { value ->
            if (disposed || version != request) return@translate
            progress.finish()
            result = value
            if (value == null) showState(INPUT)
            else {
                output.text = value.translated
                hideKeyboard()
                showState(RESULT)
            }
        }
    }

    fun hideKeyboard() {
        activity.getSystemService(InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(input.windowToken, 0)
    }

    fun updateGeometry() = dialogs.updateGeometry()

    fun dispose() {
        if (disposed) return
        disposed = true
        request++
        TranslationDraftCache.save(input.text.toString(), source.code, target.code)
        repository.close()
        progress.dispose()
        dialogs.dispose()
        hideKeyboard()
    }

    private fun showState(state: Int) {
        // The reference keeps the result object while the input is edited and ignores repeated states.
        if (layoutState == state) return
        layoutState = state
        if (state == INPUT) {
            if (result == null) {
                input.setLines(7)
                output.visibility = View.GONE
                copy.visibility = View.GONE
            } else {
                input.setLines(5)
                output.scrollX = 0
                output.setLines(1)
                output.visibility = View.VISIBLE
                copy.visibility = View.VISIBLE
            }
        } else {
            input.setLines(1)
            input.scrollX = 0
            output.setLines(5)
            output.scrollX = 0
            output.visibility = View.VISIBLE
            copy.visibility = View.VISIBLE
            output.requestFocus()
            hideKeyboard()
        }
    }

    private fun updateClear() {
        val hasInput = input.length() > 0
        clear.setImageResource(if (hasInput) R.drawable.ref_translation_clear else R.drawable.ref_translation_exit)
        clear.contentDescription = if (hasInput) "清除文本" else "退出"
    }

    private fun restoreDraft() {
        val draft = TranslationDraftCache.restore() ?: return
        if (draft.source.isEmpty() || draft.target.isEmpty()) return
        source = languages.source(draft.source)
        target = languages.target(draft.target)
        input.setText(draft.query)
    }

    private fun updateLanguages() {
        sourceLabel.text = source.label
        targetLabel.text = target.label
        sourceLabel.contentDescription = "源语言，${source.label}"
        targetLabel.contentDescription = "目标语言，${target.label}"
    }

    private fun chooseLanguage(from: Boolean) {
        if (disposed) return
        val choices = if (from) languages.sources else languages.targets
        val current = if (from) source else target
        val selected = choices.indexOfFirst { it.code == current.code }.coerceAtLeast(0)
        dialogs.show(BrowserDialog(activity, if (from) "源语言" else "目标语言")
            .singleChoice(choices.map(TranslationLanguage::label), selected) { index ->
                if (disposed) return@singleChoice
                if (from) source = choices[index] else target = choices[index]
                updateLanguages()
            })
    }

    private fun swapLanguages() {
        var from = source
        if (from.code == "auto") result?.let { from = languages.source(it.source) }
        if (from.code == "auto" || from.code == target.code) return
        source = target
        target = from
        updateLanguages()
        val original = input.text.toString()
        input.setText(output.text.toString())
        output.text = original
    }

    private fun copyResult() {
        if (output.length() == 0) return
        activity.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText(null, output.text.toString()))
        BrowserMessages.show(activity, "已复制文本")
    }

    private fun submitBackground(): RippleDrawable {
        fun circle(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
        val pressed = if (activity.palette.night) 0xFF48598B.toInt() else 0xFF5B73B5.toInt()
        return RippleDrawable(ColorStateList.valueOf(pressed), circle(activity.palette.accent), circle(Color.BLACK))
    }

    companion object {
        private const val INPUT = 1
        private const val RESULT = 2
    }
}
