package moe.pure.browser.core.app

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.core.reactive.NativeReactiveErrors

/** BrowserApp.c/b post uncaught reports back to the main thread. */
internal object NativeApplicationErrors {
    fun install(application: Application) {
        NativeCrashLoop.install { thread, error ->
            Handler(Looper.getMainLooper()).post {
                try {
                    val message = "Exception Happend\n$thread\n$error"
                    NativeLog.error(null, error)
                    Toast.makeText(application, message, Toast.LENGTH_SHORT).show()
                } catch (_: Throwable) { }
            }
        }
        NativeReactiveErrors.install { error -> NativeLog.error(null, error) }
    }
}
