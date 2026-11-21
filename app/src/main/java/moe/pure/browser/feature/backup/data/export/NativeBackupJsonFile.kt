package moe.pure.browser.feature.backup.data.export

import moe.pure.browser.core.logging.NativeLog
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

/** m9.f/g/m add their file only after flush; a subsequent close IOException retains it. */
internal object NativeBackupJsonFile {
    fun <T> write(file: File, records: List<T>, create: (T) -> JSONObject): Boolean {
        var included = false
        try {
            BufferedWriter(FileWriter(file)).use { writer ->
                for (record in records) { writer.write(create(record).toString()); writer.newLine() }
                writer.flush()
                included = true
            }
        } catch (error: IOException) { NativeLog.warn(null, error) }
        catch (error: JSONException) { NativeLog.warn(null, error) }
        return included
    }
}
