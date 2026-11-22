package moe.via.browser.feature.internalpages.render

/** u9.d.h/e/b and z8.v0.c; history titles use the original tag-removal rule. */
object GeneratedPageMarkup {
    private val tags = Regex("<.*?>")

    fun head(title: String?, extraHead: String? = null): String = buildString {
        append("<!DOCTYPE html><html><head><meta content=\"text/html; charset=utf-8\" http-equiv=\"Content-Type\"/>")
        append("<meta name=\"color-scheme\" content=\"light dark\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, user-scalable=no, minimal-ui\">")
        if (!title.isNullOrEmpty()) append("<title>").append(title).append("</title>")
        append(extraHead.orEmpty())
        append("</head>")
    }

    fun hint(value: String): String = "<div class='hint'>${value.replace("\n", "<br>")}</div>"

    fun row(url: String, title: String, icon: String? = null): String {
        val stripped = tags.replace(title, "")
        val marker = if (icon.isNullOrEmpty()) "" else "<div class=\"icon_handle\"><div class=\"$icon icon\"></div></div>"
        return "<div class=\"box\">$marker" +
            "<a href=\"${urlEntities(url)}\" title=\"$stripped\"></a><p class=\"title\">$stripped</p></div>"
    }

    fun urlEntities(value: String): String = buildString(maxOf(16, value.length)) {
        for (char in value) {
            if (char.code > 127 || char == '"' || char == '\'' || char == '<' || char == '>' || char == '&')
                append("&#").append(char.code).append(';')
            else append(char)
        }
    }
}
