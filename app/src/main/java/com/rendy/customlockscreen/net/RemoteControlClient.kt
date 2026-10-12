package com.rendy.customlockscreen.net

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Blocking TCP client used by the Parent device. Every call runs on the IO
 * dispatcher and returns `null` when the device cannot be reached.
 */
class RemoteControlClient {

    /** Sends one request and reads the single-line response. */
    suspend fun exchange(
        host: String,
        port: Int = DEFAULT_PORT,
        request: RemoteRequest,
        timeoutMs: Int = CONNECT_TIMEOUT_MS
    ): RemoteResponse? = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                socket.soTimeout = timeoutMs

                val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                writer.write(Protocol.encode(request))
                writer.flush()

                val reader: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val line = reader.readLine()
                if (line == null) null else Protocol.decodeResponse(line)
            }
        } catch (e: Exception) {
            Log.d(TAG, "exchange to $host:$port failed: ${e.message}")
            null
        }
    }

    suspend fun hello(host: String, port: Int = DEFAULT_PORT, timeoutMs: Int = CONNECT_TIMEOUT_MS) =
        exchange(host, port, RemoteRequest(cmd = Cmd.HELLO), timeoutMs)

    suspend fun pair(
        host: String,
        port: Int = DEFAULT_PORT,
        code: String,
        pin: String,
        parentName: String
    ) = exchange(
        host,
        port,
        RemoteRequest(cmd = Cmd.PAIR, code = code, pin = pin, parent = parentName)
    )

    suspend fun lock(
        host: String,
        port: Int = DEFAULT_PORT,
        token: String,
        mode: String,
        title: String? = null,
        text: String? = null,
        pack: String? = null
    ) = exchange(
        host,
        port,
        RemoteRequest(cmd = Cmd.LOCK, token = token, mode = mode, title = title, text = text, pack = pack)
    )

    suspend fun unlock(host: String, port: Int = DEFAULT_PORT, token: String) =
        exchange(host, port, RemoteRequest(cmd = Cmd.UNLOCK, token = token))

    suspend fun status(host: String, port: Int = DEFAULT_PORT, token: String) =
        exchange(host, port, RemoteRequest(cmd = Cmd.STATUS, token = token))

    suspend fun setPin(host: String, port: Int = DEFAULT_PORT, token: String, pin: String) =
        exchange(host, port, RemoteRequest(cmd = Cmd.SET_PIN, token = token, pin = pin))

    suspend fun unpair(host: String, port: Int = DEFAULT_PORT, token: String) =
        exchange(host, port, RemoteRequest(cmd = Cmd.UNPAIR, token = token))

    companion object {
        private const val TAG = "RemoteControlClient"
        const val CONNECT_TIMEOUT_MS = 1_500
    }
}
