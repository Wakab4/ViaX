package moe.pure.browser.feature.sync.data

import android.content.Context
import android.util.AtomicFile
import moe.pure.browser.feature.sync.model.CloudAccount
import moe.pure.browser.feature.sync.model.SyncCredentials
import moe.pure.browser.feature.sync.model.SyncFileState
import moe.pure.browser.feature.sync.model.SyncKind
import moe.pure.browser.feature.sync.model.WebDavProfile
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/** Synchronous local configuration/state access. Constructing this class does not load credentials or make requests. */
class SyncRepository(context: Context) {
    private val app = context.applicationContext
    private val root = File(app.filesDir, "syncing")
    private val configFile = File(root, "storages.txt")
    private val browser by lazy { app.getSharedPreferences("browser", Context.MODE_PRIVATE) }
    private val cloud by lazy { app.getSharedPreferences("sync_cloud", Context.MODE_PRIVATE) }
    private val webCipher = SyncCredentialCipher(app, "WebDav")
    private val cloudCipher = SyncCredentialCipher(app, "Cloud")

    var selectedKind: SyncKind
        get() = if (browser.getInt("syncingchoice", 0) == 1) SyncKind.WEBDAV else SyncKind.VIA_CLOUD
        set(value) { browser.edit().putInt("syncingchoice", if (value == SyncKind.WEBDAV) 1 else 0).apply() }

    fun webDav(): WebDavProfile? = synchronized(LOCK) { readProfile()?.let(::profile) }
    fun saveWebDav(
        baseUrl: String, username: String, password: String?, digestAuth: Boolean = false,
        remotePath: String = "/Via", flags: Int? = null
    ): WebDavProfile = synchronized(LOCK) {
        val url = baseUrl.trim().let { if (it.endsWith('/')) it else "$it/" }
        require(url != "/" && username.isNotEmpty()) { "地址和用户名不能为空。" }
        val old = readProfile()
        val oldConfig = old?.optJSONObject("config")
        val oldUser = oldConfig?.optString("user")?.takeIf(String::isNotEmpty)?.let(webCipher::decrypt)
        val same = oldConfig?.optString("url") == url && oldUser == username
        val secret = password?.takeIf(String::isNotEmpty) ?: if (same) oldConfig?.optString("pass")
            ?.takeIf(String::isNotEmpty)?.let(webCipher::decrypt) else null
        require(!secret.isNullOrEmpty()) { "请输入密码。" }
        val id = if (same) old!!.getString("id") else buildString { repeat(4) { append(('a'.code + RANDOM.nextInt(26)).toChar()) } }
        val config = JSONObject().put("url", url).put("user", webCipher.encrypt(username, old == null))
            .put("pass", webCipher.encrypt(secret, old == null)).put("digestAuth", digestAuth).put("path", normalizePath(remotePath))
        val saved = JSONObject().put("id", id).put("type", 16).put("flags", flags ?: old?.optInt("flags", 81) ?: 81)
            .put("config", config).put("lastSuccessAt", if (same && oldConfig?.optString("path") == normalizePath(remotePath)) old?.optLong("lastSuccessAt", 0) ?: 0 else 0)
        write(configFile, saved.toString() + "\n")
        profile(saved)
    }
    fun removeWebDav(baseUrl: String? = null, username: String? = null, digestAuth: Boolean? = null, remotePath: String? = null) = synchronized(LOCK) {
        val raw = readProfile() ?: return@synchronized
        val config = raw.getJSONObject("config")
        val changedUrl = baseUrl?.trim()?.let { if (it.isEmpty() || it.endsWith('/')) it else "$it/" }
        val previousUser = if (username == null) null else config.optString("user").takeIf(String::isNotEmpty)?.let(webCipher::decrypt).orEmpty()
        val changedIdentity = changedUrl != null && changedUrl != config.optString("url") || username != null && username != previousUser
        if (changedUrl != null) config.put("url", changedUrl)
        if (username != null) {
            if (username.isEmpty()) config.remove("user") else config.put("user", webCipher.encrypt(username, false))
        }
        if (digestAuth != null) config.put("digestAuth", digestAuth)
        if (remotePath != null) config.put("path", normalizePath(remotePath))
        config.remove("pass")
        if (changedIdentity) raw.put("id", buildString { repeat(4) { append(('a'.code + RANDOM.nextInt(26)).toChar()) } }).put("lastSuccessAt", 0)
        write(configFile, raw.toString() + "\n")
    }
    fun setWebDavFlags(flags: Int): WebDavProfile? = synchronized(LOCK) {
        val saved = readProfile() ?: return@synchronized null
        saved.put("flags", flags and 113); write(configFile, saved.toString() + "\n"); profile(saved)
    }
    fun cloudAccount(): CloudAccount = synchronized(LOCK) {
        val user = cloud.getString("user", null)?.takeIf(String::isNotEmpty)?.let(cloudCipher::decrypt).orEmpty()
        CloudAccount(cloud.getInt("server", defaultServer()), user, cloud.getBoolean("login", false))
    }
    fun selectCloudServer(index: Int) = synchronized(LOCK) {
        require(index in 0..1)
        check(cloud.edit().putInt("server", index).putBoolean("login", false).commit()); CLOUD_GENERATION.incrementAndGet(); Unit
    }
    fun logoutCloud() = synchronized(LOCK) { check(cloud.edit().putBoolean("login", false).commit()); CLOUD_GENERATION.incrementAndGet(); Unit }

