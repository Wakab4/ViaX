package moe.via.browser.core.reactive

/** f7.a and the native Maybe/Single lambda consumers keep errors separate from normal delivery. */
internal object NativeReactiveErrors {
    @Volatile private var handler: ((Throwable) -> Unit)? = null
    fun install(consumer: (Throwable) -> Unit) { handler = consumer }
    fun throwIfFatal(error: Throwable) {
        if (error is VirtualMachineError || error is ThreadDeath || error is LinkageError) throw error
    }
    fun report(error: Throwable?) {
        val value = when {
            error == null -> NullPointerException("onError called with a null Throwable. Null values are generally not allowed in 3.x operators and sources.")
            error is IllegalStateException || error is NullPointerException || error is IllegalArgumentException || error is NativeCompositeException -> error
            else -> NativeUndeliverableException(error)
        }
        val consumer = handler
        if (consumer != null) try { consumer(value); return }
        catch (failure: Throwable) { failure.printStackTrace(); uncaught(failure) }
        value.printStackTrace(); uncaught(value)
    }
    private fun uncaught(error: Throwable) {
        val thread = Thread.currentThread()
        thread.uncaughtExceptionHandler!!.uncaughtException(thread, error)
    }
    fun success(delivered: () -> Unit) {
        try { delivered() } catch (error: Throwable) { throwIfFatal(error); report(error) }
    }
    fun next(delivered: () -> Unit, failed: (Throwable) -> Unit) {
        try { delivered() } catch (error: Throwable) { throwIfFatal(error); failure(error, failed) }
    }
    fun failure(error: Throwable, delivered: (Throwable) -> Unit) {
        try { delivered(error) } catch (failure: Throwable) {
            throwIfFatal(failure); report(NativeCompositeException(error, failure))
        }
    }
}
