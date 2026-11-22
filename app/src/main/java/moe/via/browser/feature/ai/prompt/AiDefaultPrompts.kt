package moe.via.browser.feature.ai.prompt

import moe.via.browser.feature.ai.model.AiPageAttachment

object AiDefaultPrompts {
    const val GENERAL = "Act as an Expert Answerer. Follow these rules:\n\n" +
        "1. Concisely answer the core question within 3 sentences. Prioritize clarity.\n" +
        "2. Match the user's language (e.g., respond in Chinese if asked in Chinese).\n" +
        "3. Structure answers when answering professional content.\n" +
        "4. Omit polite filler, disclaimers, or redundant content.\n\nStay strictly on-task."

    private const val WEBPAGE = "You're a proficient AI assistant skilled in concise, on-point responses. Please follow the following principles:\n" +
        "1. Ground answers in the provided webpage, allowing minor thematic tangents but avoiding irrelevance. If info is missing, note gaps clearly.\n" +
        "2. Match the user's language ({{user_language}}) and tone precisely.\n" +
        "3. Prioritize clarity: lead with direct answers, add context only if critical."

    fun system(attachment: AiPageAttachment? = null): String {
        if (attachment == null) return GENERAL
        return buildString {
            append(WEBPAGE)
            append("\n\nThe webpage is as follows:\n")
            if (!attachment.url.isNullOrEmpty()) append("Link: ").append(attachment.url).append('\n')
            if (!attachment.title.isNullOrEmpty()) append("Title: ").append(attachment.title).append('\n')
            if (!attachment.content.isNullOrEmpty()) append("Content:\n").append(attachment.content)
        }
    }
}
