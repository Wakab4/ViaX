package moe.via.browser.feature.dns

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.via.browser.R
import moe.via.browser.feature.proxy.BrowserProxy
import moe.via.browser.feature.proxy.ProxyChange
import moe.via.browser.feature.proxy.messageResource
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.settings.NativeSettingViews
import moe.via.browser.ui.theme.ThemedActivity
import java.util.UUID

class WebDnsSettingsFragment : SettingsRecordPage() {
    private var page: PageScaffold? = null

    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        return PageScaffold(activity, getString(R.string.web_dns_title), ::closePage, appliesWindowInsets).also {
            page = it
            it.addTextAction(getString(R.string.web_dns_add)) { editor(BrowserProxy.dnsConfiguration()) }
            render(it, activity, BrowserProxy.dnsConfiguration())
        }.root
    }

    private fun render(page: PageScaffold, activity: ThemedActivity, settings: WebDnsSettings) {
        page.body.removeAllViews()
        if (BrowserProxy.configuration().enabled) {
            page.body.addView(NativeSettingViews.section(activity).apply { setText(R.string.web_dns_proxy) })
        }
        page.body.addView(NativeSettingViews.Radio(activity).apply {
            title.setText(R.string.settings_follow_system)
            detail.visibility = View.GONE
            check.isChecked = settings.selected == null
            setOnClickListener {
                if (settings.selected != null) update(settings.copy(selectedId = null))
            }
        })
        for (server in settings.servers) {
            page.body.addView(NativeSettingViews.Radio(activity).apply {
                title.text = server.name
                detail.text = server.url.toString()
                check.isChecked = server.id == settings.selectedId
                setOnClickListener {
                    if (!check.isChecked) update(settings.copy(selectedId = server.id))
                }
                if (server in settings.custom) setOnLongClickListener {
                    editor(settings, server)
                    true
                }
            })
        }
    }

    private fun editor(settings: WebDnsSettings, existing: DohServer? = null) {
        val activity = requireActivity() as ThemedActivity
        val title = if (existing == null) R.string.web_dns_add else R.string.web_dns_edit
        val dialog = BrowserDialog(activity, getString(title))
        val name = dialog.field(existing?.name.orEmpty(), getString(R.string.web_dns_name))
        val address = dialog.field(existing?.url?.toString().orEmpty(), getString(R.string.web_dns_url)).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        dialog.positive(getString(R.string.ref_ua_save)) {
            val url = DohServer.url(address.text.toString())
            if (url == null) {
                address.error = getString(R.string.web_dns_invalid_url)
                return@positive false
            }
            val bootstrap = existing?.takeIf { it.url.host == url.host }?.bootstrap.orEmpty()
            val server = DohServer(existing?.id ?: UUID.randomUUID().toString(),
                name.text.toString().trim().ifEmpty { url.host }, url, bootstrap)
            val custom = if (existing == null) settings.custom + server
                else settings.custom.map { if (it.id == server.id) server else it }
            update(settings.copy(custom = custom))
            true
        }.negative()
        if (existing != null) {
            dialog.neutral(getString(R.string.web_dns_delete)) {
                update(settings.copy(
                    selectedId = settings.selectedId.takeUnless { it == existing.id },
                    custom = settings.custom.filterNot { it.id == existing.id },
                ))
                true
            }
        }
        activity.dialogScope.show(dialog)
    }

    private fun update(settings: WebDnsSettings) {
        val owner = page ?: return
        val activity = requireActivity() as ThemedActivity
        BrowserProxy.configureDns(settings) { result ->
            if (page !== owner) return@configureDns
            when (result) {
                ProxyChange.APPLIED -> render(owner, activity, settings)
                ProxyChange.UNSUPPORTED -> BrowserMessages.show(activity, getString(R.string.web_dns_unsupported))
                ProxyChange.FAILED -> BrowserMessages.show(activity, getString(R.string.web_dns_apply_failed))
                ProxyChange.BUSY -> BrowserMessages.show(activity, getString(result.messageResource()))
            }
        }
    }

    override fun onDestroyView() {
        page = null
        super.onDestroyView()
    }
}
