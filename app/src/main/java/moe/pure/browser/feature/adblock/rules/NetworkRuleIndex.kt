package moe.pure.browser.feature.adblock.rules

/** v4.c accumulates bulk rules, then supports individual additions and removals. */
class NetworkRuleIndex {
    private val domains = hashSetOf<String>()
    private val block = hashMapOf<String, NetworkRule>()
    private val allow = hashMapOf<String, NetworkRule>()
    private val pageBlock = hashMapOf<String, NetworkRule>()
    private val pageAllow = hashMapOf<String, NetworkRule>()
    private var pending: MutableSet<NetworkRule>? = null

    fun clear() { domains.clear(); block.clear(); allow.clear(); pageBlock.clear(); pageAllow.clear() }
    fun begin() {
        if (pending != null) return
        pending = linkedSetOf<NetworkRule>().apply {
            addAll(allow.values); addAll(block.values); addAll(pageAllow.values); addAll(pageBlock.values)
        }
        allow.clear(); block.clear(); pageAllow.clear(); pageBlock.clear()
    }
    fun finish() {
        val rules = pending ?: return
        for (rule in rules) {
            val target = if (rule.exception) allow else block
            val token = FilterPattern.tokens(rule.pattern).firstOrNull { it !in target }
            if (token != null) target[token] = rule else scoped(rule, remove = false)
        }
        rules.clear()
        pending = null
    }
    fun add(rule: NetworkRule) {
        rule.fastDomain()?.let { domains.add(it); return }
        pending?.let { it.add(rule); return }
        val tokens = FilterPattern.tokens(rule.pattern).toList()
        if (tokens.isEmpty()) { scoped(rule, remove = false); return }
        val target = if (rule.exception) allow else block
        if (tokens.any { target[it] == rule }) return
        tokens.firstOrNull { it !in target }?.let { target[it] = rule }
    }
    fun remove(rule: NetworkRule) {
        rule.fastDomain()?.let { domains.remove(it); return }
        pending?.let { it.remove(rule); return }
        val tokens = FilterPattern.tokens(rule.pattern).toList()
        if (tokens.isEmpty()) { scoped(rule, remove = true); return }
        val target = if (rule.exception) allow else block
        tokens.firstOrNull { target[it] == rule }?.let(target::remove)
    }
    private fun scoped(rule: NetworkRule, remove: Boolean) {
        if (rule.domains.isNullOrEmpty() || FilterPattern.tokens(rule.pattern).any()) return
        for (scope in rule.domains) {
            val value = rule.copy(domains = listOf(scope))
            val target = if (rule.exception) pageAllow else pageBlock
            val tokens = FilterPattern.tokens(scope).toList()
            if (remove) tokens.firstOrNull { target[it] == value }?.let(target::remove)
            else if (tokens.none { target[it] == value }) tokens.firstOrNull { it !in target }?.let { target[it] = value }
        }
    }
    fun match(url: String, origin: String?, types: Int): NetworkRule? {
        val host = FilterUrls.host(url)
        val fast = host?.let { if (it in domains) it else FilterUrls.baseDomain(it).takeIf(domains::contains) }
        var found = fast?.let { NetworkRule("|$it^") }
        // The reference skips indexed blocking rules when it has no indexed exception rules.
        if (allow.isEmpty() && pageAllow.isEmpty()) return found
        val pageHost = FilterUrls.host(origin)
        val thirdParty = FilterUrls.thirdParty(host, pageHost)
        val start = url.indexOf("://").let { if (it < 0) 0 else it + 3 }
        var candidates = 0
        for (index in start..url.length - 5) {
            if (candidates >= 32) break
            val token = url.substring(index, index + 5)
            allow[token]?.takeIf { it.matches(url, pageHost, types, thirdParty) }?.let { return it }
            if (found == null) block[token]?.let { rule ->
                candidates++
                if (rule.matches(url, pageHost, types, thirdParty)) found = rule
            }
            if (found != null && allow.isEmpty()) break
        }
        if (pageHost != null) {
            candidates = 0
            for (index in 0..pageHost.length - 5) {
                if (candidates >= 32) break
                val token = pageHost.substring(index, index + 5)
                pageAllow[token]?.takeIf { it.matches(url, pageHost, types, thirdParty) }?.let { return it }
                if (found == null) pageBlock[token]?.let { rule ->
                    candidates++
                    if (rule.matches(url, pageHost, types, thirdParty)) found = rule
                }
                if (found != null && pageAllow.isEmpty()) break
            }
        }
        return found
    }
}
