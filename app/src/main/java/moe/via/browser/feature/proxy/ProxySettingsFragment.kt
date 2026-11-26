package moe.via.browser.feature.proxy

import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import moe.via.browser.R
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogField
import moe.via.browser.ui.editor.NativePasswordVisibility
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
        page.body.addView(protocolRow, LinearLayout.LayoutParams(-1, -2))
        val form = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(20))
        }
        page.body.addView(form, LinearLayout.LayoutParams(-1, -2))
        val host = field(form, R.string.proxy_host, endpoint?.host.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
            .apply {
                hint = SpannableString(context.getText(R.string.proxy_host_hint)).apply {
                    val size = resources.getDimensionPixelSize(R.dimen.ref_redirect_page_body_size)
                    setSpan(AbsoluteSizeSpan(size), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                setHintTextColor(activity.palette.hint)
            }
        val port = field(form, R.string.proxy_port, endpoint?.port?.toString().orEmpty(), InputType.TYPE_CLASS_NUMBER)
        val username = field(form, R.string.proxy_username, endpoint?.username.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        val password = field(form, R.string.proxy_password, endpoint?.password.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
            .apply {
                compoundDrawablePadding = dp(8)
                NativePasswordVisibility.attach(this)
            }

        fun selectProtocol(next: ProxyProtocol) {
            protocol = next
            protocolLabel.text = next.name
        }
        host.doAfterTextChanged { value ->
            val address = value.toString()
            ProxyProtocol.fromAddress(address)?.let(::selectProtocol)
            ProxyServerAddress.parse(address)?.port?.toString()?.takeUnless { it == port.text.toString() }?.let { port.setText(it) }
        }
        protocolRow.setOnClickListener {
            activity.dialogScope.show(BrowserDialog(activity, getString(R.string.proxy_type))
                .singleChoice(ProxyProtocol.entries.map { it.name }, protocol.ordinal) { index ->
                    val next = ProxyProtocol.entries[index]
                    selectProtocol(next)
                    val address = host.text.toString().trim()
                    if ("://" in address && ProxyProtocol.fromAddress(address) != next) {
                        host.setText("${next.scheme}://${address.substringAfter("://")}")
                    }
                })
        }

        page.toolbar.addTextAction(getString(R.string.proxy_import)) { importProxy(activity) }
        lateinit var save: View
        save = page.toolbar.addTextAction(getString(R.string.ref_ua_save)) {
            if (host.text.isNullOrBlank()) {
                host.error = getString(R.string.proxy_host_required)
                return@addTextAction
            }
            val address = ProxyServerAddress.parse(host.text.toString()) ?: run {
                host.error = getString(R.string.proxy_invalid_host)
                return@addTextAction
            }
            val portText = port.text.toString()
            val number = if (portText.isBlank()) address.port else portText.toIntOrNull()
            if (number == null || number !in 1..65535) {
                port.error = getString(R.string.proxy_invalid_port)
                return@addTextAction
            }
            if (address.port != null && address.port != number) {
                port.error = getString(R.string.proxy_port_mismatch)
                return@addTextAction
            }
            val selectedProtocol = address.protocol ?: protocol
            val proxy = try {
                ProxyEndpoint.create(selectedProtocol, address.host, number, username.text.toString(), password.text.toString())
            } catch (_: IllegalArgumentException) {
                BrowserMessages.show(activity, getString(if (selectedProtocol == ProxyProtocol.SOCKS5) {
                    R.string.proxy_invalid_socks_credentials
                } else R.string.proxy_invalid_basic_credentials))
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

    private fun importProxy(activity: ThemedActivity) {
        val owner = view ?: return
        val dialog = BrowserDialog(activity, getString(R.string.proxy_import_title))
        val link = dialog.field(hint = getString(R.string.proxy_import_hint)).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            isSaveEnabled = false
        }
        var applying = false
        dialog.positive(getString(R.string.proxy_import_enable)) {
            if (applying) return@positive false
            val endpoint = ProxyEndpoint.fromLink(link.text.toString())
            if (endpoint == null) {
                link.error = getString(R.string.proxy_import_invalid)
                return@positive false
            }
            applying = true
            BrowserProxy.configure(endpoint, enabled = true) { result ->
                applying = false
                if (view === owner) {
                    if (result == ProxyChange.APPLIED) {
                        dialog.dismiss()
                        closePage()
                        BrowserMessages.show(activity, getString(R.string.proxy_enabled))
                    } else {
                        BrowserMessages.show(activity, getString(result.messageResource()))
                    }
                }
            }
            false
        }.negative()
        activity.dialogScope.show(dialog)
    }

    private fun field(form: LinearLayout, label: Int, value: String, input: Int): EditText {
        val context = form.context
        val id = View.generateViewId()
        form.addView(NativeSettingViews.label(context, detail = true).apply {
            labelFor = id
            setText(label)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(12) })
        return DialogField.createEditor(context, value).apply {
            this.id = id
            setSingleLine(true)
            inputType = input
            NativeTypography.bind(this)
            gravity = Gravity.CENTER_VERTICAL
            setSelectAllOnFocus(false)
            setBackgroundResource(R.drawable.dialog_field_underline)
            setPadding(0, 0, 0, 0)
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
