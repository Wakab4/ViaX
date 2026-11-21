package moe.pure.browser.feature.adblock

import android.content.SharedPreferences
import moe.pure.browser.feature.download.DownloadNames
import java.security.MessageDigest

object AdblockStatistics {
    fun record(values: SharedPreferences, url: String) {
        val hash = MessageDigest.getInstance("MD5").digest(url.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
        var estimate = hash.take(3).toInt(16) + hash.substring(13, 16).toInt(16) + hash.takeLast(3).toInt(16)
        do { estimate /= 2 } while (estimate > 80)
        values.edit().putInt("adblockedtimes", values.getInt("adblockedtimes", 0) + 1).putLong("savedata", values.getLong("savedata", 0) + estimate).apply()
    }
    fun summary(values: SharedPreferences): String = if (!moe.pure.browser.feature.settings.data.BrowserPreferences(values).adblock) "未启用" else
        "已拦截 ${values.getInt("adblockedtimes", 0)} 个广告，共节省 ${DownloadNames.size(values.getLong("savedata", 0) * 1024)}"
}
