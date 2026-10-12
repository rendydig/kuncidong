package com.rendy.customlockscreen.net

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket

/** Current device status reported to the Parent. */
data class RemoteStatus(val locked: Boolean, val mode: String?, val battery: Int?)

/** Callbacks the server invokes when a valid command arrives. */
interface RemoteCommandHandler {
    fun onLock(mode: String, title: String?, text: String?, pack: String?): Boolean
    fun onUnlock(): Boolean
    fun currentStatus(): RemoteStatus
}

/**
 * Newline-delimited JSON server listening for commands from Parent devices.
 * Every command except [Cmd.HELLO] and [Cmd.PAIR] requires a valid auth token.
 */
class RemoteControlServer(
    private val store: RemoteAuthStore,
    private val handler: RemoteCommandHandler,
    private val port: Int = DEFAULT_PORT
) {

    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    var isRunning: Boolean = false
        private set

    /** Opens the socket and starts accepting clients. Idempotent. */
    fun start() {
        if (isRunning) return
        try {
            val socket = ServerSocket().apply {
                reuseAddress = true
                bind(java.net.InetSocketAddress(port))
            }
            serverSocket = socket
            isRunning = true
            Log.d(TAG, "Listening on port $port")
            scope.launch { acceptLoop(socket) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server on port $port: ${e.message}")
            isRunning = false
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing server socket: ${e.message}")
        }
        serverSocket = null
        scope.cancel()
    }

    private suspend fun acceptLoop(socket: ServerSocket) {
        while (scope.isActive && isRunning) {
            val client = try {
                socket.accept()
            } catch (e: Exception) {
                if (isRunning) Log.w(TAG, "accept() failed: ${e.message}")
                break
            }
            scope.launch { handleClient(client) }
        }
    }

    private fun handleClient(client: Socket) {
        try {
            client.soTimeout = CLIENT_TIMEOUT_MS
            val reader: BufferedReader = BufferedReader(InputStreamReader(client.getInputStream()))
            val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(client.getOutputStream()))

            val line = reader.readLine() ?: return
            val request = Protocol.decodeRequest(line)

            val response = if (request == null) {
                RemoteResponse(ok = false, err = Err.BAD_REQUEST)
            } else {
                dispatch(request)
            }

            writer.write(Protocol.encode(response))
            writer.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Client handling failed: ${e.message}")
        } finally {
            try {
                client.close()
            } catch (e: Exception) {
                // ignored
            }
        }
    }

    private fun dispatch(request: RemoteRequest): RemoteResponse {
        return when (request.cmd) {
            Cmd.HELLO -> {
                val parents = store.listParents()
                RemoteResponse(
                    ok = true,
                    device = store.deviceName(),
                    paired = parents.isNotEmpty(),
                    needsPair = !store.hasPin()
                )
            }

            Cmd.PAIR -> {
                val token = store.authorizePair(request.code, request.pin, request.parent)
                if (token == null) {
                    RemoteResponse(ok = false, err = Err.BAD_CODE)
                } else {
                    RemoteResponse(ok = true, token = token, device = store.deviceName())
                }
            }

            Cmd.LOCK -> withToken(request) {
                val mode = if (request.mode == LockMode.TEXT) LockMode.TEXT else LockMode.QUIZ
                val accepted = handler.onLock(mode, request.title, request.text, request.pack)
                RemoteResponse(ok = accepted, locked = true, mode = mode)
            }

            Cmd.UNLOCK -> withToken(request) {
                val accepted = handler.onUnlock()
                RemoteResponse(ok = accepted, locked = false)
            }

            Cmd.STATUS -> withToken(request) {
                val status = handler.currentStatus()
                RemoteResponse(ok = true, locked = status.locked, mode = status.mode, battery = status.battery)
            }

            Cmd.SET_PIN -> withToken(request) {
                val pin = request.pin
                if (pin.isNullOrBlank() || pin.length < MIN_PIN_LENGTH) {
                    RemoteResponse(ok = false, err = Err.BAD_PIN)
                } else {
                    store.setPin(pin)
                    RemoteResponse(ok = true)
                }
            }

            Cmd.UNPAIR -> withToken(request) {
                store.unpair(request.token)
                RemoteResponse(ok = true, paired = store.listParents().isNotEmpty())
            }

            else -> RemoteResponse(ok = false, err = Err.BAD_REQUEST)
        }
    }

    /** Runs [block] only when the request carries a valid token. */
    private inline fun withToken(request: RemoteRequest, block: () -> RemoteResponse): RemoteResponse {
        val token = request.token
        if (token.isNullOrBlank()) return RemoteResponse(ok = false, err = Err.NEEDS_TOKEN)
        if (store.verifyToken(token) == null) return RemoteResponse(ok = false, err = Err.BAD_TOKEN)
        return block()
    }

    companion object {
        private const val TAG = "RemoteControlServer"
        private const val CLIENT_TIMEOUT_MS = 5_000
        const val MIN_PIN_LENGTH = 4
    }
}
