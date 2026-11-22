package moe.via.browser.feature.sync.network

import android.util.Base64
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale

/** Authentication state belongs to one configured account and one HTTP origin. */
internal class HttpAuthentication(
    private val username: String,
    private val password: String,
    private val digest: Boolean
) {
    private var current: DigestChallenge? = null
    private var nonceCount = 0L
    private var clientNonce: String? = null
    private val random = SecureRandom()

    /** [requestTarget] must be the exact encoded path and query sent in the request. */
    @Synchronized
    fun authorization(
        method: String,
        requestTarget: String,
        body: ByteArray = ByteArray(0)
    ): String? {
        if (!digest) {
            val credentials = "$username:$password".toByteArray(StandardCharsets.ISO_8859_1)
            return "Basic " + Base64.encodeToString(credentials, Base64.NO_WRAP)
        }
        val challenge = current ?: return null
        if (method.isEmpty() || !method.all(::isTokenCharacter) || requestTarget.isEmpty() ||
            requestTarget.any(::isControlCharacter) || username.any(::isControlCharacter)
        ) return null
        if (challenge.qop != null && nonceCount == MAX_NONCE_COUNT) return null

        return try {
            val cnonce = clientNonce ?: newClientNonce().also { clientNonce = it }
            val nextCount = nonceCount + 1L
            val count = nextCount.toString(16).padStart(8, '0')
            fun hashText(text: String): String = hash(text.toByteArray(challenge.charset), challenge.algorithm)

            val initial = hashText("$username:${challenge.realm}:$password")
            val ha1 = if (challenge.algorithm.session) {
                hashText("$initial:${challenge.nonce}:$cnonce")
            } else initial
            val ha2 = if (challenge.qop == "auth-int") {
                hashText("$method:$requestTarget:${hash(body, challenge.algorithm)}")
            } else hashText("$method:$requestTarget")
            val response = if (challenge.qop == null) {
                hashText("$ha1:${challenge.nonce}:$ha2")
            } else {
                hashText("$ha1:${challenge.nonce}:$count:$cnonce:${challenge.qop}:$ha2")
            }

            val result = buildString {
                append("Digest username=").append(quote(username))
                append(", realm=").append(quote(challenge.realm))
                append(", nonce=").append(quote(challenge.nonce))
                append(", uri=").append(quote(requestTarget))
                append(", response=").append(quote(response))
                append(", algorithm=").append(challenge.algorithm.token)
                challenge.opaque?.let { append(", opaque=").append(quote(it)) }
                if (challenge.qop != null) {
                    append(", qop=").append(challenge.qop)
                    append(", nc=").append(count)
                    append(", cnonce=").append(quote(cnonce))
                } else if (challenge.algorithm.session) {
                    append(", cnonce=").append(quote(cnonce))
                }
            }
            if (challenge.qop != null) nonceCount = nextCount
            result
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    /** Returns whether the response offers a supported challenge for this configured scheme. */
    @Synchronized
    fun challenge(wwwAuthenticate: List<String>): Boolean {
        val parsed = wwwAuthenticate.flatMap(::parseChallenges)
        if (!digest) return parsed.any { it.scheme.equals("Basic", ignoreCase = true) && it.valid }

        val replacement = parsed.asSequence()
            .filter { it.scheme.equals("Digest", ignoreCase = true) && it.valid }
            .mapNotNull(::digestChallenge)
            .maxByOrNull { it.algorithm.preference }
        if (replacement == null) {
            current = null
            nonceCount = 0L
            clientNonce = null
            return false
        }
        if (replacement.stale || current?.sameNonceState(replacement) != true) {
            nonceCount = 0L
            clientNonce = null
        }
        current = replacement
        return true
    }

    override fun toString(): String = "HttpAuthentication(digest=$digest)"

    private fun newClientNonce(): String = ByteArray(16).also(random::nextBytes).toHex()

    private fun digestChallenge(challenge: ParsedChallenge): DigestChallenge? {
        val parameters = challenge.parameters
        val realm = parameters["realm"] ?: return null
        val nonce = parameters["nonce"]?.takeIf { it.isNotEmpty() } ?: return null
        val algorithm = when (parameters["algorithm"]?.uppercase(Locale.ROOT) ?: "MD5") {
            "MD5" -> DigestAlgorithm.MD5
            "MD5-SESS" -> DigestAlgorithm.MD5_SESSION
            "SHA-256" -> DigestAlgorithm.SHA256
            "SHA-256-SESS" -> DigestAlgorithm.SHA256_SESSION
            else -> return null
        }
        val qop = parameters["qop"]?.let { options ->
            val offered = options.split(',').map { it.trim().lowercase(Locale.ROOT) }
            when {
                "auth" in offered -> "auth"
                "auth-int" in offered -> "auth-int"
                else -> return null
            }
        }
        val charset = when (parameters["charset"]?.uppercase(Locale.ROOT)) {
            null -> StandardCharsets.ISO_8859_1
            "UTF-8" -> StandardCharsets.UTF_8
            else -> return null
        }
        val stale = when (parameters["stale"]?.lowercase(Locale.ROOT)) {
            null, "false" -> false
            "true" -> true
            else -> return null
        }
        return DigestChallenge(realm, nonce, parameters["opaque"], algorithm, qop, charset, stale)
    }

    private fun parseChallenges(header: String): List<ParsedChallenge> {
        if (header.any { isControlCharacter(it) && it != '\t' }) return emptyList()
        val parts = splitDirectives(header) ?: return emptyList()
        val result = mutableListOf<ParsedChallenge>()
        var active: ParsedChallenge? = null
        for (part in parts) {
            val text = part.trim()
            if (text.isEmpty()) continue
            val tokenEnd = text.indexOfFirst { !isTokenCharacter(it) }.let {
                if (it < 0) text.length else it
            }
            if (tokenEnd == 0) {
                active?.valid = false
                continue
            }
            val afterToken = skipWhitespace(text, tokenEnd)
            if (afterToken < text.length && text[afterToken] == '=') {
                active?.addDirective(text)
            } else {
                active?.let(result::add)
                active = ParsedChallenge(text.substring(0, tokenEnd))
                if (afterToken < text.length) active.addDirective(text.substring(afterToken))
            }
        }
        active?.let(result::add)
        return result
    }

    private fun splitDirectives(header: String): List<String>? {
        val parts = mutableListOf<String>()
        var quoted = false
        var escaped = false
        var start = 0
        for (index in header.indices) {
            val character = header[index]
            if (escaped) {
                escaped = false
            } else if (quoted && character == '\\') {
                escaped = true
            } else if (character == '"') {
                quoted = !quoted
            } else if (!quoted && character == ',') {
                parts += header.substring(start, index)
                start = index + 1
            }
        }
        if (quoted || escaped) return null
        parts += header.substring(start)
        return parts
    }

    private class ParsedChallenge(val scheme: String) {
        val parameters = linkedMapOf<String, String>()
        var valid = true

        fun addDirective(text: String) {
            val keyEnd = text.indexOfFirst { !isTokenCharacter(it) }.let {
                if (it < 0) text.length else it
            }
            var index = skipWhitespace(text, keyEnd)
            if (keyEnd == 0 || index >= text.length || text[index] != '=') {
                valid = false
                return
            }
            val key = text.substring(0, keyEnd).lowercase(Locale.ROOT)
            index = skipWhitespace(text, index + 1)
            if (index >= text.length) {
                valid = false
                return
            }
            val value: String
            if (text[index] == '"') {
                val decoded = StringBuilder()
                var closed = false
                index++
                while (index < text.length) {
                    val character = text[index++]
                    when {
                        character == '"' -> {
                            closed = true
                            break
                        }
                        character == '\\' && index < text.length -> decoded.append(text[index++])
                        character == '\\' -> break
                        else -> decoded.append(character)
                    }
                }
                if (!closed || skipWhitespace(text, index) != text.length) {
                    valid = false
                    return
                }
                value = decoded.toString()
            } else {
                val start = index
                while (index < text.length && isTokenCharacter(text[index])) index++
                if (index == start || skipWhitespace(text, index) != text.length) {
                    valid = false
                    return
                }
                value = text.substring(start, index)
            }
            if (parameters.put(key, value) != null) valid = false
        }
    }

    private data class DigestChallenge(
        val realm: String,
        val nonce: String,
        val opaque: String?,
        val algorithm: DigestAlgorithm,
        val qop: String?,
        val charset: Charset,
        val stale: Boolean
    ) {
        fun sameNonceState(other: DigestChallenge): Boolean =
            realm == other.realm && nonce == other.nonce &&
                algorithm == other.algorithm && charset == other.charset
    }

    private enum class DigestAlgorithm(
        val token: String,
        val hashName: String,
        val session: Boolean,
        val preference: Int
    ) {
        MD5("MD5", "MD5", false, 0),
        MD5_SESSION("MD5-sess", "MD5", true, 1),
        SHA256("SHA-256", "SHA-256", false, 2),
        SHA256_SESSION("SHA-256-sess", "SHA-256", true, 3)
    }

    private companion object {
        const val MAX_NONCE_COUNT = 0xffff_ffffL
        const val TOKEN_PUNCTUATION = "!#$%&'*+-.^_`|~"
        const val HEX = "0123456789abcdef"

        fun isTokenCharacter(character: Char): Boolean =
            character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9' ||
                character in TOKEN_PUNCTUATION

        fun isControlCharacter(character: Char): Boolean = character < ' ' || character == '\u007f'

        fun skipWhitespace(text: String, start: Int): Int {
            var index = start
            while (index < text.length && (text[index] == ' ' || text[index] == '\t')) index++
            return index
        }

        fun quote(text: String): String = buildString {
            append('"')
            for (character in text) {
                if (character == '"' || character == '\\') append('\\')
                append(character)
            }
            append('"')
        }

        fun hash(bytes: ByteArray, algorithm: DigestAlgorithm): String =
            MessageDigest.getInstance(algorithm.hashName).digest(bytes).toHex()

        fun ByteArray.toHex(): String = buildString(size * 2) {
            for (byte in this@toHex) {
                val value = byte.toInt() and 0xff
                append(HEX[value ushr 4])
                append(HEX[value and 0x0f])
            }
        }
    }
}
