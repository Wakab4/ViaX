package moe.via.browser.feature.ai.prompt

import moe.via.browser.feature.ai.model.AiPageAttachment
import moe.via.browser.feature.ai.model.AiPrompt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AiPromptVariable(val token: String, val description: String)

object AiPromptVariables {
    const val PAGE_URL = "{{webpage_url}}"
    const val PAGE_TITLE = "{{webpage_title}}"
    const val PAGE_CONTENT = "{{webpage_content}}"
    const val INPUT = "{{input}}"
    const val TIME = "{{time}}"
    private val pageVariables = setOf(PAGE_URL, PAGE_TITLE, PAGE_CONTENT)
    private val contentWhitespace = Regex("[\\t\\n\\r]+")

    fun available(type: Int): List<AiPromptVariable> = buildList {
        if (type == AiPrompt.TYPE_SYSTEM) {
            add(AiPromptVariable(PAGE_URL, "当前网页的链接。"))
            add(AiPromptVariable(PAGE_TITLE, "当前网页的标题。"))
            add(AiPromptVariable(PAGE_CONTENT, "当前网页的纯文本内容。"))
        } else add(AiPromptVariable(INPUT, "输入框内容。"))
        add(AiPromptVariable(TIME, "当前时间 yyyy-MM-dd HH:mm:ss EEEE (ZZZZ)。"))
    }

    fun scan(content: String, type: Int): List<String> {
        val result = linkedSetOf<String>()
        var position = 0
        while (position < content.length) {
            var open = content.indexOf("{{", position)
            if (open < 0) break
            val close = content.indexOf("}}", open + 2)
            if (close < 0) break
            val nested = content.lastIndexOf("{{", close)
            if (nested > open) open = nested
            if (open + 2 < close) {
                val token = content.substring(open, close + 2)
                if (accepted(token, type)) result.add(token)
            }
            position = close + 2
        }
        return result.toList()
    }

    fun flags(required: List<String>): Int = if (required.any { it in pageVariables }) AiPrompt.REQUIRES_PAGE else 0

    fun prepare(prompt: AiPrompt): AiPrompt {
        val content = normalizeContent(prompt.content)
        val required = scan(content, prompt.type)
        return prompt.copy(
            name = normalizeName(prompt.name), content = content, required = required,
            flags = flags(required), updatedAt = System.currentTimeMillis()
        )
    }

    fun normalizeContent(content: String): String = contentWhitespace.replace(content, " ").trim { it <= ' ' }

    fun normalizeName(name: String): String = buildString {
        name.forEach { char ->
            when {
                char == '\r' -> Unit
                char == '\u00a0' || Character.isWhitespace(char) -> append(' ')
                else -> append(char)
            }
        }
    }.trim { it <= ' ' }

    fun expand(prompt: AiPrompt, attachment: AiPageAttachment? = null, input: String? = null): String {
        var content = prompt.content
        prompt.required.forEach { token -> content = content.replace(token, resolve(token, attachment, input).orEmpty()) }
        return content
    }

    fun system(prompt: AiPrompt?, attachment: AiPageAttachment? = null): String =
        if (prompt == null || prompt.id == AiPrompt.DEFAULT_ID) AiDefaultPrompts.system(attachment)
        else expand(prompt, attachment)

    fun message(prompt: AiPrompt?, input: String): String = when {
        prompt == null || prompt.id == AiPrompt.DEFAULT_ID -> input
        prompt.required.isEmpty() -> prompt.content + "\n" + input
        else -> expand(prompt, input = input)
    }

    fun resolve(token: String, attachment: AiPageAttachment? = null, input: String? = null): String? = when (token) {
        PAGE_URL -> attachment?.url
        PAGE_TITLE -> attachment?.title
        PAGE_CONTENT -> attachment?.content
        INPUT -> input
        TIME -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss EEEE (ZZZZ)", Locale.getDefault()).format(Date())
        else -> null
    }

    private fun accepted(token: String, type: Int): Boolean {
        if (' ' in token) return false
        // Via lists time in the editor but does not store it among required variables.
        return if (type == AiPrompt.TYPE_SYSTEM) token in pageVariables else token == INPUT
    }
}
