package moe.via.browser.feature.adblock.rules

/** v4.b stores selector arrays and uses hash sets while rebuilding or changing one rule. */
class ElementRuleIndex(private val chromiumMajor: Int) {
    private val scoped = hashMapOf<String, Array<String>>()
    private var building: MutableMap<String, MutableSet<String>>? = null

    fun clear() = scoped.clear()
    fun begin() {
        if (building != null) return
        building = hashMapOf<String, MutableSet<String>>().apply {
            scoped.forEach { (domain, selectors) -> put(domain, HashSet(selectors.asList())) }
        }
        scoped.clear()
    }
    fun finish() {
        val rules = building ?: return
        if (rules.isEmpty()) return
        rules.forEach { (domain, selectors) -> scoped[domain] = selectors.toTypedArray() }
        rules.clear()
        building = null
    }
    fun add(rule: ElementRule) {
        if (rule.needsHas && chromiumMajor < 105) return
        val domains = rule.domains
        if (domains.isNullOrEmpty()) add(rule.selector, "")
        else domains.filter { it.length > 2 }.forEach { add(rule.selector, it) }
    }
    private fun add(selector: String, domain: String) {
        val pending = building
        val selectors = if (pending != null) pending.getOrPut(domain) { HashSet() }
            else HashSet(scoped[domain]?.asList() ?: emptyList())
        selectors.add(selector)
        if (pending == null) scoped[domain] = selectors.toTypedArray()
    }
    fun remove(rule: ElementRule) {
        val domains = rule.domains
        if (domains.isNullOrEmpty()) remove(rule.selector, "")
        else for (domain in domains) {
            if (domain.length <= 2) return
            remove(rule.selector, domain)
        }
    }
    private fun remove(selector: String, domain: String) {
        val pending = building
        val selectors = if (pending != null) pending[domain] ?: return else HashSet(scoped[domain]?.asList() ?: return)
        selectors.remove(selector)
        if (selectors.isEmpty()) {
            if (pending != null) pending.remove(domain) else scoped.remove(domain)
        } else if (pending == null) scoped[domain] = selectors.toTypedArray()
    }
    fun selectors(host: String): Set<String> {
        val base = FilterUrls.baseDomain(host)
        val specific = HashSet<String>()
        val global = HashSet<String>()
        scoped[host]?.let(specific::addAll)
        if (base != host) scoped[base]?.let(specific::addAll)
        scoped[""]?.let(global::addAll)
        scoped["~$host"]?.let { specific.removeAll(it.toSet()); global.removeAll(it.toSet()) }
        if (base != host) scoped["~$base"]?.let { specific.removeAll(it.toSet()); global.removeAll(it.toSet()) }
        if (specific.isEmpty()) return global
        if (global.isEmpty()) return specific
        return linkedSetOf<String>().apply { addAll(specific); addAll(global) }
    }
    fun css(url: String): String {
        val host = FilterUrls.host(url)?.takeIf(String::isNotEmpty) ?: return ""
        return selectors(host).chunked(100).joinToString("") { it.joinToString(",") + "{display:none!important}\n" }
    }
}
