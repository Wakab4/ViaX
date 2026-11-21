package moe.pure.browser.feature.scripts.model

import android.util.Base64
import org.json.JSONObject
import java.util.Scanner

/** b0.s/ia.d.c converts the current via.addon payload, not a Pure prototype database format. */
internal object NativeAddonParser {
    fun parse(encoded: String): UserScript? {
        if (encoded.length <= 10) return null
        return try {
            val item = JSONObject(String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8).trim { it <= ' ' })
            if (item.isNull("id") || item.isNull("code")) return null
            val patterns = item.optString("url")
            val encodedCode = item.optString("code")
            if (patterns.isEmpty() || encodedCode.isEmpty()) return null
            val id = item.optInt("id")
            val name = item.optString("name").ifEmpty { item.optString("info") }
                .ifEmpty { "Untitled Script - ${maxOf(0, id) + 10000}" }
            val body = String(Base64.decode(encodedCode, Base64.DEFAULT), Charsets.UTF_8)
            val script = buildString {
                append("// ==UserScript==\n// @name         ").append(name)
                append("\n// @namespace    https://viayoo.com/\n// @version      0.1\n")
                if (id > 0) append("// @homepageURL  https://app.viayoo.com/addons/").append(id).append('\n')
                item.optString("author").takeIf(String::isNotEmpty)?.let { append("// @author       ").append(it).append('\n') }
                append("// @run-at       document-end\n")
                patterns.split(',').forEach { raw ->
                    val pattern = raw.trim { it <= ' ' }
                    if (pattern.isNotEmpty()) {
                        append("// @match        ")
                        if (!pattern.startsWith('*')) append('*')
                        append(pattern)
                        if (!pattern.endsWith('*')) append('*')
                        append('\n')
                    }
                }
                append("// @grant        none\n// ==/UserScript==\n")
                var metadata = false
                Scanner(body).use { lines ->
                    while (lines.hasNextLine()) {
                        val text = lines.nextLine().trim { it <= ' ' }
                        when {
                            text.startsWith("// ==UserScript==") -> metadata = true
                            text.startsWith("// ==/UserScript==") -> metadata = false
                            !metadata -> append(text).append('\n')
                        }
                    }
                }
            }
            ScriptParser.parse(script)
        } catch (_: Exception) { null }
    }
}
