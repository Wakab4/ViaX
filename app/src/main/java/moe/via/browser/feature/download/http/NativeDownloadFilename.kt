package moe.via.browser.feature.download.http

import moe.via.browser.core.mime.NativeMimeTypes

import java.io.File
import java.net.URLDecoder

/** l5.b.b/e: Native disposition priority and conditional percent decoding, including its ordering. */
internal object NativeDownloadFilename {
    fun guess(url: String?, disposition: String?, mime: String?): String {
        var name = disposition?.let(::dispositionName)
        val fromDisposition = name != null
        if (name == null && url != null) {
            val query = url.indexOf('?')
            val clean = if (query > 0) url.substring(0, query) else url
            val start = clean.lastIndexOf('/') + 1
            if (!clean.endsWith('/') && start > 0) name = clean.substring(start)
        }
        if (name != null) {
            if ('%' in name) name = decode(name)
            name = name.replace(File.separatorChar, '_')
        }
        name = name ?: "downloadfile"
        val dot = name.lastIndexOf('.')
        var suffix: String
        if (dot < 0) {
            val extension = NativeMimeTypes.extension(mime)
            suffix = if (extension != null) ".$extension" else if (mime?.startsWith("image/") == true) ".png" else ""
            if (suffix.equals(".bin", true)) suffix = ""
        } else {
            suffix = name.substring(dot)
            name = name.substring(0, dot)
            if (!fromDisposition && !NativeMimeTypes.recognized(suffix.substring(1)) && mime != null) {
                val extension = NativeMimeTypes.extension(mime)
                if (extension != null && !extension.equals("bin", true) && !extension.equals(suffix.substring(1), true)) suffix = ".$extension"
            }
        }
        return name + suffix
    }
    private fun dispositionName(value: String): String? {
        if (value.isEmpty()) return null
        val start = value.indexOf("filename*=").takeIf { it >= 0 } ?: value.indexOf("filename")
        if (start < 0) return null
        var name = value.substring(start)
        while (name.lastIndexOf(';') > 0) name = name.substring(0, name.lastIndexOf(';')).trim { it <= ' ' }
        val equals = name.indexOf('=')
        if (equals > 0) {
            name = name.substring(equals + 1).trim { it <= ' ' }
            val encoding = name.indexOf("''")
            if (encoding > 0) name = name.substring(encoding + 2).trim { it <= ' ' }
            val spaced = name.indexOf("' '")
            if (spaced > 0) name = name.substring(spaced + 3).trim { it <= ' ' }
        }
        if (name.isEmpty()) return null
        if (name[0] == '"' || name[0] == '\'') name = name.substring(1)
        name = name.trimEnd('"', '\'')
        if (name.isEmpty()) return null
        return if (name.indexOf('%') > 0) decode(name) else name
    }
    private fun decode(value: String) = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}
