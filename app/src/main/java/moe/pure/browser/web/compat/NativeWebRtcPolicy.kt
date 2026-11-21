package moe.pure.browser.web.compat

import android.webkit.WebView

/** e8.y0.o deletes peer-connection constructors at page start, without changing media APIs. */
internal object NativeWebRtcPolicy {
    fun started(view: WebView, url: String, disabled: Boolean) {
        if (disabled && url.startsWith("http")) view.evaluateJavascript(
            "javascript:(function(){var a=window;try{delete a.RTCPeerConnection,delete a.webkitRTCPeerConnection,delete a.mozRTCPeerConnection}catch(b){}})();", null)
    }
}
