package moe.pure.browser.feature.scripts.model

import java.util.Locale

/** The reference treats include and match entries as case-insensitive globs or slash regexes. */
object ScriptUrlMatcher {
    fun matches(pattern: String, url: String): Boolean {
        if (pattern.isEmpty()) return true
        val rule = pattern.lowercase(Locale.ROOT); val target = url.lowercase(Locale.ROOT)
        if (rule.length >= 2 && rule.startsWith('/') && rule.endsWith('/')) {
            val body = rule.substring(1, rule.length - 1)
            val expression = (if (rule.getOrNull(1) == '^') "" else ".*") + body + if (rule.getOrNull(rule.length - 2) == '$') "" else ".*"
            return runCatching { Regex(expression).matches(target) }.getOrDefault(false)
        }
        if (glob(rule, target)) return true
        val wildcard = rule.indexOf("://*.")
        return wildcard in 1..9 && glob(rule.substring(0, wildcard) + "://" + rule.substring(wildcard + 5), target)
    }
    private fun glob(rule: String, target: String): Boolean {
        var input = 0; var token = 0; var star = -1; var retry = -1
        while (input < target.length) when {
            token < rule.length && rule[token] == target[input] -> { input++; token++ }
            token < rule.length && rule[token] == '*' -> { star = token++; retry = input }
            star >= 0 -> { token = star + 1; input = ++retry }
            else -> return false
        }
        while (token < rule.length && rule[token] == '*') token++
        return token == rule.length
    }
}
