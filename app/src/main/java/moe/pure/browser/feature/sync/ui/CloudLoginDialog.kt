package moe.pure.browser.feature.sync.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.ui.theme.palette

/** hb.a8's fields start from the current username and an empty password. */
internal class CloudLoginDialog(
    private val activity: ThemedActivity,
    server: Int,
    username: String,
    private val submitted: (String, String) -> Unit
) {
    private val user = SyncFields.field(activity, activity.getString(R.string.ref_sync_page_username_hint), username)
    private val password = SyncFields.field(activity, activity.getString(R.string.ref_sync_page_password), secret = true)
    private val agreement = CheckBox(activity).apply {
        isChecked = false
        buttonDrawable = activity.getDrawable(R.drawable.dialog_checkbox); buttonTintList = null
        minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
    }
    private val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private lateinit var agreementCaption: TextView
    val dialog = BrowserDialog(activity, activity.getString(R.string.ref_sync_page_login)).widthDp(384)

    init {
        SyncFields.append(body, user)
        SyncFields.append(body, password)
        user.nextFocusForwardId = password.id; password.imeOptions = EditorInfo.IME_ACTION_DONE
        val caption = NativeTypography.text(activity).apply {
            val terms = activity.getString(R.string.ref_sync_page_terms)
            val privacy = activity.getString(R.string.ref_sync_page_privacy)
            val text = activity.getString(R.string.ref_sync_page_agreement, terms, privacy)
            val spans = SpannableString(text)
            fun link(label: String, url: String) {
                val start = text.indexOf(label)
                spans.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) = SyncPublicLinks.open(activity, url)
                }, start, start + label.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            link(terms, SyncPublicLinks.terms(server)); link(privacy, SyncPublicLinks.privacy(server))
            this.text = spans; textSize = 12f; setTextColor(activity.palette.secondary)
            movementMethod = LinkMovementMethod.getInstance(); setLinkTextColor(activity.palette.accent)
            setPadding(activity.dp(8), 0, activity.dp(16), 0)
            setOnClickListener { if (selectionStart == selectionEnd) agreement.isChecked = !agreement.isChecked }
        }
        agreementCaption = caption
        body.addView(LinearLayout(activity).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(activity.dp(16), activity.dp(8), 0, activity.dp(12))
            addView(agreement, LinearLayout.LayoutParams(activity.dp(20), activity.dp(24)))
            addView(caption, LinearLayout.LayoutParams(0, -2, 1f))
        })
        dialog.custom(body).negative(activity.getString(android.R.string.cancel)).positive(activity.getString(android.R.string.ok)) { confirm() }
        password.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) { if (confirm()) dialog.dismiss(); true } else false
        }
    }
    private fun confirm(): Boolean {
        if (!agreement.isChecked) {
            moe.pure.browser.ui.editor.NativeFieldFeedback.shake(agreement)
            moe.pure.browser.ui.editor.NativeFieldFeedback.shake(agreementCaption)
            moe.pure.browser.ui.message.BrowserMessages.show(activity, activity.getString(R.string.ref_sync_page_agreement,
                activity.getString(R.string.ref_sync_page_terms), activity.getString(R.string.ref_sync_page_privacy)))
            return false
        }
        if (user.text.isEmpty()) { moe.pure.browser.ui.editor.NativeFieldFeedback.required(user); return false }
        if (password.text.isEmpty()) { moe.pure.browser.ui.editor.NativeFieldFeedback.required(password); return false }
        submitted(user.text.toString(), password.text.toString())
        return true
    }
}
