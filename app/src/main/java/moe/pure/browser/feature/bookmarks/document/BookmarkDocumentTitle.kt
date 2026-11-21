package moe.pure.browser.feature.bookmarks.document

/** o9.a.p/o9.b.l read through g6.p.l; rendering does not rewrite legacy stored metadata. */
internal object BookmarkDocumentTitle {
    fun clean(value: String): String = buildString(value.length) {
        for (character in value) when {
            Character.isWhitespace(character) -> if (character != '\r') append(' ')
            character == '\u00a0' -> append(' ')
            else -> append(character)
        }
    }
}