    internal fun rememberCloud(username: String, passwordHash: String, loggedIn: Boolean): CloudAccount = synchronized(LOCK) {
        check(cloud.edit().putString("user", cloudCipher.encrypt(username, true))
            .putString("hash", cloudCipher.encrypt(passwordHash, true)).putBoolean("login", loggedIn).commit())
        CLOUD_GENERATION.incrementAndGet()
        cloudAccount()
    }
    internal fun cloudCredentials(): SyncCredentials = synchronized(LOCK) {
        val account = cloudAccount()
        check(account.loggedIn) { "请先登录。" }
        SyncCredentials(account.username, cloudCipher.decrypt(cloud.getString("hash", null) ?: throw IOException("缺少账号凭据。")))
    }
    internal fun cloudServerIndex(): Int = cloud.getInt("server", defaultServer())
    internal fun cloudGeneration(): Long = CLOUD_GENERATION.get()
    internal fun checkCloudGeneration(expected: Long) {
        check(CLOUD_GENERATION.get() == expected) { "Cloud account configuration changed" }
    }
    internal fun webDavCredentials(expected: WebDavProfile): SyncCredentials = synchronized(LOCK) {
        val raw = readProfile() ?: throw IOException("无配置")
        check(raw.optString("id") == expected.id) { "同步配置已更改。" }
        val config = raw.getJSONObject("config")
        SyncCredentials(webCipher.decrypt(config.getString("user")), webCipher.decrypt(config.getString("pass")))
    }
    internal fun index(profile: WebDavProfile): MutableMap<String, SyncFileState> = synchronized(LOCK) {
        val directory = profileDirectory(profile)
        INDICES.getOrPut(directory.absolutePath) {
            val result = linkedMapOf<String, SyncFileState>()
            val file = File(directory, "timestamp.txt")
            if (file.isFile) AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.filter(String::isNotBlank).forEach { line ->
                    val json = JSONObject(line); val name = json.getString("name")
                    result[name] = SyncFileState(profile.id, name, json.optLong("modified"), json.optLong("synced"))
                }
            }
            result
        }
    }
    internal fun commitIndex(profile: WebDavProfile, states: Map<String, SyncFileState>) = synchronized(LOCK) {
        val text = states.values.filter { it.remoteModified != 0L && it.lastSynced != 0L }.joinToString("\n", postfix = "\n") {
            JSONObject().put("name", it.path).put("modified", it.remoteModified).put("synced", it.lastSynced).toString()
        }
        write(File(profileDirectory(profile), "timestamp.txt"), text)
        val saved = readProfile()
        if (saved?.optString("id") == profile.id) {
            saved.put("lastSuccessAt", states.values.maxOfOrNull { it.lastSynced } ?: 0)
            write(configFile, saved.toString() + "\n")
        }
    }
    internal fun baseline(profile: WebDavProfile, path: String, remote: Boolean): File =
        File(File(profileDirectory(profile), "temp"), md5(path) + if (remote) "-1" else "-0")

    private fun profile(raw: JSONObject): WebDavProfile {
        val config = raw.getJSONObject("config")
        val username = config.optString("user").takeIf(String::isNotEmpty)?.let(webCipher::decrypt).orEmpty()
        val profile = WebDavProfile(raw.getString("id"), config.optString("url"), username,
            normalizePath(config.optString("path", "/Via")), config.optBoolean("digestAuth"), raw.optInt("flags", 81), raw.optLong("lastSuccessAt", 0),
            config.optString("pass").isNotEmpty() && config.optString("url").isNotEmpty() && username.isNotEmpty())
        val states = index(profile)
        val prefix = profile.remotePath.trim('/').let { if (it.isEmpty()) "" else "$it/" }
        return profile.copy(lastSuccessAt = profile.data.maxOfOrNull { states[prefix + it.fileName]?.lastSynced ?: 0 } ?: 0)
    }
    private fun profileDirectory(profile: WebDavProfile): File {
        require(profile.id.matches(Regex("[a-z]{4}"))) { "同步配置标识无效。" }
        // Isolate profiles and changed directories instead of reusing Native's global timestamp cache.
        return File(File(root, "profiles"), profile.id + "-" + md5(profile.baseUrl + "\n" + profile.username + "\n" + profile.remotePath))
    }
    private fun readProfile(): JSONObject? {
        if (!configFile.isFile && !File(configFile.path + ".bak").isFile) return null
        val text = AtomicFile(configFile).openRead().bufferedReader(Charsets.UTF_8).use { it.readLine() }
        return text?.takeIf(String::isNotBlank)?.let(::JSONObject)
    }
    private fun write(file: File, text: String) {
        check(file.parentFile?.let { it.mkdirs() || it.isDirectory } == true)
        val atomic = AtomicFile(file); val output = atomic.startWrite()
        try { output.write(text.toByteArray(Charsets.UTF_8)); atomic.finishWrite(output) }
        catch (error: Exception) { atomic.failWrite(output); throw error }
    }
    companion object {
        private val LOCK = Any()
        private val RANDOM = SecureRandom()
        private val CLOUD_GENERATION = AtomicLong()
        private val INDICES = hashMapOf<String, MutableMap<String, SyncFileState>>()
        fun defaultServer() = if (Locale.getDefault().country.equals("CN", true)) 1 else 0
        fun normalizePath(path: String?): String {
            val clean = path.orEmpty().trim().filterNot { it in "\\:*?\"<>|" }.replace(Regex("/+"), "/")
            if (clean.isEmpty()) return "/Via"
            val normalized = "/" + clean.trim('/')
            return normalized
        }
        internal fun md5(value: String) = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
    }
}
