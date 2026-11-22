package moe.via.browser.feature.passwords.format

import moe.via.browser.feature.passwords.model.PasswordCredential
import moe.via.browser.feature.passwords.model.PasswordEntry
import moe.via.browser.feature.passwords.model.PasswordSites
import java.io.BufferedReader
import java.io.Reader
import java.io.Writer

/** GP va.d1's seven CSV layouts. Streams remain owned by the caller. */
object PasswordCsv {
    const val HEADER = "name,url,username,password,note"
    data class Parsed(val format: Int, val entries: List<PasswordCredential>) {
        override fun toString() = "PasswordCsv.Parsed(format=$format, count=${entries.size})"
    }

    fun detect(header: String?): Int {
        val text = header.orEmpty().filterNot { it.isWhitespace() || Character.isSpaceChar(it) }.lowercase()
        return when (text) {
            HEADER -> 1
            "\"url\",\"username\",\"password\",\"httprealm\",\"formactionorigin\",\"guid\",\"timecreated\",\"timelastused\",\"timepasswordchanged\"" -> 2
            "title,url,username,password,notes,otpauth" -> 3
            "title,url,username,password", "name,url,username,password" -> 4
            "collections,type,name,notes,fields,reprompt,login_uri,login_username,login_password,login_totp" -> 5
            "folder,favorite,type,name,notes,fields,reprompt,login_uri,login_username,login_password,login_totp" -> 6
            "title,username,password,url,notes,totp" -> 7
            else -> 0
        }
    }

    fun parse(reader: Reader): Parsed {
        val input = if (reader is BufferedReader) reader else reader.buffered()
        val format = detect(input.readLine())
        if (format == 0) return Parsed(0, emptyList())
        val entries = ArrayList<PasswordCredential>()
        val record = StringBuilder()
        var continuing = false
        var quotes = 0
        while (true) {
            val line = input.readLine() ?: break
            if (continuing) record.append('\n') else { record.setLength(0); quotes = 0 }
            record.append(line)
            quotes += line.count { it == '"' }
            continuing = quotes % 2 != 0
            if (!continuing) parseRecord(record.toString(), format)?.let(entries::add)
        }
        // As in Native, an unfinished quoted record at EOF is omitted.
        return Parsed(format, entries)
    }

    fun parseRecord(text: String, format: Int): PasswordCredential? {
        val fields = fields(text)
        var name: String; val url: String; val username: String; val password: String; var note = ""
        when (format) {
            1 -> {
                if (fields.size < 4) return null
                name = fields[0]; url = fields[1]; username = fields[2]; password = fields[3]
                if (name.contains('/')) name = PasswordSites.authority(if (url.isNotEmpty()) url else name)
                if (fields.size > 4) note = fields[4]
            }
            2 -> {
                if (fields.size != 9) return null
                url = fields[0]; name = networkSite(url); username = fields[1]; password = fields[2]
            }
            3, 4 -> {
                if (fields.size != (if (format == 3) 6 else 4)) return null
                url = fields[1]; name = networkSite(url); username = fields[2]; password = fields[3]
                if (format == 3) note = fields[4]
            }
            5, 6 -> {
                val shift = if (format == 6) 1 else 0
                if (fields.size != 10 + shift || fields[1 + shift] != "login") return null
                url = fields[6 + shift].split(',').map(String::trim).firstOrNull(PasswordSites::isNetworkUrl) ?: return null
                name = networkSite(url); username = fields[7 + shift]; password = fields[8 + shift]; note = fields[3 + shift]
            }
            7 -> {
                if (fields.size != 6) return null
                url = fields[3]; name = networkSite(url); username = fields[1]; password = fields[2]; note = fields[4]
            }
            else -> return null
        }
        if (name.isEmpty() || password.isEmpty()) return null
        val now = System.currentTimeMillis()
        return PasswordCredential(PasswordEntry(name = name, url = url, username = username, note = note, createdAt = now, updatedAt = now), password)
    }

    fun fields(record: String): List<String> {
        val fields = ArrayList<String>(); val field = StringBuilder()
        var quoted = false; var index = 0
        while (index < record.length) {
            val char = record[index]
            when {
                char == '"' -> if (quoted && index + 1 < record.length && record[index + 1] == '"') { field.append('"'); index++ } else quoted = !quoted
                char == ',' && !quoted -> { fields.add(field.toString()); field.setLength(0) }
                else -> field.append(char)
            }
            index++
        }
        fields.add(field.toString()); return fields
    }

    fun row(credential: PasswordCredential): String? {
        val entry = credential.entry
        if (entry.name.isEmpty() || credential.password.isEmpty()) return null
        val url = entry.url.takeIf { it.contains(entry.name) } ?: "https://${entry.name}/"
        return listOf(entry.name, url, entry.username, credential.password, entry.note).joinToString(",", transform = ::escape)
    }
    fun write(writer: Writer, credentials: Iterable<PasswordCredential>): Int {
        writer.append(HEADER).append('\n')
        var count = 0
        for (credential in credentials) {
            row(credential)?.let { writer.append(it); count++ }
            writer.append('\n')
        }
        writer.flush(); return count
    }
    private fun escape(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
        "\"" + value.replace("\"", "\"\"") + "\"" else value
    private fun networkSite(url: String) = if (PasswordSites.isNetworkUrl(url)) PasswordSites.authority(url) else ""
}
