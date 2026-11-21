package moe.pure.browser.core.reactive

/** RxJava's source error has no downstream after disposal. */
internal class NativeUndeliverableException(error: Throwable) : IllegalStateException(
    "The exception could not be delivered to the consumer because it has already canceled/disposed the flow or the exception has nowhere to go to begin with. Further reading: https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling | $error", error)
