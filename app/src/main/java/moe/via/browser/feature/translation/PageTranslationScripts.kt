package moe.via.browser.feature.translation

import org.json.JSONObject

/** GP APK loaders only. The provider's remote JavaScript remains owned by its provider. */
object PageTranslationScripts {
    fun google(localeTag: String, token: String): String {
        val codes = quote(googleLanguages(localeTag).joinToString(","))
        val fallback = fallback(token)
        return """
            (function(){if(window.__via_translator__)window.__via_translator__.showBanner(!0);else{window.googleTranslateElementInit=function(){window.__via_translator__||(window.__via_translator__=new google.translate.TranslateElement({autoDisplay:!1,floatPosition:0,multilanguagePage:!0,includedLanguages:$codes,pageLanguage:"auto"}));window.__via_translator__.showBanner(!0)};var e=document.createElement("script");e.src="https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit";document.getElementsByTagName("head")[0].appendChild(e)}document.addEventListener("securitypolicyviolation",function e(n){$fallback;document.removeEventListener("securitypolicyviolation",e)})})();
        """.trimIndent()
    }

    fun edge(localeTag: String, token: String): String {
        val language = quote(edgeLanguage(localeTag))
        val fallback = fallback(token)
        return """
            (function(){var e="via-translate-bridge",t=document.getElementById(e);if(t)if("undefined"!=typeof translate)$language!=translate.to&&translate.changeLanguage($language);else{var n=document.createElement("script");n.id=e;n.src=t.src;n.onload=t.onload;t.parentNode.replaceChild(n,t)}else{(t=document.createElement("script")).id=e;t.src="https://fastly.jsdelivr.net/gh/xnx3/translate@v3.14.0/translate.js/translate.min.js";t.onload=function(){translate.selectLanguageTag.show=!1;translate.service.use("client.edge");translate.changeLanguage($language)};document.head.appendChild(t)}document.addEventListener("securitypolicyviolation",function e(t){$fallback;document.removeEventListener("securitypolicyviolation",e)})})();
        """.trimIndent()
    }

    fun googleLanguages(localeTag: String): List<String> {
        val codes = if (localeTag.startsWith("zh-")) {
            mutableListOf("en", "ar", "ja", "ko", "ru", "fr", "de", "es", "it", "pt", "zh-CN", "zh-TW")
        } else {
            mutableListOf("en", "ar", "az", "be", "bn", "cs", "da", "de", "es", "fa", "fil", "fr", "hi", "hr", "hu", "id", "in", "it", "js", "ko", "lt", "ne", "pl", "pt", "pt-BR", "ro", "ru", "sk", "sl", "ta", "th", "tr", "uk", "uz", "vi", "zh-CN", "zh-TW")
        }
        codes.remove(localeTag)
        codes.sort()
        codes.add(0, localeTag)
        return codes
    }

    fun edgeLanguage(localeTag: String): String = if (localeTag.startsWith("zh-")) {
        if (localeTag.endsWith("TW") || localeTag.endsWith("HK")) "chinese_traditional" else "chinese_simplified"
    } else "english"

    private fun fallback(token: String): String = if (token.isEmpty()) "" else
        "if(window.ViaTranslation&&typeof window.ViaTranslation.fallback===\"function\")window.ViaTranslation.fallback(${quote(token)})"

    private fun quote(value: String): String = JSONObject.quote(value).replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
}
