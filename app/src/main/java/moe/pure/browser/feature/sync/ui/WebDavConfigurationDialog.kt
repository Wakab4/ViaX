package moe.pure.browser.feature.sync.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.feature.sync.model.WebDavProfile
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.touchFeedback
import moe.pure.browser.ui.dialog.DialogLayoutOwner
import moe.pure.browser.ui.theme.palette
import java.util.Locale

/** Two explicit pages, with no user swipe; confirmation stores configuration and never tests a connection. */
internal class WebDavConfigurationDialog(
    private val activity: Activity,
    private val existing: WebDavProfile?,
    private val save: (FormValues) -> Unit,
    private val remove: (FormValues) -> Unit
) : Dialog(activity, R.style.Theme_PureBrowser_Dialog), DialogLayoutOwner {
    class FormValues(val address: String, val username: String, val password: String, val digest: Boolean, val path: String) {
        override fun toString() = "WebDavFormValues(<redacted>)"
    }
    private val base = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL; setPadding(0, activity.dp(6), 0, 0)
        setBackgroundResource(R.drawable.browser_dialog_background)
    }
    private val pages = FrameLayout(activity)
    private val credentials = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private val settings = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private val address = SyncFields.field(activity, activity.getString(R.string.ref_sync_page_address), existing?.baseUrl ?: if (Locale.getDefault().country.equals("CN", true)) "https://dav.jianguoyun.com/dav/" else "")
    private val username = SyncFields.field(activity, activity.getString(R.string.ref_sync_page_username), existing?.username.orEmpty())
    private val password = SyncFields.field(activity, activity.getString(if (existing?.configured == true) R.string.ref_sync_page_password_unchanged else R.string.ref_sync_page_password), secret = true)
    private val path = SyncFields.field(activity, "/Via", existing?.remotePath.orEmpty())
    private val digest = CheckBox(activity).apply {
        isChecked = existing?.digestAuth ?: false
        buttonDrawable = activity.getDrawable(R.drawable.dialog_checkbox); buttonTintList = null
        isClickable = false; isFocusable = false
        minimumWidth = 0; minimumHeight = 0; setPadding(0, 0, 0, 0)
    }
    private var showingSettings = false
    private val saveButton = action(activity.getString(android.R.string.ok), ::confirm)
    private val removeButton = action(activity.getString(R.string.ref_sync_page_remove)) {
        remove(values()); dismiss()
    }

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(base)
        setCanceledOnTouchOutside(true)
        window?.setDimAmount(.4f)
        credentials.addView(header(activity.getString(R.string.ref_sync_page_webdav_configuration), action(activity.getString(R.string.ref_sync_page_form_settings)) { page(true) }.apply { setPadding(activity.dp(16), 0, activity.dp(16), 0) }))
        SyncFields.append(credentials, address); SyncFields.append(credentials, username); SyncFields.append(credentials, password)
        address.nextFocusForwardId = username.id; username.nextFocusForwardId = password.id
        address.imeOptions = EditorInfo.IME_ACTION_DONE
        password.imeOptions = EditorInfo.IME_ACTION_NEXT
        val buttons = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
        removeButton.visibility = if (existing?.configured == true) View.VISIBLE else View.GONE
        buttons.addView(removeButton, LinearLayout.LayoutParams(-2, -2))
        buttons.addView(View(activity), LinearLayout.LayoutParams(0, 1, 1f))
        buttons.addView(action(activity.getString(android.R.string.cancel)) { cancel() }); buttons.addView(saveButton)
        credentials.addView(buttons, LinearLayout.LayoutParams(-1, -2))

        settings.addView(LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(activity).apply {
                setImageResource(R.drawable.ref_ic_back); imageTintList = ColorStateList.valueOf(activity.palette.icon)
                setPadding(dp(13), dp(13), dp(13), dp(13)); contentDescription = activity.getString(R.string.ref_sync_page_back)
                background = activity.touchFeedback(); setOnClickListener { page(false) }
            }, LinearLayout.LayoutParams(activity.dp(54), activity.dp(48)))
            addView(caption(activity.getString(R.string.ref_sync_page_form_settings), 16f, bold = true), LinearLayout.LayoutParams(0, -1, 1f))
        })
        settings.addView(LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL; background = activity.touchFeedback()
            setPadding(dp(16), dp(10), dp(16), dp(10))
            addView(caption(activity.getString(R.string.ref_sync_page_digest), 14f), LinearLayout.LayoutParams(0, -2, 1f))
            addView(digest, LinearLayout.LayoutParams(activity.dp(24), activity.dp(24)))
            setOnClickListener { digest.isChecked = !digest.isChecked }
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = activity.dp(4) })
        settings.addView(LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), 0, dp(16), 0)
            addView(caption(activity.getString(R.string.ref_sync_page_directory), 14f))
            addView(path, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = activity.dp(16) })
        }, LinearLayout.LayoutParams(-1, activity.dp(45)).apply { bottomMargin = activity.dp(4) })
        pages.addView(credentials, FrameLayout.LayoutParams(-1, -2)); pages.addView(settings, FrameLayout.LayoutParams(-1, -2))
        base.addView(ScrollView(activity).apply { isFillViewport = true; addView(pages) }, LinearLayout.LayoutParams(-1, -2))
        page(showingSettings)
        base.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob -> if (r-l != or-ol || b-t != ob-ot) updateGeometry() }
    }
    private fun values() = FormValues(address.text.toString(), username.text.toString(), password.text.toString(), digest.isChecked, path.text.toString())
    private fun confirm() {
        val fields = values()
        val url = fields.address.trim { it <= ' ' }; val user = fields.username.trim { it <= ' ' }; val secret = fields.password.trim { it <= ' ' }
        if (url.isEmpty()) { moe.pure.browser.ui.editor.NativeFieldFeedback.required(address); return }
        if (user.isEmpty()) { moe.pure.browser.ui.editor.NativeFieldFeedback.required(username); return }
        val unchanged = existing != null && existing.baseUrl == url && existing.username == user
        if (secret.isEmpty() && !unchanged) { moe.pure.browser.ui.editor.NativeFieldFeedback.required(password); return }
        save(FormValues(url, user, secret, fields.digest, fields.path.trim { it <= ' ' })); dismiss()
    }
    private fun page(value: Boolean) {
        showingSettings = value
        credentials.visibility = if (value) View.GONE else View.VISIBLE
        settings.visibility = if (value) View.VISIBLE else View.GONE
        pages.requestLayout()
    }
    @Deprecated("Framework dialog back") override fun onBackPressed() { if (showingSettings) page(false) else cancel() }
    override fun show() { super.show(); updateGeometry() }
    override fun dismiss() { password.text?.clear(); super.dismiss() }
    override fun updateGeometry() {
        val metrics = activity.resources.displayMetrics
        val width = minOf(activity.dp(384), minOf(metrics.widthPixels, metrics.heightPixels) - activity.dp(72)).coerceAtLeast(1)
        window?.setLayout(width, -2)
    }
    private fun caption(text: String, size: Float, bold: Boolean = false) = NativeTypography.text(activity).apply {
        this.text = text; textSize = size; setTextColor(activity.palette.text)
        gravity = Gravity.CENTER_VERTICAL; NativeTypography.style(this, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    private fun action(text: String, clicked: () -> Unit) = NativeTypography.text(activity).apply {
        this.text = text; textSize = 14f; gravity = Gravity.CENTER; setTextColor(activity.palette.text)
        minWidth = activity.dp(56); setPadding(activity.dp(16), activity.dp(16), activity.dp(16), activity.dp(16))
        background = activity.touchFeedback(); setOnClickListener { clicked() }
    }
    private fun header(text: String, end: View) = LinearLayout(activity).apply {
        gravity = Gravity.CENTER_VERTICAL
        addView(caption(text, 16f, true).apply { setPadding(activity.dp(16), 0, 0, 0) }, LinearLayout.LayoutParams(0, -1, 1f))
        addView(end, LinearLayout.LayoutParams(-2, activity.dp(48)))
        layoutParams = LinearLayout.LayoutParams(-1, activity.dp(48))
    }
}
