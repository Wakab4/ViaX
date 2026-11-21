package moe.pure.browser.feature.scripts.data

import java.io.File

internal enum class ScriptResourceFetch(val success: Boolean) {
    REUSED(true), DOWNLOADED(true), FAILED(false)
}

/** p5.b.W keeps the original inverted age branch and temp-file replacement exits. */
internal object NativeScriptResourceDownload {
    fun fetch(url: String, target: File?): ScriptResourceFetch {
        if (target == null) return ScriptResourceFetch.FAILED
        if (target.exists() && target.length() > 0 && target.lastModified() < System.currentTimeMillis() - 1_296_000_000L)
            return ScriptResourceFetch.REUSED
        val staging = File(target.parent, target.name + ".tmp")
        if (!NativeScriptResourceHttp.download(url, staging)) {
            ScriptResourceFileStore.delete(staging)
            return ScriptResourceFetch.FAILED
        }
        ScriptResourceFileStore.delete(target)
        return if (staging.renameTo(target)) ScriptResourceFetch.DOWNLOADED else ScriptResourceFetch.FAILED
    }
}
