package moe.via.browser.browser.page

import moe.via.browser.storage.preferences.BrowserPreferences
import androidx.webkit.CustomHeader
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewFeature

/** Only changes the three browser-controlled headers; other profile headers remain owned by their features. */
object ProfileHeaders {
    private var applied: List<Boolean>? = null
    fun apply(preferences: BrowserPreferences) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE) ||
            !WebViewFeature.isFeatureSupported(WebViewFeature.CUSTOM_REQUEST_HEADERS)) return
        val values = listOf(preferences.doNotTrack, preferences.saveData, preferences.globalPrivacyControl)
        if (applied == values) return
        val profile = ProfileStore.getInstance().getOrCreateProfile(Profile.DEFAULT_PROFILE_NAME)
        listOf("DNT", "Save-Data", "Sec-GPC").forEach(profile::clearCustomHeader)
        if (values[0]) profile.addCustomHeader(CustomHeader("DNT", "1", setOf("*")))
        if (values[1]) profile.addCustomHeader(CustomHeader("Save-Data", "on", setOf("*")))
        if (values[2]) profile.addCustomHeader(CustomHeader("Sec-GPC", "1", setOf("*")))
        applied = values
    }
}
