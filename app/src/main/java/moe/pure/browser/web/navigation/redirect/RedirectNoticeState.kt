package moe.pure.browser.web.navigation.redirect

/** A Native short-message action targets the selected tab ID captured when the message was requested. */
internal data class RedirectNoticeState(val selectedTabId: String?, val targetUrl: String, val targetDomain: String, val expiresAt: Long)
