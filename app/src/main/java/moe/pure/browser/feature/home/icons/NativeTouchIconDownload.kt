package moe.pure.browser.feature.home.icons

import moe.pure.browser.core.files.NativeDirectories
import moe.pure.browser.core.files.NativeStreams
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** w3.b/g use the file itself as the in-flight/existing gate and normalize only after HTTP200. */
internal object NativeTouchIconDownload {
    fun obtain(directory: File, imageUrl: String, pageUrl: String) {
        if (imageUrl.isEmpty() || pageUrl.isEmpty() || !NativeDirectories.ensure(directory)) return
        val target = File(directory, NativeHomeIconPaths.key(pageUrl) + ".png")
        if (target.exists()) return
        try { target.createNewFile() } catch (_: Exception) { }
        val copied = try { download(imageUrl, target) } catch (error: Exception) { error.printStackTrace(); false }
        if (copied) NativeHomeIconBitmaps.normalize(target) else target.delete()
    }

    private fun download(address: String, file: File): Boolean {
        var success = false
        var connection: HttpURLConnection? = null
        var output: FileOutputStream? = null
        try {
            connection = URL(address).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            if (connection.responseCode == 200) {
                output = FileOutputStream(file)
                val input = connection.inputStream
                val buffer = ByteArray(1024)
                while (true) {
                    val size = input.read(buffer)
                    if (size <= 0) break
                    output.write(buffer, 0, size)
                }
                success = true
                connection.disconnect()
            } else NativeStreams.close(null)
        } catch (error: Exception) {
            error.printStackTrace()
            NativeStreams.close(output)
            connection?.disconnect()
            return success
        } catch (error: Throwable) {
            NativeStreams.close(output)
            connection?.disconnect()
            throw error
        }
        if (success) NativeStreams.close(output)
        connection.disconnect()
        return success
    }
}
