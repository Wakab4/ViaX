package moe.pure.browser.core.reactive

import java.io.PrintStream
import java.io.PrintWriter
import java.util.Collections
import java.util.IdentityHashMap

/** RxJava CompositeException's ordered causes and diagnostic formatting used by the native task consumers. */
internal class NativeCompositeException(vararg errors: Throwable?) : RuntimeException() {
    val exceptions: List<Throwable>
    private var overview: Throwable? = null
    init {
        val unique = LinkedHashSet<Throwable>()
        for (error in errors) when (error) {
            is NativeCompositeException -> unique.addAll(error.exceptions)
            null -> unique.add(NullPointerException("Throwable was null!"))
            else -> unique.add(error)
        }
        require(unique.isNotEmpty()) { "errors is empty" }
        exceptions = Collections.unmodifiableList(ArrayList(unique))
    }
    override val message: String get() = "${exceptions.size} exceptions occurred. "
    override val cause: Throwable
        @Synchronized get() {
            if (overview == null) {
                if (exceptions.size == 1) overview = exceptions[0]
                else {
                    val seen = IdentityHashMap<Throwable, Boolean>()
                    val line = System.getProperty("line.separator")!!
                    val text = StringBuilder("Multiple exceptions (${exceptions.size})").append(line)
                    for (error in exceptions) {
                        var value: Throwable? = error
                        var depth = 0
                        while (value != null) {
                            repeat(depth) { text.append("  ") }
                            text.append("|-- ").append(value.javaClass.canonicalName).append(": ")
                            val message = value.message
                            if (message == null || !message.contains(line)) text.append(message).append(line)
                            else {
                                text.append(line)
                                for (part in message.split(line.toRegex()).dropLastWhile { it.isEmpty() }) {
                                    repeat(depth + 2) { text.append("  ") }; text.append(part).append(line)
                                }
                            }
                            repeat(depth + 2) { text.append("  ") }
                            if (value.stackTrace.isNotEmpty()) text.append("at ").append(value.stackTrace[0]).append(line)
                            if (seen.containsKey(value)) {
                                val next = value.cause ?: break
                                repeat(depth + 2) { text.append("  ") }
                                text.append("|-- (cause not expanded again) ").append(next.javaClass.canonicalName)
                                    .append(": ").append(next.message).append(line)
                                break
                            }
                            seen[value] = true; value = value.cause; depth++
                        }
                    }
                    overview = ExceptionOverview(text.toString().trim { it <= ' ' })
                }
            }
            return overview!!
        }
    override fun printStackTrace() { printStackTrace(System.err) }
    override fun printStackTrace(stream: PrintStream) { print { stream.print(it) } }
    override fun printStackTrace(writer: PrintWriter) { print { writer.print(it) } }
    private fun print(write: (Any?) -> Unit) {
        write(this); write("\n")
        for (entry in stackTrace) { write("\tat "); write(entry); write("\n") }
        exceptions.forEachIndexed { index, error ->
            write("  ComposedException "); write(index + 1); write(" :\n"); trace(write, error, "\t")
        }
        write("\n")
    }
    private fun trace(write: (Any?) -> Unit, error: Throwable, prefix: String) {
        write(prefix); write(error); write('\n')
        for (entry in error.stackTrace) { write("\t\tat "); write(entry); write('\n') }
        error.cause?.let { write("\tCaused by: "); trace(write, it, "") }
    }
    private class ExceptionOverview(message: String) : RuntimeException(message) {
        override fun fillInStackTrace(): Throwable = this
    }
}
