package moe.via.browser.feature.proxy

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import moe.via.browser.R
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.settings.NativeSettingViews
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.theme.palette

class ProxySettingsFragment : SettingsRecordPage() {
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val settings = BrowserProxy.configuration()
        val endpoint = settings.endpoint
        var protocol = endpoint?.protocol ?: ProxyProtocol.HTTP
        val page = PageScaffold(activity, getString(R.string.proxy_title), ::closePage, appliesWindowInsets)
        val enable = NativeSettingViews.Global(activity).apply {
            title.setText(R.string.proxy_enable)
            detail.setText(R.string.proxy_scope)
            check.isChecked = settings.enabled
            setOnClickListener { check.isChecked = !check.isChecked }
        }
        page.body.addView(enable, LinearLayout.LayoutParams(-1, -2))
        val protocolRow = NativeSettingViews.site(activity).apply {
            (getChildAt(0) as android.widget.TextView).setText(R.string.proxy_type)
        }
        val protocolLabel = (protocolRow.getChildAt(1) as android.widget.TextView).apply {
            text = protocol.name
            visibility = View.VISIBLE
        }
        protocolRow.setOnClickListener {
            activity.dialogScope.show(BrowserDialog(activity, getString(R.string.proxy_type))
                .singleChoice(ProxyProtocol.entries.map { it.name }, protocol.ordinal) { index ->
                    protocol = ProxyProtocol.entries[index]
                    protocolLabel.text = protocol.name
                })
        }
        page.body.addView(protocolRow, LinearLayout.LayoutParams(-1, -2))
        val form = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(20))
        }
        page.body.addView(form, LinearLayout.LayoutParams(-1, -2))
        val host = field(form, R.string.proxy_host, endpoint?.host.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        val port = field(form, R.string.proxy_port, endpoint?.port?.toString().orEmpty(), InputType.TYPE_CLASS_NUMBER)
        val username = field(form, R.string.proxy_username, endpoint?.username.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        val password = field(form, R.string.proxy_password, endpoint?.password.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        form.addView(NativeSettingViews.label(activity, detail = true).apply {
            setText(R.string.proxy_auth_help)
            setPadding(0, dp(12), 0, 0)
        })

        lateinit var save: View
        save = page.toolbar.addTextAction(getString(R.string.ref_ua_save)) {
            val number = port.text.toString().toIntOrNull()
            if (number == null || number !in 1..65535) {
                port.error = getString(R.string.proxy_invalid_port)
                return@addTextAction
            }
            if (host.text.isNullOrBlank()) {
                host.error = getString(R.string.proxy_host_required)
                return@addTextAction
            }
            val proxy = try {
                ProxyEndpoint.create(protocol, host.text.toString(), number, username.text.toString(), password.text.toString())
            } catch (_: IllegalArgumentException) {
                BrowserMessages.show(activity, getString(R.string.proxy_invalid_settings))
                return@addTextAction
            }
            val owner = view ?: return@addTextAction
            save.isEnabled = false
            BrowserProxy.configure(proxy, enable.check.isChecked) { result ->
                if (view === owner) {
                    save.isEnabled = true
                    if (result == ProxyChange.APPLIED) closePage()
                    else BrowserMessages.show(activity, getString(result.messageResource()))
                }
            }
        }
        page.body.isFocusableInTouchMode = true
        page.body.defaultFocusHighlightEnabled = false
        page.body.requestFocus()
        return page.root
    }

    private fun field(form: LinearLayout, label: Int, value: String, input: Int): EditText {
        val context = form.context
        val id = View.generateViewId()
        form.addView(NativeSettingViews.label(context, detail = true).apply {
            labelFor = id
            setText(label)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(12) })
        return NativeTypography.edit(context).apply {
            this.id = id
            setSingleLine(true)
            inputType = input
            setTextColor(context.palette.text)
            setText(value)
            isSaveEnabled = false
            form.addView(this, LinearLayout.LayoutParams(-1, context.dp(46)))
        }
    }
}

internal fun ProxyChange.messageResource(): Int = when (this) {
    ProxyChange.UNSUPPORTED -> R.string.proxy_unsupported
    ProxyChange.BUSY -> R.string.proxy_busy
    ProxyChange.FAILED -> R.string.proxy_apply_failed
    ProxyChange.APPLIED -> R.string.proxy_saved
}
