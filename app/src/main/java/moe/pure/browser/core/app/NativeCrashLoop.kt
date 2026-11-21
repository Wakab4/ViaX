package moe.pure.browser.core.app

import android.os.Handler
import android.os.Looper

/** com.tuyafeng.support.crash.a owns the native nested main loop and default thread handler. */
internal object NativeCrashLoop {
    private var installed = false
    private var reported: ((Thread, Throwable) -> Unit)? = null
    private var previous: Thread.UncaughtExceptionHandler? = null

    @Synchronized
    fun install(callback: (Thread, Throwable) -> Unit) {
        if (installed) return
        installed = true
        reported = callback
        Handler(Looper.getMainLooper()).post {
            while (true) {
                try { Looper.loop() }
                catch (error: Throwable) {
                    if (error is NativeCrashExit) return@post
                    reported?.invoke(Looper.getMainLooper().thread, error)
                }
            }
        }
        previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error -> reported?.invoke(thread, error) }
    }
}

/** The source's package-private UnknowException terminates only the nested loop. */
internal class NativeCrashExit(message: String) : RuntimeException(message)
