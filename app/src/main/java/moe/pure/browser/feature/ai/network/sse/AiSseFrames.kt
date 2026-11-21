package moe.pure.browser.feature.ai.network.sse

/** e4.e: only a blank separator dispatches; EOF never flushes a pending event. */
internal class AiSseFrames(private val event: (String, String) -> Unit,
    private val comment: (String) -> Unit, private val id: (String?) -> Unit, private val retry: (Long) -> Unit) {
    private var data = StringBuilder()
    private var eventId: String? = null
    private var type = "message"
    fun line(value: String) {
        if (value.trim { it <= ' ' }.isEmpty()) { dispatch(); return }
        if (value.startsWith(':')) { comment(value.substring(1).trim { it <= ' ' }); return }
        val separator = value.indexOf(':')
        if (separator < 0) field(value.trim { it <= ' ' }, "")
        else field(value.substring(0, separator), value.substring(separator + 1).removePrefix(" "))
    }
    private fun dispatch() {
        if (data.isEmpty()) return
        val text = data.toString().removeSuffix("\n")
        id(eventId); event(type, text)
        data = StringBuilder(); type = "message"
    }
    private fun field(name: String, value: String) { when (name) {
        "data" -> data.append(value).append('\n')
        "id" -> eventId = value
        "event" -> type = value
        "retry" -> if (digits.matches(value)) retry(value.toLong())
    } }
    companion object { private val digits = Regex("^[\\d]+$") }
}
