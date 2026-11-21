package moe.pure.browser.web.compat

/** Separates provider construction failure from page loading and feature errors. */
class WebEngineStartupException(cause: Throwable) : RuntimeException("WebView initialization failed", cause)
