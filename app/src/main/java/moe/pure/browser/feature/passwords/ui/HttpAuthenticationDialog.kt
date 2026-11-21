package moe.pure.browser.feature.passwords.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.feature.passwords.model.PasswordEntry
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.touchFeedback
import moe.pure.browser.ui.dialog.DialogLayoutOwner
import moe.pure.browser.ui.theme.palette

/** A credential form only: its owner authenticates candidates, saves records and continues/cancels HTTP. */
class HttpAuthenticationDialog(
    private val activity: Activity,
    host: String,
    private val onSubmit: (String, String) -> Unit,
    private val onCancel: () -> Unit,
    private val onCandidate: (String) -> Unit,
    private val onPicker: () -> Unit
) : Dialog(activity, R.style.Theme_PureBrowser_Dialog), DialogLayoutOwner {
    private var completed = false
    private var passwordVisible = false
    private val body = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL; setPadding(0, activity.dp(12), 0, 0)
        setBackgroundResource(R.drawable.browser_dialog_background)
        contentDescription = "登录 $host"
    }
    private val username = input("用户名").apply {
        imeOptions = EditorInfo.IME_ACTION_NEXT; setAutofillHints(View.AUTOFILL_HINT_USERNAME)
    }
    private val password = input("密码").apply {
        imeOptions = EditorInfo.IME_ACTION_DONE; setAutofillHints(View.AUTOFILL_HINT_PASSWORD)
    }
    private val candidates = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private val candidateScroll = HorizontalScrollView(activity).apply {
        visibility = View.GONE; isHorizontalScrollBarEnabled = false; clipToPadding = false
        setPadding(activity.dp(10), 0, activity.dp(10), 0)
        addView(candidates, FrameLayout.LayoutParams(-2, -1))
    }
    private val picker = ImageView(activity).apply {
        setImageResource(R.drawable.ref_password_save); imageTintList = ColorStateList.valueOf(activity.palette.icon)
        setPadding(activity.dp(7), 0, activity.dp(7), 0); contentDescription = "保存的密码"
        background = activity.touchFeedback(); setOnClickListener { onPicker() }
    }

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(body)
        body.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
            if (r - l != or - ol || b - t != ob - ot) updateGeometry()
        }
        setCancelable(true); setCanceledOnTouchOutside(false)
        window?.setDimAmount(.4f)
        body.addView(NativeTypography.text(activity).apply {
            text = "登录"; textSize = 16f; NativeTypography.style(this, android.graphics.Typeface.BOLD); setTextColor(activity.palette.text)
            textDirection = View.TEXT_DIRECTION_LOCALE
            setPadding(activity.dp(16), activity.dp(8), activity.dp(16), activity.dp(8))
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = activity.dp(6) })
        val userScroll = HorizontalScrollView(activity).apply {
            isFillViewport = true; isHorizontalScrollBarEnabled = false
            setBackgroundResource(R.drawable.dialog_field_underline)
            setPadding(0, activity.dp(8), 0, activity.dp(8))
            addView(username, FrameLayout.LayoutParams(-2, -2))
        }
        body.addView(userScroll, LinearLayout.LayoutParams(-1, -2).apply {
            marginStart = activity.dp(16); marginEnd = activity.dp(16); bottomMargin = activity.dp(16)
        })
        val passwordRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(activity.dp(16), 0, activity.dp(9), 0)
        }
        password.setBackgroundResource(R.drawable.dialog_field_underline)
        password.setPadding(0, activity.dp(8), 0, activity.dp(8))
        passwordRow.addView(password, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = activity.dp(16) })
        passwordRow.addView(picker, LinearLayout.LayoutParams(activity.dp(36), -1))
        body.addView(passwordRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = activity.dp(12) })
        body.addView(candidateScroll, LinearLayout.LayoutParams(-1, activity.dp(40)))
        val buttons = LinearLayout(activity).apply { gravity = Gravity.END; orientation = LinearLayout.HORIZONTAL }
        buttons.addView(button("取消") { cancel() })
        buttons.addView(button("确定", ::submit))
        body.addView(buttons, LinearLayout.LayoutParams(-1, -2))
        username.nextFocusForwardId = password.id
        username.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_NEXT) { password.requestFocus(); true } else false
        }
        password.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) { submit(); true } else false
        }
        password.setOnKeyListener { _, code, event ->
            if (code == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) { submit(); true } else false
        }
        attachVisibility()
    }

    fun setCandidates(entries: List<PasswordEntry>, libraryAvailable: Boolean = true) {
        if (completed) return
        picker.visibility = if (libraryAvailable) View.VISIBLE else View.GONE
        candidates.removeAllViews()
        entries.forEach { entry ->
            candidates.addView(NativeTypography.text(activity).apply {
                text = entry.username; textSize = 14f; typeface = Typeface.DEFAULT
                setTextColor(activity.palette.text); setSingleLine(true); ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER; maxWidth = activity.dp(120)
                setPadding(activity.dp(8), 0, activity.dp(8), 0)
                background = activity.touchFeedback(); isSaveEnabled = false
                setOnClickListener { onCandidate(entry.id) }
            }, LinearLayout.LayoutParams(-2, -1).apply {
                setMargins(activity.dp(6), activity.dp(4), activity.dp(6), activity.dp(4))
            })
        }
        candidateScroll.visibility = if (entries.isEmpty() || !libraryAvailable) View.GONE else View.VISIBLE
    }
    fun setCredentials(username: String, password: String) {
        if (completed) return
        this.username.setText(username); this.password.setText(password)
    }
    override fun show() { if (completed) return; super.show(); updateGeometry() }
    override fun updateGeometry() {
        val metrics = activity.resources.displayMetrics
        val width = minOf(activity.dp(384), minOf(metrics.widthPixels, metrics.heightPixels) - activity.dp(72)).coerceAtLeast(activity.dp(80))
        window?.apply { setLayout(width, -2); attributes = attributes.apply { gravity = Gravity.CENTER } }
    }
    fun close() { completed = true; dismiss() }
    override fun cancel() {
        if (!completed) { completed = true; dismiss(); onCancel() } else dismiss()
    }
    override fun dismiss() {
        username.text?.clear(); password.text?.clear(); candidates.removeAllViews()
        super.dismiss()
    }
    private fun submit() {
        if (completed) return
        val user = username.text.toString()
        val pass = password.text.toString()
        completed = true; dismiss()
        onSubmit(user, pass)
    }

    private fun input(hint: String) = NativeTypography.edit(activity).apply {
        id = View.generateViewId(); this.hint = hint; textSize = 14f; typeface = Typeface.DEFAULT
        setTextColor(activity.palette.text); setHintTextColor(activity.palette.hint)
        setBackgroundColor(android.graphics.Color.TRANSPARENT); setPadding(0, 0, 0, 0)
        setSingleLine(true); setHorizontallyScrolling(true)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        textDirection = View.TEXT_DIRECTION_LOCALE; isSaveEnabled = false
    }
    private fun button(text: String, clicked: () -> Unit) = NativeTypography.text(activity).apply {
        this.text = text; textSize = 14f; gravity = Gravity.CENTER
        setTextColor(activity.palette.text); minWidth = activity.dp(56)
        setPadding(activity.dp(16), activity.dp(16), activity.dp(16), activity.dp(16))
        background = activity.touchFeedback(); setOnClickListener { clicked() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachVisibility() {
        fun update() {
            val start = password.selectionStart.coerceAtLeast(0)
            val end = password.selectionEnd.coerceAtLeast(start)
            password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                if (passwordVisible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD
            password.typeface = Typeface.DEFAULT
            val icon = activity.getDrawable(if (passwordVisible) R.drawable.ref_ai_eye_on else R.drawable.ref_ai_eye_off)?.mutate()?.apply {
                setTint(activity.palette.icon); setBounds(0, 0, activity.dp(22), activity.dp(22))
            }
            password.setCompoundDrawablesRelative(null, null, icon, null)
            password.compoundDrawablePadding = activity.dp(4)
            password.setSelection(start.coerceAtMost(password.length()), end.coerceAtMost(password.length()))
        }
        update()
        var captured = false
        password.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> captured = if (password.layoutDirection == View.LAYOUT_DIRECTION_RTL)
                    event.x <= password.compoundPaddingLeft else event.x >= password.width - password.compoundPaddingRight
                MotionEvent.ACTION_UP -> if (captured) { passwordVisible = !passwordVisible; update() }
                MotionEvent.ACTION_CANCEL -> captured = false
            }
            captured
        }
    }
}
