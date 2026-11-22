package moe.via.browser.feature.scripts.model

import java.net.URL
import java.net.URLDecoder
import java.util.Locale

object ScriptParser {
    private val directive = Regex("//[\\s\\p{Zs}\\u200B]*@([^\\s\\p{Zs}\\u200B]+)(?:[\\s\\p{Zs}\\u200B]+(.*))?")
    private val resource = Regex("(\\S+)\\s+(.*)")
    fun parse(content: String, source: String? = null, locale: Locale = Locale.getDefault()): UserScript? {
        if (content.isEmpty()) return null
        val parsedAt = System.currentTimeMillis()
        var name = source?.substringAfterLast('/')?.takeIf(String::isNotEmpty)?.replace(".user.js", "")?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        var namespace = source?.takeIf { "://" in it }?.substringAfter("://")?.substringBefore('/')
        var display: String? = null; var version: String? = null; var icon: String? = null; var home: String? = null; var support: String? = null
        var download = source; var runAt = 4; var flags = 0; var grant = 0; var started = false
        val matches = hashSetOf<String>(); val excludes = hashSetOf<String>(); val includes = hashSetOf<String>(); val excludeMatches = hashSetOf<String>()
        val requires = mutableListOf<String>(); val resources = hashMapOf<String, String>()
        for (line in content.lineSequence()) {
            val text = line.trim { it <= ' ' }
            if (!started) { if (text.startsWith("// ==UserScript==")) started = true; continue }
            if (text.startsWith("// ==/UserScript==")) {
                val originalName = name ?: return null; val originalNamespace = namespace ?: return null
                val key = ScriptIdentity.key(originalNamespace, originalName)
                return UserScript(key = key, name = display ?: originalName, content = content, version = version, matches = matches.toList(), includes = includes.toList(),
                    excludeMatches = excludeMatches.toList(), excludes = excludes.toList(), runAt = runAt, requires = requires, resources = resources,
                    flags = flags, grant = grant, iconUrl = icon, homepageUrl = home, supportUrl = support, downloadUrl = download,
                    updated = parsedAt, created = parsedAt)
            }
            val fields = directive.matchEntire(text) ?: continue
            val key = fields.groupValues[1]
            if (key == "unwrap") { flags = flags or 1; continue }
            if (key == "noframes") { flags = flags or 2; continue }
            val value = fields.groupValues[2].trim { it <= ' ' }.takeIf(String::isNotEmpty) ?: continue
            when (key) {
                "name" -> name = value
                "namespace" -> namespace = value
                "version" -> version = value
                "match" -> matches.add(value)
                "include" -> includes.add(value)
                "exclude" -> excludes.add(value)
                "exclude-match" -> excludeMatches.add(value)
                "run-at" -> runAt = when (value) { "document-start" -> 1; "document-end", "document-body" -> 2; "context-menu" -> 8; else -> 4 }
                "require" -> resolve(value, source)?.let(requires::add)
                "resource" -> resource.matchEntire(value)?.let { match -> resolve(match.groupValues[2], source)?.let { resources[match.groupValues[1]] = it } }
                "grant" -> grant = grant or (ScriptGrants.bits[value] ?: 0)
                "icon" -> icon = value
                "homepageURL" -> home = value
                "supportURL" -> support = value
                "downloadURL" -> download = value
                else -> if (key.startsWith("name:${locale.language}")) display = value
            }
        }
        return null
    }
    private fun resolve(value: String, source: String?): String? = if (value.startsWith("data:", true)) value else runCatching { URL(source?.let(::URL), value).toURI().toString() }.getOrNull()
}
