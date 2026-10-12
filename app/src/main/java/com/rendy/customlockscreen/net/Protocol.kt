package com.rendy.customlockscreen.net

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/** Protocol version exchanged in the `v` field of every message. */
const val PROTOCOL_VERSION = 1

/** Default TCP port the Kids device listens on. */
const val DEFAULT_PORT = 8787

/** mDNS/NSD service type advertised by the Kids device. */
const val SERVICE_TYPE = "_kuncidong._tcp"

/** Commands understood by the server. */
object Cmd {
    const val HELLO = "hello"
    const val PAIR = "pair"
    const val LOCK = "lock"
    const val UNLOCK = "unlock"
    const val STATUS = "status"
    const val SET_PIN = "setpin"
    const val UNPAIR = "unpair"
}

/** Lock presentation supported by the Kids device. */
object LockMode {
    const val QUIZ = "quiz"
    const val TEXT = "text"
}

/** Error codes returned in [RemoteResponse.err]. */
object Err {
    const val BAD_REQUEST = "bad_request"
    const val BAD_CODE = "bad_code"
    const val BAD_PIN = "bad_pin"
    const val NEEDS_TOKEN = "needs_token"
    const val BAD_TOKEN = "bad_token"
    const val NOT_PAIRED = "not_paired"
}

/**
 * A request sent from a Parent device to a Kids device.
 *
 * Messages are newline-delimited JSON over TCP. Fields are nullable because Gson
 * fills absent keys with `null`.
 */
data class RemoteRequest(
    val v: Int = PROTOCOL_VERSION,
    val cmd: String? = null,
    val code: String? = null,
    val pin: String? = null,
    val parent: String? = null,
    val token: String? = null,
    val mode: String? = null,
    val title: String? = null,
    val text: String? = null,
    val pack: String? = null
)

/** A response sent by the Kids device back to the Parent device. */
data class RemoteResponse(
    val v: Int = PROTOCOL_VERSION,
    val ok: Boolean = false,
    val err: String? = null,
    val device: String? = null,
    val paired: Boolean? = null,
    val needsPair: Boolean? = null,
    val token: String? = null,
    val locked: Boolean? = null,
    val mode: String? = null,
    val battery: Int? = null
)

/**
 * JSON (de)serialization for the remote control protocol. Gson is already a
 * project dependency, so no new library is introduced.
 */
object Protocol {

    private val gson = Gson()

    /** Serializes [request] as a single newline-terminated JSON line. */
    fun encode(request: RemoteRequest): String = gson.toJson(request) + "\n"

    /** Serializes [response] as a single newline-terminated JSON line. */
    fun encode(response: RemoteResponse): String = gson.toJson(response) + "\n"

    /** Decodes a request line, returning `null` when it is malformed or has no command. */
    fun decodeRequest(line: String): RemoteRequest? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val request = gson.fromJson(trimmed, RemoteRequest::class.java) ?: return null
            if (request.cmd.isNullOrBlank()) null else request
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /** Decodes a response line, returning `null` when it is malformed. */
    fun decodeResponse(line: String): RemoteResponse? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        return try {
            gson.fromJson(trimmed, RemoteResponse::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }
}
