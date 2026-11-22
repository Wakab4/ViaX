package moe.via.browser.feature.adblock.rules

/** b5.c.v selects element syntax before trying a request rule. */
internal sealed interface FilterRule {
    data class Network(val value: NetworkRule) : FilterRule
    data class Element(val value: ElementRule) : FilterRule

    companion object {
        fun parse(raw: String): FilterRule? {
            if (raw.isEmpty() || raw[0] == '!') return null
            return if ("##" in raw || "#@#" in raw) ElementRule.parse(raw)?.let { Element(it) }
            else NetworkRule.parse(raw)?.let { Network(it) }
        }
    }
}
