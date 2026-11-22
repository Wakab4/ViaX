package moe.via.browser.core.logging

import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileWriter
import java.sql.Timestamp
import java.util.concurrent.Executors

/** k8.d batches WARN+ records after 300ms and appends them on its single writer. */
internal class NativeFileLog(val file: File, private val minimum: Int) {
    private val main = Handler(Looper.getMainLooper())
    private val writer = Executors.newSingleThreadExecutor()
    private val pending = ArrayList<String>()
    private val flush = Runnable {
        val records = ArrayList(pending)
        pending.clear()
        writer.execute {
            try { FileWriter(file, true).use { output -> records.forEach { output.append(it).append('\n') }; output.flush() } }
            catch (error: Exception) { error.printStackTrace() }
        }
    }

    fun record(priority: Int, tag: String?, text: String) {
        if (priority < minimum) return
        val level = when (priority) { 2 -> "V"; 4 -> "I"; 5 -> "W"; 6 -> "E"; 7 -> "WTF"; else -> "D" }
        pending.add("[${Timestamp(System.currentTimeMillis())}] $level/${tag.orEmpty()}(${Thread.currentThread()}): $text")
        main.removeCallbacksAndMessages(null)
        main.postDelayed(flush, 300)
    }
}
