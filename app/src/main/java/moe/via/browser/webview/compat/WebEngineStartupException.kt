package moe.via.browser.webview.compat

/** Separates provider construction failure from page loading and feature errors. */
class WebEngineStartupException(cause: Throwable) : RuntimeException("WebView initialization failed", cause)
