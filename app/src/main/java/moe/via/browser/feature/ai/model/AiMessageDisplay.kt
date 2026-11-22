package moe.via.browser.feature.ai.model

/** c9.a0/d0/g separate pre-rendered display text from database messages. */
internal sealed interface AiMessageDisplay { val id: String?; val text: CharSequence? }
internal class AiUserMessageDisplay(override val id: String?, override val text: CharSequence?) : AiMessageDisplay
internal class AiAssistantMessageDisplay(override val id: String?, override var text: CharSequence?) : AiMessageDisplay
internal data class AiPromptChoice(val prompt: AiPrompt, val selected: Boolean)
