package moe.via.browser.feature.adblock.rules

/** x4.a owns mutable request and element indexes with an explicit bulk loading phase. */
internal class FilterRuleSet(chromiumMajor: Int) {
    private val network = NetworkRuleIndex()
    private val elements = ElementRuleIndex(chromiumMajor)

    fun begin() {
        network.clear()
        network.begin()
        elements.clear()
        elements.begin()
    }
    fun finish() { network.finish(); elements.finish() }
    fun load(rules: List<FilterRule>) = rules.forEach(::add)
    fun add(rule: FilterRule) = when (rule) {
        is FilterRule.Network -> network.add(rule.value)
        is FilterRule.Element -> elements.add(rule.value)
    }
    fun remove(rule: FilterRule) = when (rule) {
        is FilterRule.Network -> network.remove(rule.value)
        is FilterRule.Element -> elements.remove(rule.value)
    }
    fun match(url: String, origin: String?, types: Int) = network.match(url, origin, types)
    fun css(url: String) = elements.css(url)
    companion object { fun valid(line: String) = FilterRule.parse(line) != null }
}
