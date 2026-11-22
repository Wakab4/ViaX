package moe.via.browser.feature.media.web

import android.content.Context
import android.webkit.WebView
import moe.via.browser.feature.media.model.MediaMetadata
import org.json.JSONTokener

/** GP uc's commands execute in the original document; no second media engine is introduced. */
internal class MediaPageController(context: Context, private val webView: WebView) {
    private val findVideo = context.assets.open("media/video.js").bufferedReader().use { it.readText() }
    private var epoch = 0
    private var disposed = false

    fun metadata(callback: (MediaMetadata) -> Unit) {
        evaluate("var a=findVideoElement();return a?\"VIDEO\"!=a.tagName?\"0,1,16,9\":a.duration+\",\"+a.playbackRate+\",\"+a.videoWidth+\",\"+a.videoHeight:\"\";") { value ->
            val pieces = value.split(',')
            if (pieces.size <= 2) { callback(MediaMetadata()); return@evaluate }
            val duration = if (pieces[0].equals("Infinity", true)) -1f else pieces[0].toFloatOrNull() ?: 0f
            callback(MediaMetadata(duration, pieces.getOrNull(1)?.toFloatOrNull()?.takeIf { it.isFinite() && it > 0 } ?: 1f,
                pieces.getOrNull(2)?.toIntOrNull() ?: 16, pieces.getOrNull(3)?.toIntOrNull() ?: 9, available = true))
        }
    }

    /** Native 0 unknown, 1 paused, 2 playing. */
    fun playing(callback: (Int) -> Unit) = evaluate("var a=findVideoElement();return a&&\"VIDEO\"==a.tagName?a.paused?1:2:0;") { callback(it.toIntOrNull() ?: 0) }

    fun toggle(callback: (Int) -> Unit) = evaluate("var a=findVideoElement();if(a)try{var b=!a.paused;b?a.pause():a.play();return b?1:2}catch(c){}return 0;") { callback(it.toIntOrNull() ?: 0) }

    fun seek(deltaSeconds: Int) {
        evaluate("var b=findVideoElement();if(b)try{var a=${deltaSeconds}+b.currentTime,c=b.duration;0>a?a=0:a>c&&(a=c);b.currentTime=a}catch(d){}") {}
    }

    fun speed(rate: Float) {
        if (rate.isFinite() && rate > 0f) evaluate("var a=findVideoElement();if(a)try{a.playbackRate=$rate}catch(b){}") {}
    }

    private fun evaluate(command: String, callback: (String) -> Unit) {
        if (disposed || !webView.settings.javaScriptEnabled) return
        val expected = epoch
        runCatching {
            webView.evaluateJavascript("(function(){$findVideo$command})();") { json ->
                if (!disposed && expected == epoch) callback(decode(json))
            }
        }.onFailure { if (!disposed && expected == epoch) callback("") }
    }

    fun dispose() { disposed = true; epoch++ }

    companion object {
        private fun decode(json: String?): String = runCatching {
            val value = JSONTokener(json ?: "null").nextValue()
            if (value == null || value == org.json.JSONObject.NULL) "" else value.toString()
        }.getOrDefault("")
    }
}
